#!/usr/bin/env node

/**
 * Frame sync utility - Pulls latest exported frame from Android device
 * Usage: node sync-frames.js [--watch]
 */

import { execSync } from 'child_process';
import { existsSync, writeFileSync } from 'fs';
import path from 'path';

const DEVICE_PATH = '/sdcard/Pictures/EdgeDetectionViewer';
const LOCAL_PATH = './public/sample-frame.png';
const ADB_PATH = 'C:\\Users\\enguv\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe';

function pullLatestFrame() {
    try {
        console.log('📱 Checking for frames on device...');

        // Get latest frame
        const result = execSync(
            `"${ADB_PATH}" shell "ls -t ${DEVICE_PATH}/*.png 2>/dev/null | head -1"`,
            { encoding: 'utf8' }
        ).trim();

        if (!result) {
            console.log('❌ No frames found on device');
            return false;
        }

        console.log(`📥 Pulling: ${result}`);

        // Pull frame
        execSync(`"${ADB_PATH}" pull "${result}" "${LOCAL_PATH}"`, { encoding: 'utf8' });

        console.log('✅ Frame synced successfully!');
        console.log('🔄 Refresh your browser to see the new frame');
        return true;

    } catch (error) {
        console.error('❌ Error:', error.message);
        return false;
    }
}

// Watch mode
const watchMode = process.argv.includes('--watch');

if (watchMode) {
    console.log('👀 Watching for new frames (Ctrl+C to stop)...\n');

    setInterval(() => {
        pullLatestFrame();
    }, 2000); // Check every 2 seconds

} else {
    pullLatestFrame();
}
