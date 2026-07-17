# Picocam / InstCam 项目长期记忆

## 项目
- 名称：InstCam（原 Picocam，已重命名）
- 技术栈：Kotlin + Jetpack Compose (BOM 2025.09.00) + Material3 + CameraX
- 包名：`com.genkaim.picocam`
- 主题：复古胶片风（深棕 + 奶油白 + 铁锈红）

## 关键模块
- `ui/CameraScreen.kt`：相机主页（取景框 + 操控区 + 相册 + 效果面板）
- `ui/components/ViewfinderFrame.kt`：取景框 + PreviewView 挂载
- `ui/components/ShutterButton.kt`：拍照按钮
- `ui/components/CaptureTransitionOverlay.kt`：拍照后"取景框→灵动岛→打印→放大→详情"过渡
- `ui/components/EffectsPanel.kt`：滤镜 / 调色盘面板
- `camera/PhotoStorage.kt`：照片落盘 + EXIF 旋转 + 滤镜 + 拍立得白边

## 重要约定
- 取景框折叠态 (`progressState=0f`)：ViewfinderFrame 内部边框用 `Modifier.border`（draw 层），**不要**改回 `padding`——否则 AndroidView 高度变 0，cameraX 永不 STREAMING
- 拍照流程：`_shutterFlash.tryEmit` 提前闪白；`_photoCaptured.emit` 在 JPEG 落盘后**立即**发射（不等 addPolaroidFrame），addPolaroidFrame 放后台异步跑。**过渡动画的 AsyncImage 必须延迟到 Extend 之后才渲染**（`showPhoto` 状态），确保 Coil 读到的是加完白框的版本
- 深色模式主背景 `#242424`，卡片表面 `#2E2E2E`，前景统一白色
- ShutterButton 圆+描边在深色背景下要用 `graphicsLayer { compositingStrategy = Offscreen }` 消除毛边
- `onAddToAlbum` 中务必**先** `setPlaceholder` **再** `refreshPhotos`，避免相册显示原图闪现

## 取景框 morph 关键尺寸
- 折叠态外框：`isW × isH`（灵动岛设置宽高）
- 展开态外框：`vfW × vfW`（取景框设置宽度，正方形）
- 边框厚度：`diConfig.heightDp.coerceIn(0, vfConfig.widthDp / 2)`（典型 32dp）
  - 折叠态：厚度 = 岛高 → 覆盖整张小岛
  - 展开态：厚度 ≈ 32dp → 取景框薄边框
