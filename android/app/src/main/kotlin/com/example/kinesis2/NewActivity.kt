package com.example.kinesis2

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.kinesisvideo.client.KinesisVideoClient
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException
import com.amazonaws.kinesisvideo.producer.StreamInfo
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration
import com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils
import com.amazonaws.mobileconnectors.kinesisvideo.util.VideoEncoderUtils
import com.amazonaws.regions.Regions

class NewActivity : AppCompatActivity() {

    var KINESIS_VIDEO_REGION = Regions.AP_SOUTH_1
    private var kinesisVideoClient: KinesisVideoClient? = null
    private val RESOLUTION_320x240 = Size(320, 240)
    private val FRAMERATE_20 = 20
    private val BITRATE_384_KBPS = 384 * 1024
    private val RETENTION_PERIOD_48_HOURS = 2 * 24
    private var cameraMediaSource: AndroidCameraMediaSource? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

        val textureView = findViewById<TextureView>(R.id.stream_texture_view)
        try {
            kinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    this,
                    KINESIS_VIDEO_REGION,
                    AWSMobileClient.getInstance())

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                    try {
                        if (cameraMediaSource != null) cameraMediaSource!!.stop()
                        if (kinesisVideoClient != null) kinesisVideoClient!!.stopAllMediaSources()
                        KinesisVideoAndroidClientFactory.freeKinesisVideoClient()
                    } catch (e: KinesisVideoException) {
                        Log.e("Streaming", "failed to release kinesis video client", e)
                    }
                    return true
                }
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d("Texture", "Surface Texture Available")
                    surface?.setDefaultBufferSize(1280, 720)
                    createClientAndStartStreaming(surface)
                }
            }

        } catch (e: KinesisVideoException) {
            Log.e("Kinesis", "Failed to create Kinesis Video client", e)
        }
    }

    private fun createClientAndStartStreaming(previewTexture: SurfaceTexture?) {
        try {
            cameraMediaSource =  kinesisVideoClient?.createMediaSource("Stream", getCurrentConfiguration()) as AndroidCameraMediaSource
            cameraMediaSource!!.setPreviewSurfaces(Surface(previewTexture))
            resumeStreaming()
        } catch (e: KinesisVideoException) {
            Log.e("Stream", "unable to start streaming")
            throw RuntimeException("unable to start streaming", e)
        }
    }

    private fun getCurrentConfiguration(): AndroidCameraMediaSourceConfiguration? {
        Log.d("Config", CameraUtils.getCameras(kinesisVideoClient)[0].cameraId+" "+VideoEncoderUtils.getSupportedMimeTypes()[0].mimeType+" "+CameraUtils.getSupportedResolutions(applicationContext, CameraUtils.getCameras(kinesisVideoClient)[0].cameraId)[0].width )
        return AndroidCameraMediaSourceConfiguration(
                AndroidCameraMediaSourceConfiguration.builder()
                        .withCameraId(CameraUtils.getCameras(kinesisVideoClient)[0].cameraId)
                        .withEncodingMimeType(VideoEncoderUtils.getSupportedMimeTypes()[0].mimeType)
                        .withHorizontalResolution(320)
                        .withVerticalResolution(240)
                        .withCameraFacing(CameraUtils.getCameras(kinesisVideoClient)[0].cameraFacing)
                        .withIsEncoderHardwareAccelerated(CameraUtils.getCameras(kinesisVideoClient)[0].isEndcoderHardwareAccelerated)
                        .withFrameRate(FRAMERATE_20)
                        .withRetentionPeriodInHours(RETENTION_PERIOD_48_HOURS)
                        .withEncodingBitRate(BITRATE_384_KBPS)
                        .withCameraOrientation(CameraUtils.getCameras(kinesisVideoClient)[0].cameraOrientation)
                        .withNalAdaptationFlags(StreamInfo.NalAdaptationFlags.NAL_ADAPTATION_ANNEXB_CPD_AND_FRAME_NALS)
                        .withIsAbsoluteTimecode(false))
    }

    private fun resumeStreaming() {
        try {
            if (cameraMediaSource == null) {
                return
            }
            cameraMediaSource!!.start()
            findViewById<LinearLayout>(R.id.root_layout).post {
                Toast.makeText(this, "resumed streaming", Toast.LENGTH_SHORT).show()
            }
        } catch (e: KinesisVideoException) {
            Log.e("Streaming", "unable to resume streaming", e)
            findViewById<LinearLayout>(R.id.root_layout).post {
                Toast.makeText(this, "failed to resume streaming", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pauseStreaming() {
        try {
            if (cameraMediaSource == null) {
                return
            }
            cameraMediaSource!!.stop()
            Toast.makeText(applicationContext, "stopped streaming", Toast.LENGTH_SHORT).show()
        } catch (e: KinesisVideoException) {
            Log.e("Stream", "unable to pause streaming", e)
            Toast.makeText(applicationContext, "failed to pause streaming", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        resumeStreaming()
    }

    override fun onPause() {
        super.onPause()
        pauseStreaming()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        pauseStreaming()
    }
}//CameraUtils.getCameras(kinesisVideoClient)[-1].cameraId)[-1].width