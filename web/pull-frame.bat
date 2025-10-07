@echo off
REM Pull the latest exported frame from Android device

echo Pulling latest frame from device...
adb shell "ls -t /sdcard/Pictures/EdgeDetectionViewer/*.png | head -1" > temp.txt
set /p LATEST_FRAME=<temp.txt
del temp.txt

if "%LATEST_FRAME%"=="" (
    echo No frames found on device!
    exit /b 1
)

echo Found: %LATEST_FRAME%
adb pull "%LATEST_FRAME%" "public\sample-frame.png"

echo Frame pulled successfully!
echo Refresh your browser to see the new frame.
