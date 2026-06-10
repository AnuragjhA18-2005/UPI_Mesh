/**
 * UPImesh Frontend - Offline-First Implementation
 * Core logic for local wallet, offline packet generation, and mesh sync.
 */

// --- Configuration & Constants ---
const CONFIG = {
    STORAGE_KEY_PREFIX: 'upimesh_queue_',
    HISTORY_KEY_PREFIX: 'upimesh_history_',
    BALANCE_KEY_PREFIX: 'upimesh_balance_',
    AUTH_KEY: 'upimesh_auth_token',
    CLEANUP_DELAY: 5000
};

const STATE = {
    isSyncing: false,
    activeAccount: localStorage.getItem('upimesh_active_account') || 'alice@upimesh'
};

// --- Local Storage (State Persistence) ---
function getAccountKey(type) {
    return type + STATE.activeAccount;
}

/** @returns {Array} List of packets currently in the local queue */
function getQueue() {
    try {
        const data = localStorage.getItem(getAccountKey(CONFIG.STORAGE_KEY_PREFIX));
        return data ? JSON.parse(data) : [];
    } catch (e) {
        return [];
    }
}

function saveQueue(queue) {
    localStorage.setItem(getAccountKey(CONFIG.STORAGE_KEY_PREFIX), JSON.stringify(queue));
}

/** @param {Object} packet - The MeshPacket to add to local storage */
function addToQueue(packet) {
    const queue = getQueue();
    queue.unshift(packet);
    saveQueue(queue);
    renderQueue();
    if (navigator.onLine) flushQueue();
}

/** Updates the status of a specific packet in the queue */
function updatePacketStatus(packetId, status, txId = null) {
    const queue = getQueue();
    const packet = queue.find(p => p.packetId === packetId);
    if (packet) {
        packet.status = status;
        if (txId) packet.txId = txId;
        saveQueue(queue);
        renderQueue();
    }
}

/** Removes a packet from the queue (usually after settlement) */
function removeFromQueue(packetId) {
    const queue = getQueue().filter(p => p.packetId !== packetId);
    saveQueue(queue);
    renderQueue();
}

/** Cleans up all settled packets from the queue (e.g., on startup or after sync) */
function cleanupQueue() {
    const queue = getQueue();
    const active = queue.filter(p => p.status !== 'SETTLED');
    if (active.length !== queue.length) {
        saveQueue(active);
        renderQueue();
    }
}

/** @returns {Array} List of settled transactions from local cache */
function getLocalHistory() {
    try {
        const data = localStorage.getItem(getAccountKey(CONFIG.HISTORY_KEY_PREFIX));
        return data ? JSON.parse(data) : [];
    } catch (e) {
        return [];
    }
}

/** Adds a transaction to local history cache */
function addToLocalHistory(tx) {
    const history = getLocalHistory();
    if (!history.find(h => (h.id && h.id === tx.id) || (h.packetId && h.packetId === tx.packetId))) {
        history.unshift(tx);
        localStorage.setItem(getAccountKey(CONFIG.HISTORY_KEY_PREFIX), JSON.stringify(history.slice(0, 50)));
    }
}

// --- Account Management ---
async function switchAccount(accountId) {
    console.log(`[Account] Switching to: ${accountId}`);
    STATE.activeAccount = accountId;
    localStorage.setItem('upimesh_active_account', accountId);
    
    // Reset UI for the new account context
    initBalance();
    cleanupQueue();
    renderQueue();
    
    // Switch to Wallet view automatically
    switchView('view-wallet');
    
    // Trigger fresh data fetch
    if (navigator.onLine) {
        fetchBalance();
        flushQueue();
    }
}

async function fetchIdentities() {
    try {
        const res = await fetch('/api/accounts');
        if (res.ok) {
            const accounts = await res.json();
            const select = document.getElementById('account-select');
            if (select) {
                select.innerHTML = accounts.map(acc => 
                    `<option value="${acc.id}" ${acc.id === STATE.activeAccount ? 'selected' : ''}>${acc.id}</option>`
                ).join('');
            }
        }
    } catch (e) {
        console.warn("[Account] Failed to fetch identity list from server.");
    }
}

