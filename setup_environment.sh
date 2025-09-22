#!/bin/bash

echo "Setting up Android development environment for Essence project..."

# Update system
sudo apt update

# Install Java 11
echo "Installing Java 11..."
sudo apt install -y openjdk-11-jdk

# Verify Java installation
echo "Java version:"
java -version

# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> ~/.bashrc

# Install required packages
echo "Installing required packages..."
sudo apt install -y wget unzip

# Set up Android SDK directory
echo "Setting up Android SDK..."
mkdir -p ~/Android/Sdk
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc

# Download and install Android command line tools
echo "Downloading Android command line tools..."
cd ~/Android/Sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true

# Accept licenses and install SDK components
echo "Installing Android SDK components..."
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;34.0.0" "system-images;android-34;google_apis;x86_64"

# Create AVD for testing
echo "Creating Android Virtual Device..."
echo "no" | avdmanager create avd -n "EssenceTestDevice" -k "system-images;android-34;google_apis;x86_64"

# Reload environment variables
source ~/.bashrc

echo "Setup complete! You can now:"
echo "1. Build the project: ./gradlew build"
echo "2. Run tests: ./gradlew test"
echo "3. Start emulator: emulator -avd EssenceTestDevice"
echo "4. Install app: ./gradlew installDebug"

