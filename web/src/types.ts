/**
 * Frame metadata containing performance and processing information
 */
export interface FrameMetadata {
    /** Frame width in pixels */
    width: number;

    /** Frame height in pixels */
    height: number;

    /** Frames per second at capture time */
    fps: number;

    /** Processing time in milliseconds */
    processingTimeMs: number;

    /** Frame timestamp (Unix epoch in milliseconds) */
    timestamp: number;

    /** Optional processing mode */
    mode?: 'raw' | 'edges' | 'grayscale';

    /** Frame state */
    state?: 'live' | 'frozen' | 'saved';

    /** Device orientation when frame was captured */
    isLandscape?: boolean;
}

/**
 * Complete frame data including image and metadata
 */
export interface FrameData {
    /** Image data as base64 string or URL */
    imageData: string;

    /** Associated metadata */
    metadata: FrameMetadata;
}

/**
 * Configuration options for FrameViewer
 */
export interface ViewerConfig {
    /** Canvas element ID */
    canvasId: string;

    /** Enable stats display */
    showStats?: boolean;

    /** Canvas scaling mode */
    scalingMode?: 'fit' | 'fill' | 'stretch';
}

/**
 * WebSocket message structure (optional)
 */
export interface WebSocketMessage {
    type: 'frame' | 'stats' | 'config';
    data: FrameData | FrameMetadata | any;
}
