package com.andtest.svgadev

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.andtest.svgadev.databinding.ActivityNextBinding

class NextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding= ActivityNextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btn.setOnClickListener {
            startActivity(Intent(this, Next2Activity::class.java))
            finish()
        }
    }
}