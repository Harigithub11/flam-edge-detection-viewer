import { FrameData } from './types.js';

/**
 * WebSocket client for receiving real-time frames from Android app
 */
export class WebSocketClient {
    private socket: WebSocket | null = null;
    private reconnectTimer: number | null = null;
    private reconnectDelay = 2000; // 2 seconds
    private maxReconnectDelay = 30000; // 30 seconds
    private onFrameCallback: ((frameData: FrameData) => void) | null = null;
    private onStatusCallback: ((status: string) => void) | null = null;

    /**
     * Connect to WebSocket server
     * @param host Android device IP address
     * @param port WebSocket server port (default: 8080)
     */
    connect(host: string, port: number = 8080): void {
        const url = `ws://${host}:${port}/stream`;
        console.log(`🔌 Connecting to WebSocket: ${url}`);

        try {
            this.socket = new WebSocket(url);

            this.socket.onopen = () => {
                console.log('✅ WebSocket connected');
                this.reconnectDelay = 2000; // Reset reconnect delay
                this.updateStatus('Connected');
            };

            this.socket.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);

                    if (data.type === 'connected') {
                        console.log('📱', data.message);
                    } else if (data.type === 'frame' && this.onFrameCallback) {
                        // Convert base64 to data URL
                        const imageData = `data:image/jpeg;base64,${data.imageData}`;

                        const frameData: FrameData = {
                            imageData: imageData,
                            metadata: data.metadata
                        };

                        this.onFrameCallback(frameData);
                    }
                } catch (error) {
                    console.error('❌ Failed to parse message:', error);
                }
            };

            this.socket.onerror = (error) => {
                console.error('❌ WebSocket error:', error);
                this.updateStatus('Error');
            };

            this.socket.onclose = () => {
                console.log('🔌 WebSocket disconnected');
                this.updateStatus('Disconnected');
                this.socket = null;
                this.scheduleReconnect(host, port);
            };

        } catch (error) {
            console.error('❌ Failed to create WebSocket:', error);
            this.updateStatus('Failed');
            this.scheduleReconnect(host, port);
        }
    }

    /**
     * Schedule reconnection attempt
     */
    private scheduleReconnect(host: string, port: number): void {
        if (this.reconnectTimer !== null) {
            return; // Already scheduled
        }

        console.log(`⏳ Reconnecting in ${this.reconnectDelay / 1000}s...`);
        this.updateStatus(`Reconnecting in ${this.reconnectDelay / 1000}s`);

        this.reconnectTimer = window.setTimeout(() => {
            this.reconnectTimer = null;
            this.connect(host, port);

            // Exponential backoff
            this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, this.maxReconnectDelay);
        }, this.reconnectDelay);
    }

    /**
     * Set callback for frame updates
     */
    onFrame(callback: (frameData: FrameData) => void): void {
        this.onFrameCallback = callback;
    }

    /**
     * Set callback for status updates
     */
    onStatus(callback: (status: string) => void): void {
        this.onStatusCallback = callback;
    }

    /**
     * Update connection status
     */
    private updateStatus(status: string): void {
        if (this.onStatusCallback) {
            this.onStatusCallback(status);
        }
    }

    /**
     * Disconnect from WebSocket
     */
    disconnect(): void {
        if (this.reconnectTimer !== null) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }

        this.updateStatus('Disconnected');
    }

    /**
     * Check if connected
     */
    isConnected(): boolean {
        return this.socket !== null && this.socket.readyState === WebSocket.OPEN;
    }

    /**
     * Send message to server
     */
    send(message: any): void {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(message));
        } else {
            console.warn('⚠️ Cannot send message: WebSocket not connected');
        }
    }
}
