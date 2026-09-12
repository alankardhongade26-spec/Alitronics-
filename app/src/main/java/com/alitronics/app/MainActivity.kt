package com.alitronics.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val startButton = findViewById<Button>(R.id.startButton)
        val videoButton = findViewById<Button>(R.id.videoButton)

        startButton.setOnClickListener {
            val name = nameInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter your name",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Welcome to Alitronics, $name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        videoButton.setOnClickListener {
    val intent = android.content.Intent(this, VideoCallActivity::class.java)
    startActivity(intent)
        }
    }
}
