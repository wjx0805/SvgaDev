package com.andtest.svgadev

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.opensource.svgaplayer.SVGAImageView
import kotlin.let


/**
 * svga 动画统一处理缓存播放   (注意：不适用同一相同地址多次播放, 只用于头像框显示)
 */
class AAA_ListLocalSvgaView_BBB @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SVGAImageView(context, attrs, defStyleAttr) {

    private val kkk_lifecycleObserver_lll = object :LifecycleEventObserver{
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if(event == Lifecycle.Event.ON_DESTROY){
                kkk_lifecycle_lll?.removeObserver(this)
                kkk_lifecycle_lll = null
                clear()
            }

        }
    }

    private var kkk_lifecycle_lll:Lifecycle?=null


    //FUNCMARK

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViewTreeLifecycleOwner()?.let {
            kkk_lifecycle_lll=it.lifecycle
            kkk_lifecycle_lll?.addObserver(kkk_lifecycleObserver_lll)
        }

    }
    //FUNCMARK

    //FUNCMARK

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}