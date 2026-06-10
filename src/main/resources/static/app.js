/**
 * UPImesh Frontend - Offline-First Implementation
 * Core logic for local wallet, offline packet generation, and mesh sync.
 */

// --- Configuration & Constants ---
const CONFIG = {
    STORAGE_KEY: 'upimesh_outbound_queue',
    AUTH_KEY: 'upimesh_auth_token',
    CLEANUP_DELAY: 5000,
    SYNC_RETRY_DELAY: 10000
};

const STATE = {
    isSyncing: false
};

// --- View Navigation ---
/**
 * Switches between the different app views (Wallet, Pay, Queue)
 * @param {string} viewId - ID of the view section to show
 * @param {HTMLElement} navElement - Optional nav button that was clicked
 */
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

// --- Local Storage (State Persistence) ---
/** @returns {Array} List of packets currently in the local queue */
function getQueue() {
    try {
        const data = localStorage.getItem(CONFIG.STORAGE_KEY);
        return data ? JSON.parse(data) : [];
    } catch (e) {
        console.error("Storage corruption detected, resetting queue.");
        localStorage.removeItem(CONFIG.STORAGE_KEY);
        return [];
    }
}

/** @param {Object} packet - The MeshPacket to add to local storage */
function addToQueue(packet) {
    const queue = getQueue();
    queue.unshift(packet);
    localStorage.setItem(CONFIG.STORAGE_KEY, JSON.stringify(queue));
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
        localStorage.setItem(CONFIG.STORAGE_KEY, JSON.stringify(queue));
        renderQueue();
    }
}

/** Removes a packet from the queue (usually after settlement) */
function removeFromQueue(packetId) {
    const queue = getQueue().filter(p => p.packetId !== packetId);
    localStorage.setItem(CONFIG.STORAGE_KEY, JSON.stringify(queue));
    renderQueue();
}

// --- UI Rendering Engine ---
/** Renders the current queue to the DOM */
function renderQueue() {
    const queueList = document.getElementById('queue-list');
    if (!queueList) return;
    
    const queue = getQueue();

    if (queue.length === 0) {
        queueList.innerHTML = `
            <div class="empty-state">
                <p>No pending payments. You're all caught up!</p>
            </div>`;
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

// --- Network & Sync (Bridge Sync) ---
/** Attempts to push all pending packets to the bridge server */
async function flushQueue() {
    if (STATE.isSyncing || !navigator.onLine) return;
    
    const queue = getQueue().filter(p => p.status !== 'SETTLED');
    if (queue.length === 0) return;

    STATE.isSyncing = true;
    console.log(`[Bridge Sync] Starting sync for ${queue.length} packets...`);

    const token = localStorage.getItem(CONFIG.AUTH_KEY) || 'DEV_TOKEN';

    for (const packet of queue) {
        try {
            const response = await fetch('/api/bridge/ingest', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json', 
                    'Authorization': `Bearer ${token}` 
                },
                body: JSON.stringify({ 
                    packetId: packet.packetId, 
                    cipherText: packet.cipherText 
                })
            });

            if (response.ok) {
                const result = await response.text();
                const txId = result.split('ID ')[1] || 'OK';
                updatePacketStatus(packet.packetId, 'SETTLED', txId);
                
                // Cleanup settled items after a delay
                setTimeout(() => removeFromQueue(packet.packetId), CONFIG.CLEANUP_DELAY);
            } else {
                console.warn(`[Bridge Sync] Failed to process ${packet.packetId}: ${response.status}`);
            }
        } catch (err) {
            console.error("[Bridge Sync] Network error during sync:", err);
            break; // Stop iteration on network failure
        }
    }

    STATE.isSyncing = false;
}

// --- Crypto & Packet Generation ---
/** Generates a payment packet (Uses backend if online, simulates if offline) */
async function generateOfflinePacket(receiver, amount) {
    if (navigator.onLine) {
        try {
            const response = await fetch('/api/demo/generate-packet');
            if (response.ok) {
                const packet = await response.json();
                return { 
                    ...packet, 
                    receiver, 
                    amount, 
                    timestamp: Date.now(), 
                    type: 'SECURE' 
                };
            }
        } catch (e) {
            console.warn("Backend encryption unavailable, switching to local mode.");
        }
    }

    // Local Simulation Mode
    return {
        packetId: 'offline-' + Math.random().toString(36).substr(2, 9),
        cipherText: btoa(JSON.stringify({ receiver, amount, nonce: Math.random() })),
        receiver, 
        amount, 
        timestamp: Date.now(), 
        type: 'SIMULATED'
    };
}

// --- Event Handlers & Initialization ---
const updateNetworkStatus = () => {
    const pulse = document.querySelector('.pulse');
    const badge = document.querySelector('.status-badge');
    const isOnline = navigator.onLine;

    if (pulse) pulse.style.backgroundColor = isOnline ? 'var(--accent-green)' : 'var(--accent-red)';
    if (badge) {
        badge.innerHTML = isOnline 
            ? '<span class="pulse" style="background-color: var(--accent-green)"></span> Online (Bridge Active)'
            : '<span class="pulse" style="background-color: var(--accent-red)"></span> Offline Mode Active';
    }

    if (isOnline) flushQueue();
};

window.addEventListener('online', updateNetworkStatus);
window.addEventListener('offline', updateNetworkStatus);

document.addEventListener('DOMContentLoaded', () => {
    // Initial status check
    updateNetworkStatus();
    
    // Initial UI render
    renderQueue();

    // Setup Payment Form Listener
    const form = document.getElementById('payment-form');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const vpa = document.getElementById('vpa').value;
            const amount = document.getElementById('amount').value;
            const btn = e.target.querySelector('button');

            btn.disabled = true;
            btn.innerHTML = '🔒 Encrypting...';

            try {
                const packet = await generateOfflinePacket(vpa, amount);
                addToQueue(packet);
                e.target.reset();
                switchView('view-queue');
            } catch (err) {
                console.error("Payment generation failed:", err);
                alert("Critical error during encryption. Payment aborted.");
            } finally {
                btn.disabled = false;
                btn.innerHTML = '<span class="icon">🔒</span> Encrypt & Queue Payment';
            }
        });
    }
});
