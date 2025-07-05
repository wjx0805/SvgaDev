package com.andtest.svgadev

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.andtest.svgadev.databinding.ActivityNextBinding

class Next2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding= ActivityNextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btn.text="I'm Next2"
    }
}