<p align="center">
  <a href="https://genkaim.top" target="blank">
    <img src="/logo.png" alt="Logo" width="156" height="156">
  </a>
  <h2 align="center" style="font-weight: 600">InstCam</h2>
  <p align="center">
    一款拍立得风格的 Android 相机应用
    <br>
    <a href="README.en.md">English</a>
  </p>
</p>

## 特色

- **取景框 morph 动画** — 取景框在「折叠态（灵动岛）」与「展开态（相机）」之间无缝过渡，效果跟手流畅
- **拍立得打印动画** — 拍照后照片从灵动岛位置“打印”而出，播放完整过渡动画，支持灵动岛位置自定义
- **实时滤镜** — 左右滑动调节黑白 / 暗角 / 亮度等参数，所见即所得
- **内置编辑器** — 可调节曝光、饱和度、对比度，叠加黑白/暗角/暖色/冷色滤镜，并可添加/更换拍立得白框颜色
- **毛玻璃相框** — 拍立得边框支持毛玻璃质感，模糊程度可控
- **手动相机参数** — 支持手动对焦距离和快门速度调节，左右横滑操控
- **音效系统** — 可开启/关闭快门音效和拍立得打印音效
- **导入照片** — 从系统相册选择照片导入，自动添加拍立得白框
- **EXIF 数据** — 自动保留照片的位置信息，分享时移除保护隐私
- **多语言** — 支持简体中文、繁体中文、English，可跟随系统或手动切换
- **响应式相册** — 下拉展开相册，支持多选删除/分享
- **多主题** — 浅色、深色、跟随系统三种模式可切换
- **动画总开关** — 关闭后仅正常拍照，不播放拍立得动画

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3
- **相机**：CameraX（Preview + ImageCapture + ProcessCameraProvider）
- **图片加载**：Coil（AsyncImage）
- **音频**：SoundPool（低延迟音效播放）
- **存储**：DataStore（偏好设置持久化）
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

# 建议先 clean 再打包，避免缓存问题
./gradlew clean assembleRelease
```

## 自定义取景框

1. 打开设置 → 灵动岛与取景框设置
2. 点按「灵动岛」记录当前取景框位置和尺寸作为折叠态
3. 点按「取景框」记录展开态的尺寸/圆角

## 效果面板

从取景框底部上滑或点击「特效」按钮展开效果面板：
- **右侧滑块**：黑白 / 暗角 / 亮度（可分别调节强度）
- **左侧调色盘**：暖色 / 冷色（调节饱和度与明暗）
- **还原按钮**：一键清除所有滤镜

## 许可证

本项目基于 [MIT License](LICENSE) 开源。
