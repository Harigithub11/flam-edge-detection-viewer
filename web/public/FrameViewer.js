/**
 * FrameViewer class for displaying processed frames on HTML5 Canvas
 */
export class FrameViewer {
    constructor(config) {
        this.currentRotation = 0; // Track rotation in degrees (0, 90, 180, 270)
        this.currentImage = null; // Store current image for rotation
        this.currentMetadata = null; // Store metadata
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
            // Store current image and metadata for rotation
            this.currentImage = img;
            this.currentMetadata = frameData.metadata;
            this.currentRotation = 0; // Reset rotation on new frame
            const frameWidth = frameData.metadata.width;
            const frameHeight = frameData.metadata.height;
            const isLandscape = frameData.metadata.isLandscape || false;
            // Hide placeholder, show canvas
            if (this.placeholder) {
                this.placeholder.classList.add('hidden');
            }
            this.canvas.classList.add('visible');
            // Use device orientation to determine display
            // Portrait: rotate 90° clockwise to make it tall
            // Landscape: display as-is (wide)
            if (isLandscape) {
                this.displayLandscapeFrame(img, frameWidth, frameHeight);
            }
            else {
                this.displayPortraitFrame(img, frameWidth, frameHeight);
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
    /**
     * Rotate the current frame by 90 degrees clockwise
     */
    rotateFrame() {
        if (!this.currentImage || !this.currentMetadata) {
            console.warn('No frame to rotate');
            return;
        }
        // Increment rotation by 90 degrees
        this.currentRotation = (this.currentRotation + 90) % 360;
        // Redraw with new rotation
        this.renderFrameWithRotation();
        console.log(`Frame rotated to ${this.currentRotation}°`);
    }
    /**
     * Render frame with current rotation
     */
    renderFrameWithRotation() {
        if (!this.currentImage || !this.currentMetadata)
            return;
        const frameWidth = this.currentMetadata.width;
        const frameHeight = this.currentMetadata.height;
        const isLandscape = this.currentMetadata.isLandscape || false;
        // Calculate total rotation (base rotation + user rotation)
        let totalRotation = this.currentRotation;
        // Add base rotation for portrait mode (90 degrees)
        if (!isLandscape) {
            totalRotation += 90;
        }
        // Normalize to 0-360
        totalRotation = totalRotation % 360;
        // Set canvas dimensions based on total rotation
        if (totalRotation === 90 || totalRotation === 270) {
            // Swap dimensions for 90/270 rotation
            this.canvas.width = frameHeight;
            this.canvas.height = frameWidth;
        }
        else {
            // Normal dimensions for 0/180 rotation
            this.canvas.width = frameWidth;
            this.canvas.height = frameHeight;
        }
        // Clear and draw
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.save();
        this.ctx.translate(this.canvas.width / 2, this.canvas.height / 2);
        this.ctx.rotate(totalRotation * Math.PI / 180);
        this.ctx.drawImage(this.currentImage, -frameWidth / 2, -frameHeight / 2);
        this.ctx.restore();
    }
    /**
     * Reset viewer to initial state (show placeholder)
     */
    reset() {
        // Clear current frame data
        this.currentImage = null;
        this.currentMetadata = null;
        this.currentRotation = 0;
        // Clear canvas
        this.clear();
        // Hide canvas, show placeholder
        this.canvas.classList.remove('visible');
        if (this.placeholder) {
            this.placeholder.classList.remove('hidden');
        }
        console.log('Viewer reset');
    }
}
//# sourceMappingURL=FrameViewer.js.map