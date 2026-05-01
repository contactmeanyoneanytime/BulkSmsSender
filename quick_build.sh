#!/bin/bash
echo "Building Bulk SMS Sender APK..."

cd /Users/mr.amir/downloads/android\ studio/BulkSmsSender

# Kill any existing Gradle daemons
./gradlew --stop

# Set JAVA_HOME correctly (using the Java from Android Studio)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify Java is found
echo "Using Java at: $JAVA_HOME"
java -version

# Clean and build
echo "Starting build..."
./gradlew clean assembleDebug --no-daemon --stacktrace

if [ $? -eq 0 ]; then
    echo "✅ APK built successfully!"
    echo "📱 Location: app/build/outputs/apk/debug/app-debug.apk"
    ls -la app/build/outputs/apk/debug/
else
    echo "❌ Build failed"
fi