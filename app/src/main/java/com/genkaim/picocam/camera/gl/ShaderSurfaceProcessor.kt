package com.genkaim.picocam.camera.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLES11Ext
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.atomic.AtomicReference

/**
 * CameraX SurfaceProcessor — 在相机和输出 Surface 之间插入 OpenGL 滤镜渲染。
 */
class ShaderSurfaceProcessor(
    private val context: android.content.Context,
) : SurfaceProcessor {

    data class Params(
        val grayscale: Float = 0f,
        val vignette: Float = 0f,
        val exposure: Float = 0f,
        val warmth: Float = 0f,
        val saturation: Float = 0f,
    )
    private val paramsRef = AtomicReference(Params())
    fun updateParams(p: Params) { paramsRef.set(p) }
    fun getParams(): Params = paramsRef.get()

    private var eglCore: EglCore? = null
    private var filterShader: FilterShader? = null
    private var inputTexId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var eglOutputSurface: android.opengl.EGLSurface? = null
    private val texMatrix = FloatArray(16)
    private var frameWidth = 0
    private var frameHeight = 0

    override fun onInputSurface(request: SurfaceRequest) {
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        inputTexId = texIds[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTexId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val res = request.resolution
        surfaceTexture = SurfaceTexture(inputTexId).apply {
            setDefaultBufferSize(res.width, res.height)
            setOnFrameAvailableListener { renderFrame() }
        }
        frameWidth = res.width
        frameHeight = res.height

        val inputSurface = Surface(surfaceTexture)
        request.provideSurface(inputSurface, { /* executor */ }) { inputSurface.release() }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        if (eglCore == null) {
            eglCore = EglCore()
            filterShader = FilterShader().also { it.compile(context) }
        }
        val st = surfaceTexture ?: return
        surfaceOutput.updateTransformMatrix(texMatrix, FloatArray(16) /* identity */)

        // 获取输出 Surface 并创建 EGL 窗口 surface
        val outputSurface = surfaceOutput.getSurface({ }, { })
        eglOutputSurface = eglCore?.createWindowSurface(outputSurface)
        eglCore?.makeCurrent(eglOutputSurface!!)

        val outSize = surfaceOutput.size
        frameWidth = outSize.width
        frameHeight = outSize.height
    }

    private fun renderFrame() {
        val st = surfaceTexture ?: return
        val egl = eglCore ?: return
        val outSurf = eglOutputSurface ?: return
        val shader = filterShader ?: return
        val params = getParams()

        try {
            st.updateTexImage()
            st.getTransformMatrix(texMatrix)

            egl.makeCurrent(outSurf)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            shader.draw(
                texId = inputTexId,
                texMatrix = texMatrix,
                width = frameWidth, height = frameHeight,
                grayscale = params.grayscale,
                vignette = params.vignette,
                exposure = params.exposure,
                warmth = params.warmth,
                saturation = params.saturation,
                time = System.nanoTime() / 1e9f,
            )
            if (!egl.swapBuffers(outSurf)) {
                // 可能 surface 已失效，忽略
            }
        } catch (e: Exception) {
            android.util.Log.e("ShaderSurfaceProc", "renderFrame error", e)
        }
    }
}
