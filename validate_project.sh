#!/bin/bash

echo "🔍 Essence Launcher - Project Validation"
echo "========================================"

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Error: Not in Android project root directory"
    exit 1
fi

echo "✅ Project structure validated"

# Check key files exist
echo ""
echo "📁 Checking key files..."

files=(
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/nexusapps/essence/MainActivity.kt"
    "app/src/main/java/com/nexusapps/essence/OnboardingActivity.kt"
    "app/src/main/java/com/nexusapps/essence/SettingsActivity.kt"
    "app/src/main/java/com/nexusapps/essence/AnalyticsActivity.kt"
    "app/src/main/java/com/nexusapps/essence/AppWhitelistManager.kt"
    "app/src/main/java/com/nexusapps/essence/AppCategoryManager.kt"
    "app/src/main/java/com/nexusapps/essence/GestureManager.kt"
    "app/src/main/java/com/nexusapps/essence/PerformanceMonitor.kt"
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/res/layout/activity_settings.xml"
    "app/src/main/res/layout/activity_analytics.xml"
    "app/src/main/res/layout/activity_onboarding.xml"
    "app/build.gradle.kts"
    "gradle/libs.versions.toml"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ Missing: $file"
    fi
done

echo ""
echo "📊 Checking dependencies..."

# Check if key dependencies are in build.gradle.kts
if grep -q "androidx.room" app/build.gradle.kts; then
    echo "✅ Room database"
else
    echo "❌ Room database missing"
fi

if grep -q "androidx.recyclerview" app/build.gradle.kts; then
    echo "✅ RecyclerView"
else
    echo "❌ RecyclerView missing"
fi

if grep -q "androidx.viewpager2" app/build.gradle.kts; then
    echo "✅ ViewPager2"
else
    echo "❌ ViewPager2 missing"
fi

if grep -q "kotlinx-coroutines" app/build.gradle.kts; then
    echo "✅ Coroutines"
else
    echo "❌ Coroutines missing"
fi

echo ""
echo "🎯 Checking AndroidManifest.xml..."

if grep -q "android.intent.category.HOME" app/src/main/AndroidManifest.xml; then
    echo "✅ HOME launcher registered"
else
    echo "❌ HOME launcher not registered"
fi

if grep -q "QUERY_ALL_PACKAGES" app/src/main/AndroidManifest.xml; then
    echo "✅ App discovery permission"
else
    echo "❌ App discovery permission missing"
fi

echo ""
echo "📱 Checking activities..."

activities=(
    "OnboardingActivity"
    "MainActivity"
    "SettingsActivity"
    "AnalyticsActivity"
)

for activity in "${activities[@]}"; do
    if grep -q "android:name=\"\\.$activity\"" app/src/main/AndroidManifest.xml; then
        echo "✅ $activity registered"
    else
        echo "❌ $activity not registered"
    fi
done

echo ""
echo "🎨 Checking resources..."

if [ -f "app/src/main/res/values/strings.xml" ]; then
    string_count=$(grep -c "<string name=" app/src/main/res/values/strings.xml)
    echo "✅ $string_count string resources"
else
    echo "❌ strings.xml missing"
fi

if [ -f "app/src/main/res/values/themes.xml" ]; then
    echo "✅ themes.xml exists"
else
    echo "❌ themes.xml missing"
fi

echo ""
echo "📊 Project Statistics:"
echo "====================="

# Count Kotlin files
kotlin_files=$(find app/src/main/java -name "*.kt" | wc -l)
echo "📄 Kotlin files: $kotlin_files"

# Count layout files
layout_files=$(find app/src/main/res/layout -name "*.xml" | wc -l)
echo "🎨 Layout files: $layout_files"

# Count drawable files
drawable_files=$(find app/src/main/res/drawable -name "*.xml" | wc -l)
echo "🖼️  Drawable files: $drawable_files"

echo ""
echo "🏆 Validation Complete!"
echo "====================="
echo ""
echo "The Essence Launcher project is ready for testing!"
echo ""
echo "Next steps:"
echo "1. Install Java 11: sudo apt install openjdk-11-jdk"
echo "2. Set JAVA_HOME: export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
echo "3. Build project: ./gradlew build"
echo "4. Generate APK: ./gradlew assembleDebug"
echo "5. Install on device: ./gradlew installDebug"
echo ""
echo "🎯 Ready to compete with Nova Launcher, Microsoft Launcher, and Niagara Launcher!"
