import { FrameViewer } from './FrameViewer.js';
import { FrameData } from './types.js';

/**
 * Main application entry point
 */
class App {
    private viewer: FrameViewer;

    constructor() {
        console.log('🚀 Flam Edge Detection Viewer - Starting...');

        // Initialize viewer
        this.viewer = new FrameViewer({
            canvasId: 'frameCanvas',
            showStats: true,
            scalingMode: 'fit'
        });

        // Load sample frame
        this.loadSampleFrame();

        console.log('✅ Application initialized');
    }

    /**
     * Load and display sample frame
     */
    private loadSampleFrame(): void {
        // Sample frame data (replace with actual exported frame)
        const sampleFrame: FrameData = {
            imageData: './sample-frame.png',  // Path to exported frame
            metadata: {
                width: 1280,
                height: 720,
                fps: 15.3,
                processingTimeMs: 45.2,
                timestamp: Date.now(),
                mode: 'edges'
            }
        };

        // Display frame
        this.viewer.displayFrame(sampleFrame);

        // Update stats
        this.viewer.updateStats(sampleFrame.metadata);

        console.log('Sample frame loaded');
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
