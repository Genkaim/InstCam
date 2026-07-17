package com.genkaim.picocam.camera.gl

import android.opengl.GLES20
import android.opengl.GLES11Ext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** GLSL 着色器编译 + 绘制工具。 */
class FilterShader {

    private var program = 0

    private val vertexData = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    private val texCoordData = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private var aPosition = 0
    private var aTexCoord = 0
    private var uTexMatrix = 0
    private var uTexture = 0
    var uGrayscale = 0
    var uVignette = 0
    var uExposure = 0
    var uWarmth = 0
    var uSaturation = 0
    var uTime = 0

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(vertexData).also { it.position(0) }
        texCoordBuffer = ByteBuffer.allocateDirect(texCoordData.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(texCoordData).also { it.position(0) }
    }

    fun compile(context: android.content.Context) {
        val vertexSrc = loadRaw(context, com.genkaim.picocam.R.raw.filter_vertex)
        val fragmentSrc = loadRaw(context, com.genkaim.picocam.R.raw.filter_fragment)
        program = createProgram(vertexSrc, fragmentSrc)

        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        uGrayscale = GLES20.glGetUniformLocation(program, "uGrayscale")
        uVignette = GLES20.glGetUniformLocation(program, "uVignette")
        uExposure = GLES20.glGetUniformLocation(program, "uExposure")
        uWarmth = GLES20.glGetUniformLocation(program, "uWarmth")
        uSaturation = GLES20.glGetUniformLocation(program, "uSaturation")
        uTime = GLES20.glGetUniformLocation(program, "uTime")
    }

    fun draw(
        texId: Int, texMatrix: FloatArray,
        width: Int, height: Int,
        grayscale: Float, vignette: Float, exposure: Float,
        warmth: Float, saturation: Float, time: Float,
    ) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        GLES20.glUniform1i(uTexture, 0)
        GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)

        GLES20.glUniform1f(uGrayscale, grayscale)
        GLES20.glUniform1f(uVignette, vignette)
        GLES20.glUniform1f(uExposure, exposure)
        GLES20.glUniform1f(uWarmth, warmth)
        GLES20.glUniform1f(uSaturation, saturation)
        GLES20.glUniform1f(uTime, time)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPosition)

        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoord)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
    }

    companion object {
        fun loadRaw(ctx: android.content.Context, resId: Int): String {
            return ctx.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        }

        fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compile error: $log")
            }
            return shader
        }

        fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
            val prog = GLES20.glCreateProgram()
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
            val status = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(prog)
                GLES20.glDeleteProgram(prog)
                throw RuntimeException("Program link error: $log")
            }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
            return prog
        }
    }
}
