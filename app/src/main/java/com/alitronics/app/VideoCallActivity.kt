package com.alitronics.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class VideoCallActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: androidx.camera.core.Camera? = null

    private var micEnabled = true
    private var cameraEnabled = true

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        previewView = findViewById(R.id.cameraPreview)

        val micButton = findViewById<Button>(R.id.micButton)
        val cameraButton = findViewById<Button>(R.id.cameraButton)
        val endCallButton = findViewById<Button>(R.id.endCallButton)

        micButton.setOnClickListener {
            micEnabled = !micEnabled

            if (micEnabled) {
                micButton.text = "🎙 Mic ON"
                Toast.makeText(this, "Microphone ON", Toast.LENGTH_SHORT).show()
            } else {
                micButton.text = "🔇 Mic OFF"
                Toast.makeText(this, "Microphone OFF", Toast.LENGTH_SHORT).show()
            }
        }

        cameraButton.setOnClickListener {
            cameraEnabled = !cameraEnabled

            if (cameraEnabled) {
                cameraButton.text = "📷 Camera ON"
                previewView.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Camera ON", Toast.LENGTH_SHORT).show()
            } else {
                cameraButton.text = "📷 Camera OFF"
                previewView.visibility = android.view.View.INVISIBLE
                Toast.makeText(this, "Camera OFF", Toast.LENGTH_SHORT).show()
            }
        }

        endCallButton.setOnClickListener {
            cameraProvider?.unbindAll()
            finish()
        }

        checkPermissionsAndStartCamera()
    }

    private fun checkPermissionsAndStartCamera() {

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (permissions.all {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {

            startCamera()

        } else {

            ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == PERMISSION_REQUEST_CODE) {

            if (grantResults.isNotEmpty() &&
                grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera and microphone permissions are required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )

            val cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA

            try {

                cameraProvider?.unbindAll()

                camera = cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Unable to start camera",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        super.onDestroy()
    }
}                
