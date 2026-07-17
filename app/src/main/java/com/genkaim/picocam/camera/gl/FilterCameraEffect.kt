package com.genkaim.picocam.camera.gl

import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceProcessor
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/** 可实例化的 CameraEffect 子类。 */
class FilterCameraEffect(
    targets: Int,
    executor: Executor,
    processor: SurfaceProcessor,
) : CameraEffect(targets, executor, processor, Consumer { }) // 错误忽略
