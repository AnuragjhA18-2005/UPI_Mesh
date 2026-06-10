// --- View Management ---
function switchView(viewId, navElement) {
    document.querySelectorAll('.view').forEach(view => view.classList.remove('active'));
    document.getElementById(viewId).classList.add('active');

    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    if (navElement) {
        navElement.classList.add('active');
    } else {
        const indexMap = { 'view-wallet': 0, 'view-pay': 1, 'view-queue': 2 };
        const index = indexMap[viewId];
        if (index !== undefined) {
            document.querySelectorAll('.nav-item')[index].classList.add('active');
        }
    }
}

// --- Local Storage Management ---
const STORAGE_KEY = 'upimesh_outbound_queue';
const AUTH_KEY = 'upimesh_auth_token';

function getQueue() {
    const data = localStorage.getItem(STORAGE_KEY);
    return data ? JSON.parse(data) : [];
}

function addToQueue(packet) {
    const queue = getQueue();
    queue.unshift(packet);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
    renderQueue();
    if (navigator.onLine) flushQueue();
}

function updatePacketStatus(packetId, status, txId = null) {
    const queue = getQueue();
    const packet = queue.find(p => p.packetId === packetId);
    if (packet) {
        packet.status = status;
        if (txId) packet.txId = txId;
        localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
        renderQueue();
    }
}

function removeFromQueue(packetId) {
    const queue = getQueue().filter(p => p.packetId !== packetId);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
    renderQueue();
}

// --- UI Rendering ---
function renderQueue() {
    const queueList = document.getElementById('queue-list');
    if (!queueList) return;
    const queue = getQueue();

    if (queue.length === 0) {
        queueList.innerHTML = `<div class="empty-state"><p>No pending payments. You're all caught up!</p></div>`;
        return;
    }

    queueList.innerHTML = queue.map(packet => {
        const isSettled = packet.status === 'SETTLED';
        const statusClass = isSettled ? 'badge-success' : 'badge-pending';
        const statusLabel = isSettled ? 'Settled' : 'Pending Sync';
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
                    <span class="badge ${statusClass}">${statusLabel}</span>
                    <p class="time">${new Date(packet.timestamp).toLocaleTimeString()}</p>
                </div>
            </div>`;
    }).join('');
}

// --- Bridge Sync Module ---
let isSyncing = false;
async function flushQueue() {
    if (isSyncing || !navigator.onLine) return;
    const queue = getQueue().filter(p => p.status !== 'SETTLED');
    if (queue.length === 0) return;

    isSyncing = true;
    const token = localStorage.getItem(AUTH_KEY) || 'DEV_TOKEN';

    for (const packet of queue) {
        try {
            const response = await fetch('/api/bridge/ingest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ packetId: packet.packetId, cipherText: packet.cipherText })
            });
            if (response.ok) {
                const result = await response.text();
                const txId = result.split('ID ')[1] || 'OK';
                updatePacketStatus(packet.packetId, 'SETTLED', txId);
                setTimeout(() => removeFromQueue(packet.packetId), 5000);
            }
        } catch (err) {
            console.error("Sync error:", err);
            break;
        }
    }
    isSyncing = false;
}

// --- Packet Generation ---
async function generateOfflinePacket(receiver, amount) {
    if (navigator.onLine) {
        try {
            const response = await fetch('/api/demo/generate-packet');
            if (response.ok) {
                const p = await response.json();
                return { ...p, receiver, amount, timestamp: Date.now(), type: 'REAL' };
            }
        } catch (e) {}
    }
    return {
        packetId: 'off-' + Math.random().toString(36).substr(2, 9),
        cipherText: btoa(JSON.stringify({ receiver, amount })),
        receiver, amount, timestamp: Date.now(), type: 'SIM'
    };
}

// --- Initialization ---
window.addEventListener('online', () => {
    const pulse = document.querySelector('.pulse');
    const badge = document.querySelector('.status-badge');
    if (pulse) pulse.style.backgroundColor = 'var(--accent-green)';
    if (badge) badge.innerHTML = '<span class="pulse" style="background-color: var(--accent-green)"></span> Online (Bridge Active)';
    flushQueue();
});

window.addEventListener('offline', () => {
    const pulse = document.querySelector('.pulse');
    const badge = document.querySelector('.status-badge');
    if (pulse) pulse.style.backgroundColor = 'var(--accent-red)';
    if (badge) badge.innerHTML = '<span class="pulse" style="background-color: var(--accent-red)"></span> Offline Mode Active';
});

document.addEventListener('DOMContentLoaded', () => {
    if (navigator.onLine) window.dispatchEvent(new Event('online'));
    renderQueue();
    const form = document.getElementById('payment-form');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = e.target.querySelector('button');
            btn.disabled = true; btn.innerText = "🔒 Encrypting...";
            try {
                const p = await generateOfflinePacket(document.getElementById('vpa').value, document.getElementById('amount').value);
                addToQueue(p);
                e.target.reset(); switchView('view-queue');
            } finally {
                btn.disabled = false; btn.innerHTML = '<span class="icon">🔒</span> Encrypt & Queue Payment';
            }
        });
    }
});
