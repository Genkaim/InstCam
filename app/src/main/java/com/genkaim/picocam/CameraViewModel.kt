package com.genkaim.picocam

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.location.Location
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genkaim.picocam.camera.PhotoStorage
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.genkaim.picocam.camera.BRIGHTNESS_DEFAULT
import com.genkaim.picocam.camera.BRIGHTNESS_KEY
import com.genkaim.picocam.camera.EffectiveFilter
import com.genkaim.picocam.camera.FILTER_KEYS
import com.genkaim.picocam.camera.FilterParams

/** 进程内单例：DataStore 存于 context.filesDir/datastore/filter_state.preferences_pb。 */
private val Context.filterDataStore: DataStore<Preferences> by preferencesDataStore(name = "filter_state")

/** 相册首屏一次性加载的最近照片数；其余在后台补全，避免进入页面时被全量目录扫描+排序阻塞。 */
private const val ALBUM_FIRST_PAGE = 60

/** 调色盘三态：无滤镜 / 暖色 / 冷色，由底部箭头循环切换，替代原独立的暖/冷滤镜。 */
enum class TintState { NONE, WARM, COOL }

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    // —— 相机绑定（用 ProcessCameraProvider 直接绑定 use case，强制 4:3 比例）——
    // 之前用 LifecycleCameraController，但它的 setPreviewResolutionSelector / setTargetAspectRatio
    // 都被默认「屏幕分辨率」覆盖（CameraController 官方文档明确说明：preview size 默认为屏幕分辨率或 1080p），
    // 导致预览用 9:20 缓冲、FILL_CENTER 裁切后比 4:3 成片窄 ~2.8×，预览异常放大。
    // 改用 ProcessCameraProvider + 自定义 Preview/ImageCapture（.setTargetAspectRatio(4:3)），
    // setTargetAspectRatio 是 use case 上的硬约束，不会被默认覆盖。
    val preview: Preview = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(application)
    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    /** 闪光灯模式（forward 到 imageCapture.flashMode，每次 rebind 后需重设）。 */
    var imageCaptureFlashMode: Int = ImageCapture.FLASH_MODE_AUTO
        set(value) {
            field = value
            imageCapture.flashMode = value
        }

    /**
     * ViewfinderFrame 的 AndroidView factory 创建 PreviewView 后回调此方法：
     * 把 PreviewView 的 surfaceProvider 接到 preview 上（已在 factory 块完成），
     * 并绑 use case 到 lifecycle。cameraProvider 异步初始化：若还没就绪，缓存请求并在就绪后立即绑定。
     */
    fun bindCameraUseCases(owner: LifecycleOwner) {
        lifecycleOwner = owner
        val provider = cameraProvider
        if (provider != null) {
            doBind()
        } else {
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                doBind()
            }, androidx.core.content.ContextCompat.getMainExecutor(getApplication()))
        }
    }

    private fun doBind() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        provider.unbindAll()
        boundCamera = provider.bindToLifecycle(owner, cameraSelector, preview, imageCapture)
        // 重新应用 flash 模式 + 焦段（bindToLifecycle 会重置）
        imageCapture.flashMode = imageCaptureFlashMode
        boundCamera?.cameraControl?.setZoomRatio(_zoomLevel.value)
    }

    private val _photos = MutableStateFlow<List<File>>(emptyList())
    val photos: StateFlow<List<File>> = _photos.asStateFlow()
    /** 是否已拍过照（按下快门即置 true）。用于空相册引导光晕：第一次拍照后不再显示，且不依赖相册里是否还有照片。 */
    private val _hasTakenPhoto = MutableStateFlow(false)
    val hasTakenPhoto: StateFlow<Boolean> = _hasTakenPhoto.asStateFlow()
    private val _flashMode = MutableStateFlow(FlashMode.AUTO)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()
    private val _isBackCamera = MutableStateFlow(true)
    val isBackCamera: StateFlow<Boolean> = _isBackCamera.asStateFlow()
    // SharedFlow 容量 =1 + DROP_OLDEST：保证 tryEmit 永不挂起、capture 期间瞬时反馈可被 collector 消费
    private val _shutterFlash = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val shutterFlash = _shutterFlash.asSharedFlow()
    /**
     * 快门按下即发射（在 CameraX 抓取前，文件已通过 generateFile 生成），携带目标路径，
     * 供相机页【立即】启动"取景框→灵动岛"过渡动画，使 morph 起点与按下快门同步——
     * 不再等待 onImageSaved，从而避免不同机型拍照延迟差异导致 morph 起点的时间间隔不一致。
     * 文件内容（加白框）何时真正在 overlay 显示仍由 photoCaptured + photoVersion 控制，
     * 故本信号只决定"动画何时开始"，与拍照硬件快慢无关。
     */
    private val _captureStarted = MutableSharedFlow<File>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val captureStarted = _captureStarted.asSharedFlow()
    /** 拍照完成（已落盘、已加相框、已刷新相册）后发射所拍文件，供相机页无动画路径处理占位/刷新相册，以及驱动照片内容就绪。 */
    private val _photoCaptured = MutableSharedFlow<File>()
    val photoCaptured = _photoCaptured.asSharedFlow()
    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    /** 相册缩略图缓存版本：每次 refreshPhotos 递增，迫使 Coil 内存缓存 key 变化，重新加载 Polaroid 后文件。 */
    private val _photoVersion = MutableStateFlow(0L)
    val photoVersion: StateFlow<Long> = _photoVersion.asStateFlow()

    /** 拍照详情态期间，相册首位显示为空白占位（照片已在列表中但不渲染图片），退出详情后占位变为真实照片。 */
    private val _placeholderPhoto = MutableStateFlow<File?>(null)
    val placeholderPhoto: StateFlow<File?> = _placeholderPhoto.asStateFlow()
    fun setPlaceholder(file: File) { _placeholderPhoto.value = file }
    fun clearPlaceholder() { _placeholderPhoto.value = null }

    /** 拍照处理中：addPolaroidFrame 在 IO 协程里跑期间为 true。UI 端可监听此信号决定何时 clearPlaceholder。 */
    private val _photoProcessing = MutableStateFlow(false)
    val photoProcessing: StateFlow<Boolean> = _photoProcessing.asStateFlow()

    /** 莫奈取色结果：在 addPolaroidFrame 完成后（同一 IO 协程）直接提取，避免 overlay 读取时机问题。 */
    private val _extractedColor = MutableStateFlow(0xFFEDE0C8.toInt())  // RetroPaper 作为初始值
    val extractedColor: StateFlow<Int> = _extractedColor.asStateFlow()

    /** 将照片同步插入相册列表首位（立即可见，不等 refreshPhotos 异步完成）。 */
    fun prependPhoto(file: File) {
        val list = _photos.value.toMutableList()
        if (file !in list) list.add(0, file)
        _photos.value = list
        _photoVersion.value = System.currentTimeMillis()
    }

    // —— 手动相机参数：对焦距离 / 快门速度（0 = 自动；>0 为手动）。交互与滤镜滑块一致（横滑调节）——
    // 经 Camera2CameraControl 真正作用于相机：对焦 = 关闭 AF 后设 LENS_FOCUS_DISTANCE；
    // 快门 = 关闭 AE 后设 SENSOR_EXPOSURE_TIME，并用目标等效感光度维持近似曝光。
    var focusFraction by mutableStateOf(0f)
    var shutterFraction by mutableStateOf(0f)
    var minFocusDistance by mutableStateOf(0f)   // 镜头最近对焦距离（屈光度），由相机特性读取
    private val TARGET_EI = 1_000_000_000L       // 手动快门时维持近似曝光的等效感光度目标
    private val REF_MIN_FOCUS_DIOPTER = 10f // 设备不报最小对焦距离(=0/null)时退化的参考最大屈光度(≈0.1m 最近对焦)，保证滑块始终显示距离
    private var targetFps = 30

    private var lastLocation: Location? = null

    // —— 滤镜参数（每个滤镜独立存储，单一数据源）——
    private val SAT_SCALE = 1f      // 正方形 x(0~1) → 饱和度(-1~1)
    private val BRIGHT_SCALE = 0.5f // 正方形 y(0~1) → 明暗(±0.5)
    private val EXPOSURE_SCALE = 1f // 亮度强度偏移(-0.5~0.5) → 曝光

    // 各滤镜的默认参数：亮度默认强度 0.5（正常），其余默认 1。
    private fun defaultParams(key: String): FilterParams =
        if (key == BRIGHTNESS_KEY) FilterParams(intensity = BRIGHTNESS_DEFAULT) else FilterParams()

    var filters by mutableStateOf(FILTER_KEYS.associateWith { defaultParams(it) })
    var selectedFilter by mutableStateOf<String?>(null)

    /** 调色盘当前状态（无滤镜 / 暖色 / 冷色）。 */
    var tintState by mutableStateOf(TintState.NONE)
        private set

    // —— DataStore 持久化：每个滤镜 4 个键（enabled/intensity/saturation/brightness），加一个 selected 字符串。
    // 命名规则：`f_<key>_e` / `f_<key>_i` / `f_<key>_s` / `f_<key>_b`，避免偏好键名冲突、未来扩展预留 `f_` 前缀。
    private fun fEnabled(k: String) = booleanPreferencesKey("f_${k}_e")
    private fun fIntensity(k: String) = floatPreferencesKey("f_${k}_i")
    private fun fSaturation(k: String) = floatPreferencesKey("f_${k}_s")
    private fun fBrightness(k: String) = floatPreferencesKey("f_${k}_b")
    private val SELECTED_KEY = stringPreferencesKey("selected_filter")
    private val TINT_KEY = stringPreferencesKey("tint_state")

    /**
     * 预览与保存共用的“有效参数”：把各滤镜的开关与强度合并成着色器 uniform。
     * 互斥规则：暖色与冷色互斥（由 setEnabled 保证），故色温最多一个符号生效。
     * 亮度：强度 0.5 = 正常，>0.5 变亮、<0.5 变暗，映射到曝光 -1~1。
     */
    val effective: EffectiveFilter
        get() {
            val f = filters
            val bw = f["黑白"]!!; val vig = f["暗角"]!!
            val bri = f[BRIGHTNESS_KEY]!!
            val grayscale = if (bw.enabled) bw.intensity else 0f
            val vignette = if (vig.enabled) vig.intensity else 0f
            // 亮度：(intensity-0.5)*2 → -1~1；未启用则 0（正常）
            val exposureMode = if (bri.enabled) (bri.intensity - 0.5f) * 2f * EXPOSURE_SCALE else 0f
            // 色温/调色盘：由单一 tintState 驱动（无滤镜=0，暖色=正，冷色=负），替代原独立的暖/冷开关
            val (warmth, tone) = when (tintState) {
                TintState.WARM -> f["暖色"]!!.intensity to f["暖色"]!!
                TintState.COOL -> -f["冷色"]!!.intensity to f["冷色"]!!
                TintState.NONE -> 0f to null
            }
            val saturation = tone?.let { (it.saturation - 0.5f) * 2f * SAT_SCALE } ?: 0f
            val bright = tone?.let { (0.5f - it.brightness) * 2f * BRIGHT_SCALE } ?: 0f
            val exposure = (exposureMode + bright).coerceIn(-1f, 1f)
            return EffectiveFilter(grayscale, vignette, exposure, warmth, saturation)
        }

    /** 切换某滤镜的启用开关；暖/冷 互斥。 */
    fun setEnabled(key: String, enabled: Boolean) {
        filters = filters.toMutableMap().apply {
            this[key] = this[key]!!.copy(enabled = enabled)
            when (key) {
                "暖色" -> if (enabled) this["冷色"] = this["冷色"]!!.copy(enabled = false)
                "冷色" -> if (enabled) this["暖色"] = this["暖色"]!!.copy(enabled = false)
            }
        }
    }

    /** 修改当前选中滤镜的参数（点按钮主体选中后才能调）。 */
    fun updateSelectedParams(block: FilterParams.() -> FilterParams) {
        val key = selectedFilter ?: return
        filters = filters.toMutableMap().apply { this[key] = this[key]!!.block() }
    }

    /** 调整当前选中滤镜的参数并自动启用它（供调色盘拖拽 / 滑块调节时调用，使调整即时可见）。 */
    fun adjustSelected(block: FilterParams.() -> FilterParams) {
        val key = selectedFilter ?: return
        setEnabled(key, true)
        updateSelectedParams(block)
    }

    /** 修改指定滤镜的参数（用于滤镜按钮上直接滑动调整强度等，无需先选中）。 */
    fun updateFilterParams(key: String, block: FilterParams.() -> FilterParams) {
        filters = filters.toMutableMap().apply { this[key] = this[key]!!.block() }
    }

    /**
     * 还原：只把【当前选中的滤镜】参数重置为默认（intensity/saturation/brightness 等），
     * 但【保留其启用开关 enabled 不变】（含暖/冷互斥状态），其余滤镜完全不动、也不清空选中。
     * 即"还原当前正在调的这个滤镜"，而非一键全关。
     */
    fun resetSelected() {
        val key = selectedFilter ?: return
        filters = filters.toMutableMap().apply {
            this[key] = defaultParams(key).copy(enabled = this[key]!!.enabled)
        }
    }

    /** 循环切换调色盘三态：dir>0 正向（无→暖→冷→无），dir<0 反向。 */
    fun cycleTint(dir: Int) {
        tintState = when (tintState) {
            TintState.NONE -> if (dir >= 0) TintState.WARM else TintState.COOL
            TintState.WARM -> if (dir >= 0) TintState.COOL else TintState.NONE
            TintState.COOL -> if (dir >= 0) TintState.NONE else TintState.WARM
        }
        filters = filters.toMutableMap().apply {
            this["暖色"] = this["暖色"]!!.copy(enabled = tintState == TintState.WARM)
            this["冷色"] = this["冷色"]!!.copy(enabled = tintState == TintState.COOL)
        }
    }

    /** 调整当前调色盘参数（饱和度/明暗/强度）；无滤镜时拖动自动切到暖色，使调整即时生效。 */
    fun adjustTint(block: FilterParams.() -> FilterParams) {
        val key = when (tintState) {
            TintState.WARM -> "暖色"
            TintState.COOL -> "冷色"
            TintState.NONE -> { tintState = TintState.WARM; "暖色" }
        }
        filters = filters.toMutableMap().apply { this[key] = this[key]!!.block().copy(enabled = true) }
    }

    /** 还原：清空所有滤镜（黑白/暗角/亮度重置为默认并关闭，调色盘回到无滤镜）。 */
    fun resetAll() {
        filters = FILTER_KEYS.associateWith { defaultParams(it).copy(enabled = false) }
        tintState = TintState.NONE
    }

    init {
        // 4:3 强制约束已在类初始化时通过 .setTargetAspectRatio(AspectRatio.RATIO_4_3) 应用到 preview 和 imageCapture use case。
        imageCaptureFlashMode = ImageCapture.FLASH_MODE_AUTO
        refreshPhotos()
        // —— DataStore 加载/保存 —— 启动时一次性读盘恢复（不重置，仅"还原"按钮才重置）；
        // 之后用 snapshotFlow + debounce(500ms) 监听 filters/selectedFilter 变化批量写盘，
        // 避免拖动时每帧 toMutableMap 都触发一次 IO 写。debounce 500ms 内停顿即合并为一次写。
        viewModelScope.launch {
            val initial = getApplication<Application>().filterDataStore.data.first()
            val restored = FILTER_KEYS.associateWith { k ->
                val e = initial[fEnabled(k)]
                val i = initial[fIntensity(k)]
                val s = initial[fSaturation(k)]
                val b = initial[fBrightness(k)]
                if (e == null && i == null && s == null && b == null) {
                    defaultParams(k)  // 该滤镜从未保存过 → 用默认
                } else {
                    defaultParams(k).copy(
                        enabled = e ?: false,
                        intensity = i ?: defaultParams(k).intensity,
                        saturation = s ?: defaultParams(k).saturation,
                        brightness = b ?: defaultParams(k).brightness,
                    )
                }
            }
            filters = restored
            initial[SELECTED_KEY]?.let { if (it in FILTER_KEYS) selectedFilter = it }
            // 调色盘状态：优先读 tint_state；旧版本无此键则按暖/冷启用开关推断（兼容历史数据）
            tintState = initial[TINT_KEY]?.let { runCatching { TintState.valueOf(it) }.getOrNull() } ?: run {
                when {
                    restored["暖色"]!!.enabled -> TintState.WARM
                    restored["冷色"]!!.enabled -> TintState.COOL
                    else -> TintState.NONE
                }
            }
            // 同步暖/冷启用开关与 tintState 一致（避免脏数据）
            filters = filters.toMutableMap().apply {
                this["暖色"] = this["暖色"]!!.copy(enabled = tintState == TintState.WARM)
                this["冷色"] = this["冷色"]!!.copy(enabled = tintState == TintState.COOL)
            }
        }
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            // snapshotFlow 跟踪 State 引用变化；filters/selectedFilter 每次写都换新引用，故会触发。
            // debounce 合并高频拖动写；distinctUntilChanged 避免连续相同值（如重置后再写一次）触发。
            snapshotFlow { filters to selectedFilter }
                .debounce(500)
                .distinctUntilChanged()
                .collect { (f, sel) ->
                    runCatching {
                        getApplication<Application>().filterDataStore.edit { p ->
                            // 清空旧键（应对移除的滤镜、未来扩展）
                            p.asMap().keys.filter { it.name.startsWith("f_") || it.name == SELECTED_KEY.name }
                                .forEach { p.remove(it as androidx.datastore.preferences.core.Preferences.Key<*>) }
                            // 写入所有滤镜
                            f.forEach { (k, v) ->
                                p[fEnabled(k)] = v.enabled
                                p[fIntensity(k)] = v.intensity
                                p[fSaturation(k)] = v.saturation
                                p[fBrightness(k)] = v.brightness
                            }
                            // 写入选中
                            if (sel != null) p[SELECTED_KEY] = sel else p.remove(SELECTED_KEY)
                            // 写入调色盘状态
                            p[TINT_KEY] = tintState.name
                        }
                    }
                }
        }
    }

    fun setLastLocation(loc: Location?) { lastLocation = loc }

    fun takePhoto() {
        val file = PhotoStorage.generateFile(getApplication())
        val eff = effective
        _hasTakenPhoto.value = true   // 第一次按下快门后，空相册引导光晕不再显示
        // 在 CameraX 抓取前立即闪白（用 tryEmit 不挂起）：给用户最快反馈
        _shutterFlash.tryEmit(Unit)
        // 立即发射 captureStarted：让相机页同步启动"取景框→灵动岛"morph（与按下快门对齐，
        // 不受不同机型拍照延迟影响）。照片内容就绪仍由 onImageSaved→photoCaptured→photoVersion 控制。
        _captureStarted.tryEmit(file)
        imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompatExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val loc = lastLocation
                    lastLocation = null
                    // 关键：不在这里 setPlaceholder，也不立即更新 _photos。
                    // 用户期望"拍完照之后应该是没有任何动作"——相册在 showDetail=true 之前完全不变。
                    // _photos 更新延后到 onAddToAlbum（showDetail=true 后）通过 refreshPhotosSync 触发。
                    // 也不立即更新 _photoVersion(T1)——T1 的 photoVersion 变化会让 LaunchedEffect 的 while
                    // 提前退出（不等 addPolaroidFrame 完成就 showPhoto=true），可能导致读到原图（无白框）。
                    _photoProcessing.value = true
                    // 立即发射：UI 端进入过渡动画（或无动画处理流程）
                    viewModelScope.launch { _photoCaptured.emit(file) }
                    viewModelScope.launch {
                        // 直接做 addPolaroidFrame，_photos 由 onAddToAlbum.refreshPhotosSync 触发更新
                        withContext(Dispatchers.IO) {
                            PhotoStorage.addPolaroidFrame(file = file, location = loc, eff = eff)
                        }
                        // T2: addPolaroidFrame 完成 → photoVersion 变化 → LaunchedEffect 的 while 退出 → showPhoto=true
                        //    → AsyncImage 读到带白框版本。必须早于取色，取色不计入照片出现间隔（避免不同设备时长差异）。
                        _photoVersion.value = System.currentTimeMillis()
                        _photoProcessing.value = false
                        // 取色在照片出现之后再后台进行（仅用于详情页背景色），不阻塞过渡动画
                        _extractedColor.value = PhotoStorage.extractDominantColor(file, 0xFFEDE0C8.toInt())
                    }
                }
                override fun onError(exc: ImageCaptureException) = Unit
            })
    }

    fun deletePhoto(file: File) {
        // 同时删除缩略图，避免残留的 _thumb 文件占用空间
        PhotoStorage.thumbnailFileFor(file).delete()
        if (file.delete()) refreshPhotos()
    }
    fun toggleFlash() {
        val next = when (_flashMode.value) { FlashMode.AUTO -> FlashMode.ON; FlashMode.ON -> FlashMode.OFF; FlashMode.OFF -> FlashMode.AUTO }
        _flashMode.value = next
        imageCaptureFlashMode = when (next) { FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO; FlashMode.ON -> ImageCapture.FLASH_MODE_ON; FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF }
    }
    fun switchCamera() {
        _isBackCamera.value = !_isBackCamera.value
        cameraSelector = if (_isBackCamera.value) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        doBind()
    }
    fun setZoom(level: Float) { _zoomLevel.value = level; boundCamera?.cameraControl?.setZoomRatio(level) }
    /** 从相册详情返回后重新应用焦段（controller 可能被 lifecycle 重置） */
    fun restoreZoom() { boundCamera?.cameraControl?.setZoomRatio(_zoomLevel.value) }

    // —— 手动相机参数控制 ——

    /** 预览出流（STREAMING）后由 ViewfinderFrame 回调：重设目标帧率、还原焦段并把当前对焦/快门合并应用到相机。
     *  焦段在相机重新绑定（切前台/从设置或相册返回等导致预览中途重连）后会被重置为 1x，故每次重连都按 _zoomLevel 重新套用。 */
    fun onPreviewStreaming() {
        targetFps = 30
        pushCameraOptions()  // 先设 FPS/AF/AE（内部 clearCaptureRequestOptions 可能清掉 zoom）
        boundCamera?.cameraControl?.setZoomRatio(_zoomLevel.value)   // 之后还原焦段——确保 zoom 不被 clearCaptureRequestOptions 清掉
    }

    fun updateFocusFraction(f: Float) { focusFraction = f.coerceIn(0f, 1f); pushCameraOptions() }
    fun updateShutterFraction(s: Float) { shutterFraction = s.coerceIn(0f, 1f); pushCameraOptions() }

    /** 把 FPS 限制 + 对焦距离 + 快门速度合并成一份 CaptureRequestOptions 应用到相机。
     * 所有相机参数设置都走这里，避免多次 setCaptureRequestOptions 互相覆盖（旧 API 为整体替换）。 */
    fun pushCameraOptions() {
        try {
            val cameraControl = boundCamera?.cameraControl ?: return
            val cameraInfo = boundCamera?.cameraInfo ?: return
            val c2 = Camera2CameraControl.from(cameraControl)
            val info = Camera2CameraInfo.from(cameraInfo)
            val minFocus = info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            minFocusDistance = minFocus
            val expRange = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

            val b = CaptureRequestOptions.Builder()
            // 持续限制预览帧率到 30fps（性能）
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(targetFps, targetFps),
            )
            // 对焦距离：0 = 连续自动对焦；>0 = 关闭 AF 后按屈光度设最近对焦距离
            if (focusFraction <= 0.001f) {
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            } else {
                // 设备支持手动对焦时用其真实最小对焦距离；否则退化为参考值，保证滑块始终对应一个连续距离
                val effMin = if (minFocus > 0f) minFocus else REF_MIN_FOCUS_DIOPTER
                val diopters = (focusFraction * effMin).coerceAtLeast(0f)
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, diopters)
            }
            // 快门速度：0 = 自动曝光；>0 = 关闭 AE 后按对数刻度设曝光时间，并用目标等效感光度维持近似亮度
            if (shutterFraction <= 0.001f) {
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            } else {
                val secs = shutterSeconds(shutterFraction)
                val minE = (expRange?.lower ?: 16_666L).toDouble()
                val maxE = (expRange?.upper ?: 500_000_000L).coerceAtMost(500_000_000L).toDouble()
                val exp = secs * 1_000_000_000.0
                val expClamped = exp.coerceIn(minE, maxE)
                val iso = (TARGET_EI / expClamped).roundToLong().coerceIn(50, 3200)
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                b.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, expClamped.toLong())
                b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso.toInt())
            }
            c2.clearCaptureRequestOptions()
            c2.addCaptureRequestOptions(b.build())
        } catch (_: Exception) {
            // 设备不支持手动对焦/曝光时静默忽略，不影响拍照
        }
    }

    /** 快门 fraction(0~1) → 曝光秒数（固定 1/4000s..3s 对数刻度：0 = 最快 1/4000s，1 = 最慢 3s）。 */
    private fun shutterSeconds(fraction: Float): Double {
        val fastNs = 1_000_000_000.0 / 4000.0   // 1/4000 s
        val slowNs = 3_000_000_000.0            // 3 s
        return fastNs * Math.pow(slowNs / fastNs, fraction.toDouble()) / 1_000_000_000.0
    }

    /** 对焦距离文本：自动 / "x.xx m"（连续距离，从近到远）。设备不报最小对焦距离时退化为参考值，始终显示距离而非"固定"。 */
    fun focusDistanceText(): String {
        if (focusFraction <= 0.001f) return "自动"
        val minF = if (minFocusDistance > 0f) minFocusDistance else REF_MIN_FOCUS_DIOPTER
        val meters = (1f / (focusFraction * minF)).coerceAtLeast(0.05f)
        return if (meters >= 1f) "%.1f m".format(meters) else "%.2f m".format(meters)
    }

    /** 快门速度文本：自动 / "1/xxx" / "x.x s"。 */
    fun shutterSpeedText(): String {
        if (shutterFraction <= 0.001f) return "自动"
        val secs = shutterSeconds(shutterFraction)
        return if (secs >= 1.0) "%.1f s".format(secs) else "1/%d".format((1.0 / secs).roundToInt().coerceAtLeast(2))
    }
    fun refreshPhotos() {
        viewModelScope.launch {
            // 首屏只取最近 ALBUM_FIRST_PAGE 张，先让相册快速出图；其余后台继续加载
            val first = withContext(Dispatchers.IO) {
                PhotoStorage.listPhotos(getApplication(), limit = ALBUM_FIRST_PAGE)
            }
            _photos.value = first
            val all = withContext(Dispatchers.IO) {
                PhotoStorage.listPhotos(getApplication())
            }
            if (all.size > _photos.value.size) _photos.value = all
            // 后台为尚无缩略图的照片补生成（前 ALBUM_FIRST_PAGE 张优先），让历史照片下次进入也能秒开列表
            warmMissingThumbnails(all)
        }
    }

    /** 后台为尚无缩略图的照片补生成（首批优先）。运行在 IO 线程，不阻塞 UI。 */
    private suspend fun warmMissingThumbnails(list: List<File>) {
        withContext(Dispatchers.IO) {
            list.take(ALBUM_FIRST_PAGE).forEach { f ->
                if (!PhotoStorage.thumbnailFileFor(f).exists()) {
                    PhotoStorage.generateThumbnail(f)
                }
            }
        }
    }

    /** 同步刷新相册列表（suspend，调用方可等待完成后再继续）。不更新 photoVersion，避免缩略图重载闪烁。 */
    suspend fun refreshPhotosSync() {
        val list = withContext(Dispatchers.IO) {
            PhotoStorage.listPhotos(getApplication())
        }
        _photos.value = list
    }
}

private class ContextCompatExecutor : Executor { override fun execute(command: Runnable) = Thread(command).start() }
