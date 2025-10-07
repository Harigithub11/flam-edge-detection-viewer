import { FrameViewer } from './FrameViewer.js';
import { WebSocketClient } from './WebSocketClient.js';
/**
 * Main application entry point
 */
class App {
    constructor() {
        console.log('🚀 Flam Edge Detection Viewer - Starting...');
        // Initialize viewer
        this.viewer = new FrameViewer({
            canvasId: 'frameCanvas',
            showStats: true,
            scalingMode: 'fit'
        });
        // Initialize WebSocket client
        this.wsClient = new WebSocketClient();
        // Setup WebSocket callbacks
        this.setupWebSocketCallbacks();
        // Setup connection UI
        this.setupConnectionUI();
        // Try to connect if device IP is provided
        this.tryAutoConnect();
        console.log('✅ Application initialized');
    }
    /**
     * Setup WebSocket callbacks
     */
    setupWebSocketCallbacks() {
        // Handle incoming frames
        this.wsClient.onFrame((frameData) => {
            // Display frame
            this.viewer.displayFrame(frameData);
            // Update stats
            this.viewer.updateStats(frameData.metadata);
            // Update mode display
            this.updateModeDisplay(frameData.metadata.mode || 'unknown');
            // Update UI based on state
            if (frameData.metadata.state) {
                this.updateUIState(frameData.metadata.state);
            }
        });
        // Handle state changes
        this.wsClient.onStateChange((state, mode) => {
            console.log(`State change: ${state}, mode: ${mode}`);
            this.updateUIState(state);
            this.updateModeDisplay(mode);
        });
        // Handle status updates
        this.wsClient.onStatus((status) => {
            const statusElement = document.getElementById('connectionStatus');
            if (statusElement) {
                statusElement.textContent = status;
                // Update color based on status
                if (status === 'Connected') {
                    statusElement.style.color = '#00ff88';
                }
                else if (status.includes('Reconnecting')) {
                    statusElement.style.color = '#ffaa00';
                }
                else {
                    statusElement.style.color = '#ff4444';
                }
            }
        });
    }
    /**
     * Setup connection UI
     */
    setupConnectionUI() {
        const connectButton = document.getElementById('connectButton');
        const ipInput = document.getElementById('deviceIP');
        if (connectButton && ipInput) {
            connectButton.addEventListener('click', () => {
                const ip = ipInput.value.trim();
                if (ip) {
                    localStorage.setItem('deviceIP', ip);
                    this.wsClient.connect(ip);
                }
            });
        }
    }
    /**
     * Try to auto-connect using saved IP
     */
    tryAutoConnect() {
        const savedIP = localStorage.getItem('deviceIP');
        if (savedIP) {
            const ipInput = document.getElementById('deviceIP');
            if (ipInput) {
                ipInput.value = savedIP;
            }
            console.log(`🔄 Auto-connecting to ${savedIP}...`);
            this.wsClient.connect(savedIP);
        }
    }
    /**
     * Update mode display
     */
    updateModeDisplay(mode) {
        const modeElement = document.getElementById('modeDisplay');
        if (modeElement) {
            let modeText = '';
            switch (mode) {
                case 'raw':
                    modeText = 'Raw Feed';
                    break;
                case 'edges':
                    modeText = 'Canny Edge Detection';
                    break;
                case 'grayscale':
                    modeText = 'Grayscale';
                    break;
                default:
                    modeText = mode;
            }
            modeElement.textContent = modeText;
        }
    }
    /**
     * Update UI based on capture state
     */
    updateUIState(state) {
        const captureActions = document.getElementById('captureActions');
        if (state === 'frozen') {
            // Show confirm/retake buttons
            if (captureActions) {
                captureActions.style.display = 'flex';
            }
        }
        else if (state === 'live' || state === 'saved') {
            // Hide confirm/retake buttons
            if (captureActions) {
                captureActions.style.display = 'none';
            }
        }
    }
}
// Initialize app when DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new App();
    });
}
else {
    new App();
}
//# sourceMappingURL=index.js.map