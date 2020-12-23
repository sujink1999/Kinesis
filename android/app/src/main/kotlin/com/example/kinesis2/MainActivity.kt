package com.example.kinesis2

import android.Manifest
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import java.security.Permission
import java.util.concurrent.CountDownLatch

class MainActivity: FlutterActivity() {

    private val aCHANNEL = "samples.flutter.dev/battery"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngine
                .platformViewsController
                .registry
                .registerViewFactory("StreamView", StreamViewFactory(flutterEngine.dartExecutor.binaryMessenger))
//        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, aCHANNEL).setMethodCallHandler {
//            // Note: this method is invoked on the main thread.
//            call, result ->
//            if (call.method == "getBatteryLevel") {
//                val batteryLevel = getBatteryLevel()
//                if (batteryLevel != -1) {
//                    result.success(batteryLevel)
//                } else {
//                    result.error("UNAVAILABLE", "Battery level not available.", null)
//                }
//
//
//                Toast.makeText(applicationContext, "Permission is " +checkPersmission(), Toast.LENGTH_SHORT).show()
//                if (checkPersmission()){
//                    initializeAWSMobileClient()
//
//                }else{
//                    requestPermission()
//                }
//
//            } else {
//                result.notImplemented()
//            }
//        }
    }

    private fun checkPersmission(): Boolean {
        return (ContextCompat.checkSelfPermission(applicationContext, CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), 9393)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            9393 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(applicationContext, "Permission is " +checkPersmission(), Toast.LENGTH_SHORT).show()
                    initializeAWSMobileClient()
                } else {
                    Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }


    private fun initializeAWSMobileClient(){
        val latch = CountDownLatch(1)
        AWSMobileClient.getInstance().initialize(applicationContext, object : Callback<UserStateDetails> {
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
        Log.d("Auth check", auth.isSignedIn.toString());

        if (auth.isSignedIn) {
            moveToNextScreen()
        } else {
            auth.signIn("+917299603606", "australia", v, object : Callback<SignInResult> {
                override fun onResult(result: SignInResult?) {
                    Log.d("Sign in", "Success")
                    moveToNextScreen()
                }
                override fun onError(e: java.lang.Exception?) {
                    Log.d("Sign in", e?.printStackTrace().toString())
                }
            });
        }
    }

    private fun moveToNextScreen(){
        val intent = Intent(applicationContext, NewActivity::class.java);
        startActivity(intent);
    }


    private fun getBatteryLevel(): Int {



        val batteryLevel: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryLevel = intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        }

        return batteryLevel
    }
}
