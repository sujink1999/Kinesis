package com.example.kinesis2

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import com.amazonaws.kinesisvideo.client.KinesisVideoClient
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException
import com.amazonaws.kinesisvideo.producer.StreamInfo
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignInUIOptions
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignUpResult
import com.amazonaws.mobile.client.results.Tokens
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration
import com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils
import com.amazonaws.mobileconnectors.kinesisvideo.util.VideoEncoderUtils
import com.amazonaws.regions.Regions
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import org.apache.http.client.CredentialsProvider
import java.util.concurrent.CountDownLatch


class StreamPlatformView internal constructor(private val context: Context?, binaryMessenger: BinaryMessenger, id: Int, args: Any?) : PlatformView, MethodChannel.MethodCallHandler {
    private val streamView: TextureView
    private val methodChannel: MethodChannel
    private var textureAvailable = false
    private var surfaceTexture : SurfaceTexture? = SurfaceTexture(0)


    // KINESIS CONFIGURATION
    var KINESIS_VIDEO_REGION = Regions.AP_SOUTH_1
    private var kinesisVideoClient: KinesisVideoClient? = null
    private val RESOLUTION_320x240 = Size(320, 240)
    private val FRAMERATE_20 = 20
    private val BITRATE_384_KBPS = 384 * 1024
    private val RETENTION_PERIOD_48_HOURS = 2 * 24
    private var cameraMediaSource: AndroidCameraMediaSource? = null

    init {
        streamView = TextureView(context)
        methodChannel = MethodChannel(binaryMessenger, "StreamView/$id")
        methodChannel.setMethodCallHandler(this)

        streamView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                try {
                    if (cameraMediaSource != null) cameraMediaSource!!.stop()
                    if (kinesisVideoClient != null) kinesisVideoClient!!.stopAllMediaSources()
                    KinesisVideoAndroidClientFactory.freeKinesisVideoClient()
                    Log.d("Texture", "Texture destroyed")
                } catch (e: KinesisVideoException) {
                    Log.e("Streaming", "failed to release kinesis video client", e)
                }
                return true
            }
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                Log.d("Texture", "Surface Texture Available")
                textureAvailable = true
                surfaceTexture = surface
            }
        }
    }

    private fun startStreaming(methodCall: MethodCall,
                               result: MethodChannel.Result) {
        result.success("Streaming Started")
    }
    override fun getView(): View {
        return streamView
    }

    override fun dispose() {
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        when (call.method) {
            "startStreaming" -> {
                initializeAWSMobileClient()
            }
            "stopStreaming" -> {
                pauseStreaming()
            }
            else -> {
                result.notImplemented()
            }
        }

    }


    private fun initializeAWSMobileClient(){
        val latch = CountDownLatch(1)
        AWSMobileClient.getInstance().initialize(context, object : Callback<UserStateDetails> {
            override fun onResult(result: UserStateDetails) {
                Log.d("Result", "onResult: user state: " + result.userState)
                latch.countDown()
                signInAndMoveToNextScreen()
            }
            override fun onError(e: Exception) {
                Log.e("Error", "onError: Initialization error of the mobile client", e)
                latch.countDown()
            }
        })
        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun signInAndMoveToNextScreen(){
        val auth = AWSMobileClient.getInstance()
        val v = HashMap<String, String>()
        val user = HashMap<String, String>()
        user["email"] = "lksujins@gmail.com"
        Log.d("Auth check", auth.isSignedIn.toString());

        if (auth.isSignedIn) {
            auth.getTokens(object : Callback<Tokens>{
                override fun onResult(result: Tokens?) {
                    setTextureView()
                }

                override fun onError(e: java.lang.Exception?) {
                }
            })
        } else {
            auth.signIn("+917299603606", "australia", v, object : Callback<SignInResult> {
                override fun onResult(result: SignInResult?) {
                    Log.d("Sign in", "Success")
                    setTextureView()
                }
                override fun onError(e: java.lang.Exception?) {
                    Log.d("Sign in", e?.printStackTrace().toString())
                }
            });

        }
    }

    private fun setTextureView(){
        try {
            kinesisVideoClient = context?.let {
                KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                        it,
                        KINESIS_VIDEO_REGION,
                        AWSMobileClient.getInstance())
            }
            if(textureAvailable){
                surfaceTexture?.setDefaultBufferSize(1280, 720)
                createClientAndStartStreaming(surfaceTexture)
            }else{
                Log.d("Texture","Texture Not Available")
            }

        } catch (e: KinesisVideoException) {
            Log.e("Kinesis", "Failed to create Kinesis Video client", e)
        }
    }

    private fun createClientAndStartStreaming(previewTexture: SurfaceTexture?) {
        try {
            runOnUiThread {
                    cameraMediaSource =  kinesisVideoClient?.createMediaSource("Stream", getCurrentConfiguration()) as AndroidCameraMediaSource
                    cameraMediaSource!!.setPreviewSurfaces(Surface(previewTexture))
            }
            resumeStreaming()

        } catch (e: KinesisVideoException) {
            Log.e("Stream", "unable to start streaming")
            throw RuntimeException("unable to start streaming", e)
        }
    }

    private fun getCurrentConfiguration(): AndroidCameraMediaSourceConfiguration? {
        Log.d("Config", CameraUtils.getCameras(kinesisVideoClient)[0].cameraId+" "+ VideoEncoderUtils.getSupportedMimeTypes()[0].mimeType+" "+ CameraUtils.getSupportedResolutions(context, CameraUtils.getCameras(kinesisVideoClient)[0].cameraId)[0].width )
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
                Log.d("Texture","CameraSource is null")
                return
            }
            cameraMediaSource!!.start()
            Log.d("Texture","CameraSource started")
//            findViewById<LinearLayout>(R.id.root_layout).post {
//                Toast.makeText(this, "resumed streaming", Toast.LENGTH_SHORT).show()
//            }
        } catch (e: KinesisVideoException) {
            Log.e("Streaming", "unable to resume streaming", e)
//            findViewById<LinearLayout>(R.id.root_layout).post {
//                //Toast.makeText(this, "failed to resume streaming", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun pauseStreaming() {
        try {
            if (cameraMediaSource == null) {
                return
            }
            cameraMediaSource!!.stop()
            //Toast.makeText(context, "stopped streaming", Toast.LENGTH_SHORT).show()
        } catch (e: KinesisVideoException) {
            Log.e("Stream", "unable to pause streaming", e)
            //Toast.makeText(context, "failed to pause streaming", Toast.LENGTH_SHORT).show()
        }
    }
}