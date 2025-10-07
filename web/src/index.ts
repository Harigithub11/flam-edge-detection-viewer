import { FrameViewer } from './FrameViewer.js';
import { WebSocketClient } from './WebSocketClient.js';
import { FrameData } from './types.js';

/**
 * Main application entry point
 */
class App {
    private viewer: FrameViewer;
    private wsClient: WebSocketClient;

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
    private setupWebSocketCallbacks(): void {
        // Handle incoming frames
        this.wsClient.onFrame((frameData: FrameData) => {
            // Display frame
            this.viewer.displayFrame(frameData);

            // Update stats
            this.viewer.updateStats(frameData.metadata);

            // Update mode display
            this.updateModeDisplay(frameData.metadata.mode || 'unknown');
        });

        // Handle status updates
        this.wsClient.onStatus((status: string) => {
            const statusElement = document.getElementById('connectionStatus');
            if (statusElement) {
                statusElement.textContent = status;

                // Update color based on status
                if (status === 'Connected') {
                    statusElement.style.color = '#00ff88';
                } else if (status.includes('Reconnecting')) {
                    statusElement.style.color = '#ffaa00';
                } else {
                    statusElement.style.color = '#ff4444';
                }
            }
        });
    }

    /**
     * Setup connection UI
     */
    private setupConnectionUI(): void {
        const connectButton = document.getElementById('connectButton');
        const ipInput = document.getElementById('deviceIP') as HTMLInputElement;

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
    private tryAutoConnect(): void {
        const savedIP = localStorage.getItem('deviceIP');
        if (savedIP) {
            const ipInput = document.getElementById('deviceIP') as HTMLInputElement;
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
    private updateModeDisplay(mode: string): void {
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
}

// Initialize app when DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new App();
    });
} else {
    new App();
}
