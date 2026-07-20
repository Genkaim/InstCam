<p align="center">
  <a href="https://genkaim.top" target="blank">
    <img src="/logo.png" alt="Logo" width="156" height="156">
  </a>
  <h2 align="center" style="font-weight: 600">InstCam</h2>
  <p align="center">
   一款拍立得风格的 Android 相机应用
  </p>
</p>

## 特色

- **取景框 morph 动画** — 取景框在「折叠态（灵动岛）」与「展开态（相机）」之间无缝过渡，效果跟手流畅
- **拍立得动画** — 拍照后照片从灵动岛位置"打印"而出，播放完整过渡动画，支持灵动岛位置自定义
- **实时滤镜** — 左右滑动调节色调/饱和度/亮度等参数，所见即所得
- **EXIF 数据** — 自动保留照片的位置信息，分享时移除保护隐私
- **响应式相册** — 下拉展开相册，支持多选删除/分享

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3
- **相机**：CameraX（Preview + ImageCapture + ProcessCameraProvider）
- **图片加载**：Coil
- **默认主题**：RetroBrown / RetroCream / RetroRust（复古胶片风）

## 安装

从 [GitHub Releases](https://github.com/Genkaim/InstCam/releases) 下载最新的 APK 安装包。

```bash
# 或者克隆后自行构建
git clone https://github.com/Genkaim/InstCam.git
cd InstCam
./gradlew assembleRelease
```

> 构建正式版需要签名密钥，详见 `keystore.properties` 示例。

## 构建

```bash
# Debug（无需签名）
./gradlew assembleDebug

# Release（需要签名配置）
./gradlew assembleRelease
```

## 许可证

本项目基于 [MIT License](LICENSE) 开源。
