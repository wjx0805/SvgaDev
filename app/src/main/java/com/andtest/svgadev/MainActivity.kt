package com.andtest.svgadev

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.andtest.svgadev.databinding.ActivityMainBinding
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import com.opensource.svgaplayer.SvgaCacheManager.SvgaLoadListener

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        SVGAParser.shareParser().init(this)
        binding.recycleView.layoutManager = LinearLayoutManager(this)
        val adapter =SvgaAdapter()
        binding.recycleView.adapter=adapter
        val  datas=arrayListOf<String>()
        for (i in 0 until 40) {
            datas.add("$i")
        }
        adapter.submitList(datas)

        SVGAParser.shareParser().decodeFromAssets2("test.svga",object :SvgaLoadListener{
            override fun onLoadSuccess(videoItem: SVGAVideoEntity) {
                binding.svga.setVideoItem(videoItem)
                binding.svga.stepToFrame(0, true)
            }

            override fun onLoadFailed(error: String) {
            }

        })
        binding.svga.setOnClickListener{
            binding.svga.clear()
        }
        binding.btnClear.setOnClickListener{
            adapter.submitList(arrayListOf<String>())
            startActivity(Intent(this, NextActivity::class.java))
            finish()
        }
    }
}