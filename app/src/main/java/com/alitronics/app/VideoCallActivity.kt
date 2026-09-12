package com.alitronics.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VideoCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        val micButton = findViewById<Button>(R.id.micButton)
        val cameraButton = findViewById<Button>(R.id.cameraButton)
        val endCallButton = findViewById<Button>(R.id.endCallButton)

        micButton.setOnClickListener {
            Toast.makeText(this, "Microphone button pressed", Toast.LENGTH_SHORT).show()
        }

        cameraButton.setOnClickListener {
            Toast.makeText(this, "Camera button pressed", Toast.LENGTH_SHORT).show()
        }

        endCallButton.setOnClickListener {
            finish()
        }
    }
}
