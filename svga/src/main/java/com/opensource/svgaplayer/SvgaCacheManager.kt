package com.opensource.svgaplayer

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class SvgaCacheManager private constructor() {
    companion object {
        private const val TAG = "SvgaCacheManager"
        @SuppressLint("StaticFieldLeak")
        private var instance: SvgaCacheManager? = null

        fun getInstance(): SvgaCacheManager {
            return instance ?: synchronized(this) {
                instance ?: SvgaCacheManager().also { instance = it }
            }
        }
    }


    // 使用 LruCache 管理 SVGADrawable 缓存
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val svgaCache = object : LruCache<String, CacheItem>(cacheSize) {
        override fun sizeOf(key: String, item: CacheItem): Int {
            // 估算 Drawable 大小
            return 1
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: CacheItem?, newValue: CacheItem?) {
            // 条目被移除时释放资源
            oldValue?.let {
                if (!it.isResourcesReleased) {
                    releaseSvgaResources(it.svgaVideoEntity)
                    it.isResourcesReleased = true
                    Log.d(TAG, "Released SVGA resources on eviction: $key")
                }
            }
        }
    }

    // 缓存项类，包含 Drawable 和资源释放状态
    inner class CacheItem(val svgaVideoEntity: SVGAVideoEntity, var isResourcesReleased: Boolean = false)

    init {
        // 注册内存状态监听器

    }

    fun initMemoryCallBack(context: Context?){
        context?.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                // 根据内存级别决定释放多少资源
                when {
                    level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                        // 后台运行，释放50%的最少使用缓存
                        Log.d(TAG, "Memory low, trimming SVGA cache: $level")
                        trimCache(0.5f)
                    }
                    level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                        // 内存中度紧张，释放所有未使用的缓存
                        Log.d(TAG, "Memory moderate, clearing unused SVGA cache: $level")
                        clearUnusedResources()
                    }
                    level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                        // 内存严重不足，释放所有缓存
                        Log.d(TAG, "Memory critical, clearing all SVGA cache: $level")
//                        clearAllResources()
                    }
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) {
                // 配置变更时的处理
            }

            override fun onLowMemory() {
                // 系统内存不足时的处理
                Log.d(TAG, "System low memory, clearing all SVGA cache")
                clearAllResources()
            }
        })
    }

    // 加载 SVGA 资源并缓存
    // 新增：记录正在加载的资源及其等待的监听器列表
    private val loadingResources = ConcurrentHashMap<String, MutableList<SvgaLoadListener>>()

    // 改进后的 loadSvgaResource 方法
    fun loadSvgaResource(key: String, assetName: String, listener: SvgaLoadListener) {
        // 先检查缓存
        val cachedItem = svgaCache.get(key)
        if (cachedItem != null) {
            if (cachedItem.isResourcesReleased) {
                // 资源已释放，需要重新加载
                enqueueOrLoadResource(key, assetName, listener)
            } else {
                // 使用缓存的 Drawable
                listener.onLoadSuccess(cachedItem.svgaVideoEntity)
            }
            return
        }
        // 缓存中没有，检查是否已有其他任务正在加载
        enqueueOrLoadResource(key, assetName, listener)
    }

    // 新增：排队或加载资源的方法
    private fun enqueueOrLoadResource(key: String, assetName: String, listener: SvgaLoadListener) {
        // 检查是否已有其他任务正在加载此资源
        val listeners = loadingResources.getOrPut(key) { mutableListOf() }

        synchronized(listeners) {
            if (listeners.isEmpty()) {
                // 没有其他任务在加载，创建新的加载任务
                listeners.add(listener)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val drawable = loadAndParseSvga(key, assetName)

                        // 通知所有等待的监听器
                        withContext(Dispatchers.Main) {
                            notifyListeners(key, drawable, null)
                        }
                    } catch (e: Exception) {
                        // 通知所有等待的监听器加载失败
                        withContext(Dispatchers.Main) {
                            notifyListeners(key, null, e.message ?: "Load failed")
                        }
                    }
                }
            } else {
                // 已有任务在加载，加入等待列表
                listeners.add(listener)
            }
        }
    }

    // 新增：加载并解析 SVGA 资源的方法
    private suspend fun loadAndParseSvga(key: String, assetName: String): SVGAVideoEntity {
        return withContext(Dispatchers.IO) {
            try {
                val assetStream = SVGAParser.shareParser().mContext!!.assets.open(assetName)

                // 使用 CompletableDeferred 包装异步回调
                val deferred = CompletableDeferred<SVGAVideoEntity>()

                SVGAParser.shareParser().decodeFromInputStream(assetStream, assetName, object : SVGAParser.ParseCompletion {
                    override fun onComplete(videoItem: SVGAVideoEntity) {
                        deferred.complete(videoItem)
                    }

                    override fun onError() {
                        deferred.completeExceptionally(Exception("Parse failed"))
                    }
                }, false)

                // 等待解析完成
                val drawable = deferred.await()

                // 解析完成后存入缓存
                svgaCache.put(key, CacheItem(drawable))

                drawable
            } catch (e: IOException) {
                throw Exception("Asset not found: $assetName", e)
            }
        }
    }

    // 新增：通知所有等待的监听器
    private fun notifyListeners(key: String, drawable: SVGAVideoEntity?, error: String?) {
        val listeners = loadingResources.remove(key) ?: return

        listeners.forEach { listener ->
            if (drawable != null) {
                listener.onLoadSuccess(drawable)
            } else {
                listener.onLoadFailed(error ?: "Unknown error")
            }
        }
    }

    // 从 Asset 加载 SVGA


    // 释放 SVGA Drawable 资源
    private fun releaseSvgaResources(drawable: SVGAVideoEntity) {
        // 释放 SVGA 内部资源
        drawable.release()
        // 其他必要的资源释放操作
    }

    // 释放指定资源
    fun releaseResource(key: String) {
        val item = svgaCache.get(key)
        item?.let {
            if (!it.isResourcesReleased) {
                releaseSvgaResources(it.svgaVideoEntity)
                it.isResourcesReleased = true
                svgaCache.remove(key)
                Log.d(TAG, "Manually released SVGA resource: $key")
            }
        }
    }

    // 清理未使用的资源
    fun clearUnusedResources() {
        svgaCache.snapshot().forEach { (key, item) ->
            if (item != null && !item.isResourcesReleased && item.svgaVideoEntity.refrenceCount<=0) {
                releaseSvgaResources(item.svgaVideoEntity)
                item.isResourcesReleased = true
                svgaCache.remove(key)
            }
        }
    }

    // 清理指定比例的缓存
    fun trimCache(percentage: Float) {
        val count = (svgaCache.size() * percentage).toInt()
        if (count <= 0) return

        // 获取最不常用的条目并释放
        var removed = 0
        svgaCache.snapshot().forEach { (key, item) ->
            if (removed >= count) return@forEach

            if (item != null && !item.isResourcesReleased && item.svgaVideoEntity.refrenceCount<=0) {
                releaseSvgaResources(item.svgaVideoEntity)
                item.isResourcesReleased = true
                svgaCache.remove(key)
            }
        }
    }

    // 清理所有资源
    fun clearAllResources() {
        svgaCache.snapshot().forEach { (_, item) ->
            if (item != null && !item.isResourcesReleased) {
                releaseSvgaResources(item.svgaVideoEntity)
                item.isResourcesReleased = true
                item.svgaVideoEntity.refrenceCount=0
            }
        }
        svgaCache.evictAll()
    }

    // 加载监听器接口
    interface SvgaLoadListener {
        fun onLoadSuccess(svgaVideoEntity: SVGAVideoEntity)
        fun onLoadFailed(error: String)
    }
}