// --- UI Rendering Engine ---
function renderQueue() {
    const queueList = document.getElementById('queue-list');
    if (!queueList) return;
    const queue = getQueue();

    if (queue.length === 0) {
        queueList.innerHTML = `<div class="empty-state"><p>No pending payments for ${STATE.activeAccount}.</p></div>`;
        return;
    }

    queueList.innerHTML = queue.map(packet => {
        const isSettled = packet.status === 'SETTLED';
        return `
            <div class="queue-item ${isSettled ? 'settled' : ''}">
                <div class="item-info">
                    <p class="receiver">To: ${packet.receiver}</p>
                    <p class="amount">₹ ${parseFloat(packet.amount).toFixed(2)}</p>
                    <small style="color: var(--text-secondary); font-size: 0.6rem; display: block; margin-top: 4px;">
                        ${isSettled ? 'TX ID: ' + packet.txId : 'ID: ' + packet.packetId.substring(0, 8) + '...'}
                    </small>
                </div>
                <div class="item-status">
                    <span class="badge ${isSettled ? 'badge-success' : 'badge-pending'}">${isSettled ? 'Settled' : 'Pending'}</span>
                    <p class="time">${new Date(packet.timestamp).toLocaleTimeString()}</p>
                </div>
            </div>`;
    }).join('');
}

async function fetchHistory() {
    const historyList = document.getElementById('history-list');
    if (!historyList) return;

    let backendTxs = [];
    try {
        const res = await fetch(`/api/accounts/${STATE.activeAccount}/transactions`);
        if (res.ok) {
            backendTxs = await res.json();
            backendTxs.forEach(tx => addToLocalHistory(tx));
        }
    } catch (e) {
        console.error("Failed to fetch history from backend:", e);
    }

    const localHistory = getLocalHistory();
    const localQueue = getQueue();
    renderHistory(backendTxs.length > 0 ? backendTxs : localHistory, localQueue);
}

function renderHistory(settledTxs, unsettledTxs) {
    const historyList = document.getElementById('history-list');
    if (!historyList) return;

    const allTxs = [
        ...unsettledTxs.map(tx => ({ ...tx, isPending: true })),
        ...settledTxs.map(tx => ({ ...tx, isPending: false }))
    ].sort((a, b) => new Date(b.timestamp || b.signedAt) - new Date(a.timestamp || a.signedAt));

    if (allTxs.length === 0) {
        historyList.innerHTML = `<div class="empty-state"><p>No transaction history for ${STATE.activeAccount}.</p></div>`;
        return;
    }

    historyList.innerHTML = allTxs.map(tx => {
        const isPending = tx.isPending;
        const isFailed = tx.status === 'FAILED' || tx.id === 'FAILED';
        
        let statusBadge = 'badge-success';
        let statusLabel = 'Settled';
        
        if (isPending) {
            statusBadge = 'badge-pending';
            statusLabel = 'Pending';
        } else if (isFailed) {
            statusBadge = 'badge-error';
            statusLabel = 'Failed';
        }

        return `
            <div class="history-item ${isPending ? 'pending' : (isFailed ? 'failed' : '')}">
                <div class="item-info">
                    <p class="receiver">To: ${isPending ? tx.receiver : tx.receiverID}</p>
                    <p class="amount">₹ ${parseFloat(tx.amount).toFixed(2)}</p>
                    <small style="color: var(--text-secondary); font-size: 0.6rem; display: block; margin-top: 4px;">
                        ${isPending ? 'Pending' : (isFailed ? 'Rejected by Bank' : 'TX ID: ' + tx.id)} | ${new Date(tx.timestamp || tx.signedAt).toLocaleDateString()}
                    </small>
                </div>
                <div class="item-status">
                    <span class="badge ${statusBadge}">${statusLabel}</span>
                    <p class="time">${new Date(tx.timestamp || tx.signedAt).toLocaleTimeString()}</p>
                </div>
            </div>`;
    }).join('');
}

// --- Network & Sync ---
async function flushQueue() {
    if (STATE.isSyncing || !navigator.onLine) return;
    cleanupQueue();
    const queue = getQueue().filter(p => p.status !== 'SETTLED');
    if (queue.length === 0) return;
    STATE.isSyncing = true;

    let token = localStorage.getItem(CONFIG.AUTH_KEY);
    if (!token || token === 'undefined') {
        try {
            const authRes = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: 'bridge-node-1', password: 'secret123' })
            });
            if (authRes.ok) {
                const data = await authRes.json();
                token = data.token;
                localStorage.setItem(CONFIG.AUTH_KEY, token);
            }
        } catch (e) { STATE.isSyncing = false; return; }
    }

    for (const packet of queue) {
        try {
            const response = await fetch('/api/bridge/ingest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ packetId: packet.packetId, cipherText: packet.cipherText })
            });

            if (response.ok) {
                const result = await response.text();
                const txIdMatch = result.match(/Transaction ID (\d+)/);
                const txId = txIdMatch ? txIdMatch[1] : 'OK';

                addToLocalHistory({
                    id: txId,
                    senderID: STATE.activeAccount,
                    receiverID: packet.receiver,
                    amount: packet.amount,
                    status: 'SETTLED',
                    timestamp: new Date().toISOString(),
                    packetId: packet.packetId
                });

                removeFromQueue(packet.packetId);
                fetchBalance();
                if (document.getElementById('view-history').classList.contains('active')) fetchHistory();
                showNotification(`Payment of ₹${packet.amount} for ${packet.receiver} settled!`, 'success');
            } else if (response.status === 400) {
                addToLocalHistory({
                    id: 'FAILED',
                    senderID: STATE.activeAccount,
                    receiverID: packet.receiver,
                    amount: packet.amount,
                    status: 'FAILED',
                    timestamp: new Date().toISOString(),
                    packetId: packet.packetId
                });
                showNotification(`Payment Failed: ${packet.receiver} rejected (Insufficient Funds).`, 'error');
                removeFromQueue(packet.packetId);
                if (document.getElementById('view-history').classList.contains('active')) fetchHistory();
            }
        } catch (err) { break; }
    }
    STATE.isSyncing = false;
}

