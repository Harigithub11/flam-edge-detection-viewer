/**
 * FrameViewer class for displaying processed frames on HTML5 Canvas
 */
export class FrameViewer {
    constructor(config) {
        this.config = config;
        // Get canvas element
        const canvasElement = document.getElementById(this.config.canvasId);
        if (!canvasElement || !(canvasElement instanceof HTMLCanvasElement)) {
            throw new Error(`Canvas element with ID '${this.config.canvasId}' not found`);
        }
        this.canvas = canvasElement;
        // Get 2D context
        const context = this.canvas.getContext('2d');
        if (!context) {
            throw new Error('Failed to get 2D context from canvas');
        }
        this.ctx = context;
        console.log('FrameViewer initialized');
    }
    /**
     * Display a frame on the canvas
     */
    displayFrame(frameData) {
        const img = new Image();
        img.onload = () => {
            // Set canvas size to match image
            this.canvas.width = frameData.metadata.width;
            this.canvas.height = frameData.metadata.height;
            // Clear canvas
            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            // Draw image
            this.ctx.drawImage(img, 0, 0);
            console.log(`Frame displayed: ${frameData.metadata.width}x${frameData.metadata.height}`);
        };
        img.onerror = (error) => {
            console.error('Failed to load image:', error);
        };
        // Set image source (triggers load)
        img.src = frameData.imageData;
    }
    /**
     * Update stats display
     */
    updateStats(metadata) {
        // FPS
        const fpsElement = document.getElementById('fpsDisplay');
        if (fpsElement) {
            fpsElement.textContent = metadata.fps.toFixed(1);
        }
        // Resolution
        const resolutionElement = document.getElementById('resolutionDisplay');
        if (resolutionElement) {
            resolutionElement.textContent = `${metadata.width}x${metadata.height}`;
        }
        // Processing Time
        const processingTimeElement = document.getElementById('processingTimeDisplay');
        if (processingTimeElement) {
            processingTimeElement.textContent = `${metadata.processingTimeMs.toFixed(2)} ms`;
        }
        // Timestamp
        const timestampElement = document.getElementById('timestampDisplay');
        if (timestampElement) {
            const date = new Date(metadata.timestamp);
            timestampElement.textContent = date.toLocaleTimeString();
        }
    }
    /**
     * Clear the canvas
     */
    clear() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }
    /**
     * Get canvas dimensions
     */
    getDimensions() {
        return {
            width: this.canvas.width,
            height: this.canvas.height
        };
    }
}
//# sourceMappingURL=FrameViewer.js.map