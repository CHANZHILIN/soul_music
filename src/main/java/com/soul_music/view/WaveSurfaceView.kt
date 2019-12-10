package com.soul_music.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 该类只是一个初始化surfaceview的封装
 *
 * @author tcx
 */
class WaveSurfaceView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    var line_off: Int = 0//上下边距距离


    init {
        val holder: SurfaceHolder = getHolder()
        holder.addCallback(this)
    }


    /**
     * @author tcx
     * init surfaceview
     */
    fun initSurfaceView(sfv: SurfaceView) {
        object : Thread() {
            override fun run() {
                val canvas = sfv.holder.lockCanvas(
                        Rect(0, 0, sfv.width, sfv.height)) ?: return// 关键:获取画布
                canvas.drawARGB(255, 0, 0, 0)// 清除背景(黑色)

                val height = sfv.height - line_off
                val paintLine = Paint()
                val centerLine = Paint()
                val circlePaint = Paint()
                circlePaint.color = Color.rgb(246, 131, 126)
                paintLine.color = Color.rgb(255, 255, 255)
                paintLine.strokeWidth = 2f
                circlePaint.isAntiAlias = true

                canvas.drawLine((sfv.width / 2).toFloat(), 0f, (sfv.width / 2).toFloat(), sfv.height.toFloat(), circlePaint)//垂直的线
                centerLine.color = Color.rgb(39, 199, 175)
                canvas.drawLine(0f, (line_off / 2).toFloat(), sfv.width.toFloat(), (line_off / 2).toFloat(), paintLine)//最上面的那根线
                canvas.drawLine(0f, (sfv.height - line_off / 2 - 1).toFloat(), sfv.width.toFloat(), (sfv.height - line_off / 2 - 1).toFloat(), paintLine)//最下面的那根线
                canvas.drawLine(0f, height * 0.5f + line_off / 2, sfv.width.toFloat(), height * 0.5f + line_off / 2, centerLine)//中心线
                sfv.holder.unlockCanvasAndPost(canvas)// 解锁画布，提交画好的图像
            }
        }.start()

    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initSurfaceView(this)

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }




}
