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
        // Get placeholder element
        this.placeholder = document.getElementById('placeholderImage');
        console.log('FrameViewer initialized');
    }
    /**
     * Display a frame on the canvas
     */
    displayFrame(frameData) {
        const img = new Image();
        img.onload = () => {
            const frameWidth = frameData.metadata.width;
            const frameHeight = frameData.metadata.height;
            // Hide placeholder, show canvas
            if (this.placeholder) {
                this.placeholder.classList.add('hidden');
            }
            this.canvas.classList.add('visible');
            // Check if Android is in portrait (sends wide image)
            const isAndroidPortrait = frameWidth > frameHeight;
            if (isAndroidPortrait) {
                this.displayPortraitFrame(img, frameWidth, frameHeight);
            }
            else {
                this.displayLandscapeFrame(img, frameWidth, frameHeight);
            }
        };
        img.onerror = (error) => {
            console.error('Failed to load image:', error);
        };
        // Set image source (triggers load)
        img.src = frameData.imageData;
    }
    /**
     * Display portrait frame (rotate 90 degrees clockwise)
     */
    displayPortraitFrame(img, frameWidth, frameHeight) {
        // Portrait: rotate 90 degrees clockwise (right)
        this.canvas.width = frameHeight;
        this.canvas.height = frameWidth;
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.save();
        this.ctx.translate(this.canvas.width / 2, this.canvas.height / 2);
        this.ctx.rotate(90 * Math.PI / 180); // 90 degrees clockwise
        this.ctx.drawImage(img, -frameWidth / 2, -frameHeight / 2);
        this.ctx.restore();
        console.log(`Portrait rotated 90° right: ${frameWidth}x${frameHeight} → ${this.canvas.width}x${this.canvas.height}`);
    }
    /**
     * Display landscape frame (show as-is, wide)
     */
    displayLandscapeFrame(img, frameWidth, frameHeight) {
        // Landscape: show as-is (wide orientation)
        this.canvas.width = frameWidth;
        this.canvas.height = frameHeight;
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.drawImage(img, 0, 0);
        console.log(`Landscape no rotation: ${frameWidth}x${frameHeight}`);
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