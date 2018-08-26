/*
 * Copyright 2013 Sony Corporation
 */

package com.thibaudperso.sonycamera.timelapse.ui.adjustments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

/**
 * A SurfaceView based class to draw stream frames serially.
 */
class SimpleStreamSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback {

    companion object : KLogging();

    private val framePaint: Paint = Paint().apply {
        isDither = true
    }
    private val blackFramePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    // State
    private var streamSubscription: Disposable? = null
    private var previousWidth = 0
    private var previousHeight = 0

    /**
     * Check to see whether start() is already called.
     *
     * @return true if start() is already called, false otherwise.
     */
    val isStarted: Boolean
        get() = streamSubscription != null

    init {
        holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        streamSubscription?.dispose()
        streamSubscription = null
    }

    /**
     * Start retrieving and drawing stream frame data by new threads.
     *
     * @return true if the starting is completed successfully, false otherwise.
     * @throws IllegalStateException when Remote API object is not set.
     */
    fun start(liveViewStreamService: LiveViewStreamService, liveviewUrl: String): Boolean {
        if (isStarted) {
            logger.warn("start() already starting.")
            return false
        }

        streamSubscription = liveViewStreamService.stream(HttpUrl.get(liveviewUrl))
                .subscribeOn(Schedulers.io())
                // limit to 60fps
                .throttleLatest(1000L / 60L, TimeUnit.MILLISECONDS)
                .map(PayloadToBitmapMapper())
                .doOnNext(this::drawFrame)
                .retry()
                .subscribe()
        return true
    }

    /**
     * Request to stop retrieving and drawing stream data.
     */
    fun stop() {
        streamSubscription?.dispose()
        streamSubscription = null
    }

    /**
     * Draw frame bitmap onto a canvas.
     *
     * @param frame
     */
    private fun drawFrame(frame: Bitmap) {
        if (frame.width != previousWidth || frame.height != previousHeight) {
            onDetectedFrameSizeChanged(frame.width, frame.height)
            return
        }

        val canvas = holder.lockCanvas() ?: return
        val w = frame.width
        val h = frame.height
        val src = Rect(0, 0, w, h)

        val by = Math.min(width.toFloat() / w, height.toFloat() / h)
        val offsetX = (width - (w * by).toInt()) / 2
        val offsetY = (height - (h * by).toInt()) / 2
        val dst = Rect(offsetX, offsetY, width - offsetX, height - offsetY)
        canvas.drawBitmap(frame, src, dst, framePaint)
        holder.unlockCanvasAndPost(canvas)
    }

    // Called when the width or height of stream frame image is changed.
    private fun onDetectedFrameSizeChanged(width: Int, height: Int) {
        previousWidth = width
        previousHeight = height
        drawBlackFrame()
        drawBlackFrame()
        drawBlackFrame() // delete triple buffers

        post {
            layoutParams = layoutParams.apply {
                this.width = getWidth()
                this.height = (height.toFloat() / width.toFloat() * getWidth().toFloat()).toInt()
            }
        }
    }

    // Draw black screen.
    private fun drawBlackFrame() {
        val canvas = holder.lockCanvas() ?: return
        canvas.drawRect(Rect(0, 0, width, height), blackFramePaint)
        holder.unlockCanvasAndPost(canvas)
    }

}