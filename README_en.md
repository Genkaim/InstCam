<p align="center">
  <a href="https://genkaim.top" target="blank">
    <img src="/logo.png" alt="Logo" width="156" height="156">
  </a>
  <h2 align="center" style="font-weight: 600">InstCam</h2>
  <p align="center">
    A Polaroid-style Android camera app
    <br>
  </p>
</p>

[简体中文](/README.md)  English

## Features

- **Viewfinder morph animation** — Seamless transition between "collapsed (Dynamic Island)" and "expanded (camera)" viewfinder states
- **Polaroid print animation** — Photo "prints out" from the Dynamic Island after capture, with a full transition animation and customizable island position
- **Built-in editor** — Adjust exposure, saturation, contrast; apply B&W/vignette/warm/cool filters; change Polaroid frame colors
- **Frosted glass frame** — Polaroid border supports a frosted glass texture with adjustable blur

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Camera**: CameraX (Preview + ImageCapture + ProcessCameraProvider)
- **Image loading**: Coil (AsyncImage)

## Installation

Download the latest APK from [GitHub Releases](https://github.com/Genkaim/InstCam/releases).

```bash
# Or clone and build yourself
git clone https://github.com/Genkaim/InstCam.git
cd InstCam
./gradlew assembleRelease
```

> Building a release requires signing keys — see `keystore.properties` example.

## Building

```bash
# Debug (no signing required)
./gradlew assembleDebug

# Release (signing config required)
./gradlew assembleRelease

# Clean build recommended to avoid cache issues
./gradlew clean assembleRelease
```

## License

This project is licensed under the [MIT License](LICENSE).
