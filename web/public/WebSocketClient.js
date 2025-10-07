/**
 * WebSocket client for receiving real-time frames from Android app
 */
export class WebSocketClient {
    constructor() {
        this.socket = null;
        this.reconnectTimer = null;
        this.reconnectDelay = 2000; // 2 seconds
        this.maxReconnectDelay = 30000; // 30 seconds
        this.onFrameCallback = null;
        this.onStatusCallback = null;
        this.onStateChangeCallback = null;
    }
    /**
     * Connect to WebSocket server
     * @param host Android device IP address
     * @param port WebSocket server port (default: 8080)
     */
    connect(host, port = 8080) {
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
                    }
                    else if (data.type === 'stateChange' && this.onStateChangeCallback) {
                        // Handle state change notification
                        this.onStateChangeCallback(data.state, data.mode);
                        console.log(`🔄 State changed to: ${data.state}`);
                    }
                    else if (data.type === 'frame' && this.onFrameCallback) {
                        // Convert base64 to data URL
                        const imageData = `data:image/jpeg;base64,${data.imageData}`;
                        const frameData = {
                            imageData: imageData,
                            metadata: data.metadata
                        };
                        this.onFrameCallback(frameData);
                    }
                }
                catch (error) {
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
        }
        catch (error) {
            console.error('❌ Failed to create WebSocket:', error);
            this.updateStatus('Failed');
            this.scheduleReconnect(host, port);
        }
    }
    /**
     * Schedule reconnection attempt
     */
    scheduleReconnect(host, port) {
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
    onFrame(callback) {
        this.onFrameCallback = callback;
    }
    /**
     * Set callback for status updates
     */
    onStatus(callback) {
        this.onStatusCallback = callback;
    }
    /**
     * Set callback for state change updates
     */
    onStateChange(callback) {
        this.onStateChangeCallback = callback;
    }
    /**
     * Update connection status
     */
    updateStatus(status) {
        if (this.onStatusCallback) {
            this.onStatusCallback(status);
        }
    }
    /**
     * Disconnect from WebSocket
     */
    disconnect() {
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
    isConnected() {
        return this.socket !== null && this.socket.readyState === WebSocket.OPEN;
    }
    /**
     * Send message to server
     */
    send(message) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(message));
        }
        else {
            console.warn('⚠️ Cannot send message: WebSocket not connected');
        }
    }
}
//# sourceMappingURL=WebSocketClient.js.map