async function fetchBalance() {
    if (!navigator.onLine) return;
    try {
        const res = await fetch(`/api/accounts/${STATE.activeAccount}/balance`);
        if (res.ok) {
            const data = await res.json();
            const formatted = parseFloat(data.balance).toFixed(2);
            document.querySelector('.balance').innerText = `₹ ${formatted}`;
            localStorage.setItem(getAccountKey(CONFIG.BALANCE_KEY_PREFIX), formatted);
        }
    } catch (e) {}
}

function initBalance() {
    const last = localStorage.getItem(getAccountKey(CONFIG.BALANCE_KEY_PREFIX)) || '0.00';
    document.querySelector('.balance').innerText = `₹ ${last}`;
}

async function generateOfflinePacket(receiver, amount) {
    if (navigator.onLine) {
        try {
            // Updated to pass senderID if backend supports it in demo controller
            const response = await fetch(`/api/demo/generate-packet?sender=${STATE.activeAccount}&receiver=${receiver}&amount=${amount}`);
            if (response.ok) {
                const packet = await response.json();
                return { ...packet, receiver, amount, timestamp: Date.now() };
            }
        } catch (e) {}
    }
    return {
        packetId: 'offline-' + Math.random().toString(36).substr(2, 9),
        cipherText: btoa(JSON.stringify({ receiver, amount, sender: STATE.activeAccount, timestamp: Date.now() })),
        receiver, amount, timestamp: Date.now()
    };
}

// --- Lifecycle ---
function switchView(viewId, navElement) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(viewId).classList.add('active');
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    if (navElement) navElement.classList.add('active');
    else {
        const map = { 'view-wallet': 0, 'view-pay': 1, 'view-queue': 2, 'view-history': 3 };
        const items = document.querySelectorAll('.nav-item');
        if (items[map[viewId]]) items[map[viewId]].classList.add('active');
    }
    if (viewId === 'view-history') fetchHistory();
}

async function checkConnectivity() {
    if (!navigator.onLine) return false;
    try {
        const res = await fetch('/api/health?t=' + Date.now());
        return res.ok;
    } catch (e) { return false; }
}

const updateNetworkStatus = async () => {
    const isOnline = await checkConnectivity();
    const pulse = document.querySelector('.pulse');
    const badge = document.querySelector('.status-badge');
    if (pulse) pulse.style.backgroundColor = isOnline ? 'var(--accent-green)' : 'var(--accent-red)';
    if (badge) badge.innerHTML = isOnline ? '<span class="pulse" style="background-color: var(--accent-green)"></span> Online (Bridge Active)' : '<span class="pulse" style="background-color: var(--accent-red)"></span> Offline Mode Active';
    if (isOnline) { fetchBalance(); flushQueue(); fetchIdentities(); }
};

document.addEventListener('DOMContentLoaded', () => {
    initBalance();
    cleanupQueue();
    renderQueue();
    updateNetworkStatus();
    setInterval(updateNetworkStatus, 5000);

    const form = document.getElementById('payment-form');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const vpa = document.getElementById('vpa').value;
            const amount = document.getElementById('amount').value;
            const btn = e.target.querySelector('button');
            btn.disabled = true; btn.innerHTML = '🔒 Encrypting...';
            try {
                const packet = await generateOfflinePacket(vpa, amount);
                addToQueue(packet);
                e.target.reset();
                switchView('view-queue');
                showNotification(`Payment of ₹${amount} queued for ${vpa}.`, 'success');
            } finally { btn.disabled = false; btn.innerHTML = '<span class="icon">🔒</span> Encrypt & Queue Payment'; }
        });
    }
});
