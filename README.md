name: Build Messi Android APK

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Android API 36
        run: sdkmanager "platforms;android-36" "build-tools;35.0.0"

      - name: Set up Gradle 8.13
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.13'

      - name: Unzip project
        run: |
          mkdir -p src
          unzip -q messi-android-studio-v2.zip -d src
          test -f src/messi-android-studio/settings.gradle

      - name: Build debug APK
        working-directory: src/messi-android-studio
        run: gradle :app:assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: messi-debug-apk
          path:
