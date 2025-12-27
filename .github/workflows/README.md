# GitHub Actions Workflow - Build APK

## Overview

This repository now includes a GitHub Actions workflow that automatically builds the Bluecord APK whenever you push code to specific branches.

## Workflow File

The workflow is defined in `.github/workflows/build-apk.yml`

## Triggers

The workflow runs automatically on:
- **Push** to branches:
  - `main`
  - `copilot/build-apk-using-workflows`
  - `copilot/add-inbuilt-voice-changer`
- **Pull requests** to `main` branch
- **Manual trigger** via the Actions tab (workflow_dispatch)

## How to Get the APK

### First Time Setup

1. **Approve the Workflow** (first run only):
   - Go to your repository on GitHub
   - Click the "Actions" tab at the top
   - You may see a message asking to approve workflows
   - Click "I understand my workflows, go ahead and enable them"

### Download APK from Workflow Run

1. **Navigate to Actions**:
   - Go to https://github.com/Tanmayop9/Bluecord-OWN/actions

2. **Select the Workflow**:
   - Click on "Build APK" in the left sidebar
   - You'll see a list of workflow runs

3. **Choose a Run**:
   - Click on any completed run (green checkmark means success)
   - Look for the most recent run if you want the latest build

4. **Download Artifacts**:
   - Scroll down to the "Artifacts" section at the bottom
   - Click on "bluecord-debug-apk" to download
   - The download will be a ZIP file containing `app-debug.apk`

5. **Extract and Install**:
   ```bash
   # Extract the ZIP
   unzip bluecord-debug-apk.zip
   
   # Install on your device via ADB
   adb install app-debug.apk
   
   # Or transfer to your phone and install manually
   ```

### Manual Workflow Trigger

You can also trigger the build manually:

1. Go to https://github.com/Tanmayop9/Bluecord-OWN/actions
2. Click "Build APK" in the left sidebar
3. Click the "Run workflow" button (top right)
4. Select the branch you want to build from
5. Click "Run workflow"
6. Wait for the build to complete
7. Download the artifact as described above

## What Gets Built

The workflow builds:
- **Debug APK**: Unsigned debug build suitable for testing
- Located at: `app/build/outputs/apk/debug/app-debug.apk`

## Build Environment

The workflow uses:
- **OS**: Ubuntu Latest
- **Java**: JDK 17 (Temurin distribution)
- **Android SDK**: Automatically installed
- **Gradle**: Version specified in gradle wrapper

## Build Process

The workflow executes these steps:
1. ✅ Checkout code from repository
2. ✅ Set up Java 17
3. ✅ Set up Android SDK
4. ✅ Make gradlew executable
5. ✅ Build debug APK using `./gradlew assembleDebug`
6. ✅ Upload APK as artifact (retained for 90 days by default)

## Troubleshooting

### Workflow Not Running

**Problem**: Workflow shows "action_required" status
**Solution**: 
- First-time workflows need approval
- Go to Actions tab and approve the workflow
- Re-run the workflow

**Problem**: Workflow fails during build
**Solution**:
- Check the workflow logs by clicking on the failed run
- Common issues:
  - Missing dependencies (check build.gradle)
  - Gradle sync failures
  - Build tool version mismatches

### Can't Download Artifact

**Problem**: No artifacts section visible
**Solution**:
- Make sure the workflow completed successfully (green checkmark)
- Artifacts are only created after successful builds
- Artifacts expire after 90 days

### Build Fails

Check the workflow logs:
1. Click on the failed workflow run
2. Click on "build" job
3. Expand the failed step to see error details
4. Common fixes:
   - Update dependencies in build.gradle
   - Check Android SDK requirements
   - Verify Kotlin version compatibility

## APK Details

### Debug APK Properties
- **Package Name**: `com.bluecord`
- **Version Code**: 1
- **Version Name**: 1.0
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 30
- **Compile SDK**: 34

### APK Size
- Expected size: ~12-15 MB (debug build)

## Security Note

The debug APK is signed with a debug keystore and is suitable for testing only. For production releases, you would need to:
1. Create a release keystore
2. Add signing configuration to build.gradle
3. Store keystore credentials in GitHub Secrets
4. Update workflow to build release APK

## Workflow Badge

Add this badge to your README to show build status:

```markdown
[![Build APK](https://github.com/Tanmayop9/Bluecord-OWN/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Tanmayop9/Bluecord-OWN/actions/workflows/build-apk.yml)
```

## Customization

### Change Branches

To build from different branches, edit `.github/workflows/build-apk.yml`:

```yaml
on:
  push:
    branches: [ your-branch-name ]
```

### Build Release APK

To build a release APK instead, change:

```yaml
- name: Build Release APK
  run: ./gradlew assembleRelease --stacktrace
```

And update the artifact path:

```yaml
path: app/build/outputs/apk/release/app-release-unsigned.apk
```

## Local Build (Alternative)

If you prefer to build locally instead of using GitHub Actions:

```bash
# Clone the repository
git clone https://github.com/Tanmayop9/Bluecord-OWN.git
cd Bluecord-OWN

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Find APK at
ls app/build/outputs/apk/debug/app-debug.apk
```

## Support

If you encounter issues:
1. Check the workflow logs in the Actions tab
2. Review the BUILD_INSTRUCTIONS.md for manual build steps
3. Open an issue on GitHub with:
   - Error message from workflow logs
   - Branch you're building from
   - Link to the failed workflow run
