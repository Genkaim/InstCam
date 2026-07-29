<p align="center">
  <a href="https://genkaim.top" target="blank">
    <img src="/logo.png" alt="Logo" width="156" height="156">
  </a>
  <h2 align="center" style="font-weight: 600">InstCam</h2>
  <p align="center">
    一款拍立得风格的 Android 相机应用
    <br>
  </p>
</p>

简体中文  [English](/README_en.md)

## 特色

- **取景框 morph 动画** — 取景框在「折叠态（灵动岛）」与「展开态（相机）」之间无缝过渡，效果跟手流畅
- **拍立得打印动画** — 拍照后照片从灵动岛位置“打印”而出，播放完整过渡动画，支持灵动岛位置自定义
- **内置编辑器** — 可调节曝光、饱和度、对比度，叠加黑白/暗角/暖色/冷色滤镜，并可添加/更换拍立得白框颜色
- **毛玻璃相框** — 拍立得边框支持毛玻璃质感，模糊程度可控
- **手动相机参数** — 支持手动对焦距离和快门速度调节，左右横滑操控

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3
- **相机**：CameraX（Preview + ImageCapture + ProcessCameraProvider）
- **图片加载**：Coil（AsyncImage）

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

# 建议先 clean 再打包，避免缓存问题
./gradlew clean assembleRelease
```

## 许可证

本项目基于 [MIT License](LICENSE) 开源。
