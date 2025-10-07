import { FrameViewer } from './FrameViewer.js';
import { FrameData } from './types.js';

/**
 * Main application entry point
 */
class App {
    private rawViewer: FrameViewer;
    private edgesViewer: FrameViewer;
    private grayscaleViewer: FrameViewer;

    constructor() {
        console.log('🚀 Flam Edge Detection Viewer - Starting...');

        // Initialize three viewers
        this.rawViewer = new FrameViewer({
            canvasId: 'rawCanvas',
            showStats: true,
            scalingMode: 'fit'
        });

        this.edgesViewer = new FrameViewer({
            canvasId: 'edgesCanvas',
            showStats: true,
            scalingMode: 'fit'
        });

        this.grayscaleViewer = new FrameViewer({
            canvasId: 'grayscaleCanvas',
            showStats: true,
            scalingMode: 'fit'
        });

        // Load sample frames
        this.loadSampleFrames();

        console.log('✅ Application initialized');
    }

    /**
     * Load and display sample frames
     */
    private loadSampleFrames(): void {
        // Sample metadata
        const metadata = {
            width: 1280,
            height: 720,
            fps: 15.3,
            processingTimeMs: 45.2,
            timestamp: Date.now()
        };

        // Raw frame
        const rawFrame: FrameData = {
            imageData: './sample-frame-raw.png',
            metadata: { ...metadata, mode: 'raw' as const }
        };

        // Edges frame
        const edgesFrame: FrameData = {
            imageData: './sample-frame-edges.png',
            metadata: { ...metadata, mode: 'edges' as const }
        };

        // Grayscale frame
        const grayscaleFrame: FrameData = {
            imageData: './sample-frame-grayscale.png',
            metadata: { ...metadata, mode: 'grayscale' as const }
        };

        // Display frames
        this.rawViewer.displayFrame(rawFrame);
        this.edgesViewer.displayFrame(edgesFrame);
        this.grayscaleViewer.displayFrame(grayscaleFrame);

        // Update stats (using edges frame metadata)
        this.rawViewer.updateStats(edgesFrame.metadata);

        console.log('Sample frames loaded');
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
