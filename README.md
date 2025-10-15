# 🚀 GPTX v2.0 - Advanced AI Chat App

**Modern Android AI chat app with DeepSeek-inspired UI**

## ✨ Features
- 🎨 Modern Material 3 Design
- 🌓 Dark/Light theme toggle
- 📷 Image upload & vision analysis
- 💬 Real-time streaming chat
- 📊 Chat history export (PDF/Text)
- 🔒 Secure API proxy backend
- 🎯 Clean, professional UI

## 🛠️ Tech Stack
- 
# Fix AndroidManifest path
cat > ~/storage/shared/gptx/gptx-android/app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <application
        android:name=".GPTXApp"
        android:allowBackup="true"
        android:label="GPTX"
        android:usesCleartextTraffic="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.Material3.DayNight">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
