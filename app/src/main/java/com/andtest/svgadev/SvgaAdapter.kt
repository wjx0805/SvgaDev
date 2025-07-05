package com.andtest.svgadev

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andtest.svgadev.databinding.ItemSvgaBinding
import com.opensource.svgaplayer.SVGAImageView
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import com.opensource.svgaplayer.SvgaCacheManager.SvgaLoadListener
import java.lang.ref.WeakReference


class SvgaAdapter : ListAdapter<String, RecyclerView.ViewHolder>(DataDiffUtils()) {

    inner class ItemViewHolder(
        var binding: ItemSvgaBinding
    ) :   RecyclerView.ViewHolder(binding.root){

         fun bindData(data: String, position: Int) {

             val kkk_view_lll=WeakReference<SVGAImageView>(binding.svga)

//             SVGAParser.shareParser().decodeFromAssets("test.svga", object : SVGAParser.ParseCompletion {
//                 override fun onComplete(videoItem: SVGAVideoEntity) {
//                     kkk_view_lll.get()?.stopAnimation(true)
//                     kkk_view_lll.get()?.setVideoItem(videoItem)
//                     kkk_view_lll.get()?.stepToFrame(0, true)
//                 }
//
//                 override fun onError() {
//                 }
//             })
             SVGAParser.shareParser().decodeFromAssets2("test.svga",object :SvgaLoadListener{
                 override fun onLoadSuccess(videoItem: SVGAVideoEntity) {
                     kkk_view_lll.get()?.setVideoItem(videoItem)
                     kkk_view_lll.get()?.stepToFrame(0, true)
                 }

                 override fun onLoadFailed(error: String) {
                 }

             })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            (holder as? ItemViewHolder)?.bindData(it,position)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            ItemSvgaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
}

class DataDiffUtils : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(
        oldItem: String,
        newItem: String
    ): Boolean {
        return false
    }

    override fun areContentsTheSame(
        oldItem: String,
        newItem: String
    ): Boolean {
        return false
    }
}
