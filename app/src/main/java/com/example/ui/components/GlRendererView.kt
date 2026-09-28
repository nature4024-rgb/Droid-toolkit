package com.example.ui.components

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun GlRendererView(
    onGlInfoReady: (
        renderer: String, 
        vendor: String, 
        version: String,
        extensions: String,
        maxTextureSize: Int,
        maxViewportWidth: Int,
        maxViewportHeight: Int,
        maxRenderbufferSize: Int
    ) -> Unit
) {
    AndroidView(
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                        val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown GPU"
                        val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown Vendor"
                        val version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown GL Version"
                        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
                        
                        val maxTex = IntArray(1)
                        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTex, 0)
                        
                        val maxView = IntArray(2)
                        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, maxView, 0)
                        
                        val maxRender = IntArray(1)
                        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, maxRender, 0)
                        
                        onGlInfoReady(
                            renderer, 
                            vendor, 
                            version, 
                            extensions, 
                            maxTex[0], 
                            maxView[0], 
                            maxView[1], 
                            maxRender[0]
                        )
                    }

                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
                    override fun onDrawFrame(gl: GL10?) {}
                })
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
        },
        update = {},
        modifier = Modifier.size(1.dp)
    )
}
