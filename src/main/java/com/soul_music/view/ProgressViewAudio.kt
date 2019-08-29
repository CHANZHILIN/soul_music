package com.soul_music.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

import com.soul_music.R

import java.util.ArrayList

/**
 * Progress bar on the top of screen
 *
 * @author xiaodong
 */
class ProgressViewAudio : View {
    /**
     * 最大的可以录制的时长
     */
    var maxRecordTime: Int = 0
    /**
     * 是否处于编辑删除状态，如果为true，最后一段高亮显示
     */
    var isEditing = false
    /**
     * 存储断点百分比的集合
     */
    private var pausePoints: MutableList<Float>? = null
    var tipPoints: ArrayList<Float>? = null
    /**
     * 用于存储断点拍摄的点的进度值
     */
    var tips: ArrayList<Int>? = null
    /**
     * 用于存储打标记的点的进度值
     */
    var flags: ArrayList<Int>? = null
    private val mPaint = Paint()
    private val editPaint = Paint()
    private val mPausePaint = Paint()
    private val mTipPaint = Paint()


    private var shouldBeWidth = 0


    /**
     * 获取标记点
     *
     * @return
     */
    /**
     * 重新设置标记点
     *
     * @return
     */
    var pausePoint: MutableList<Float>?
        get() = pausePoints
        set(pausePoints) {
            this.pausePoints = pausePoints
            invalidate()
        }

    fun setWidth(width: Int) {
        shouldBeWidth = width
        invalidate()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, paramAttributeSet: AttributeSet) : super(context, paramAttributeSet) {
        init()
    }

    constructor(context: Context, paramAttributeSet: AttributeSet,
                paramInt: Int) : super(context, paramAttributeSet, paramInt) {
        init()
    }

    private fun init() {
        this.mPaint.style = Paint.Style.FILL
        this.mPaint.color = resources.getColor(R.color.vine_green)

        this.mPausePaint.style = Paint.Style.FILL
        this.mPausePaint.color = resources.getColor(R.color.progress_divider_color)

        this.mTipPaint.style = Paint.Style.FILL
        this.mTipPaint.color = resources.getColor(R.color.vine_green_1)

        this.editPaint.style = Paint.Style.FILL
        this.editPaint.color = resources.getColor(R.color.edit_color)
        pausePoints = ArrayList()
        tipPoints = ArrayList()

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0.0f, measuredWidth.toFloat(),
                measuredHeight.toFloat(), mPaint)


        if (pausePoints != null && !pausePoints!!.isEmpty()) {
            for (pause in pausePoints!!) {
                canvas.drawRect(pause, 0.0f, pause + DIVIDER_WIDTH,
                        measuredHeight.toFloat(), mTipPaint)
            }
        }
    }

    fun clearPausePoints() {
        if (pausePoints != null) {
            pausePoints!!.clear()
        }
        invalidate()
    }

    /**
     * 暂停断点显示
     *
     * @param pausePoint
     */
    fun addPausePoint(pausePoint: Float) {

        if (pausePoints != null && pausePoint != 0f) {
            pausePoints!!.add(pausePoint)
        }
        invalidate()
    }


    /**
     * 将打标记的时刻的播放进度进行存储
     *
     * @param progress
     */
    fun addTipProgress(progress: Int) {
        if (tips == null) {
            tips = ArrayList()
        }
        tips!!.add(progress)
    }

    fun addFlagProgress(progress: Int) {
        if (flags == null) {
            flags = ArrayList()
        }
        flags!!.add(progress)
    }

    /**
     * tip断点显示
     */
    fun addTipPoint(tipPoint: Float) {
        if (tipPoints != null) {
            tipPoints!!.add(tipPoint)
        }
    }

    /**
     * tip断点清除
     */
    fun clearTipPoints() {
        if (tipPoints != null) {
            tipPoints!!.clear()
        }
    }

    /**
     * 移除最后一个分段视频的记录的断点
     */
    fun removeLastPausePoint() {
        if (pausePoints != null && !pausePoints!!.isEmpty()) {
            pausePoints!!.removeAt(pausePoints!!.size - 1)
        }
    }

    /**
     * 做一些清空的操作
     */
    fun doClear() {
        if (tips != null) {
            tips!!.clear()
            tips = null
        }
        if (flags != null) {
            flags!!.clear()
            flags = null

        }
        if (pausePoints != null) {
            pausePoints!!.clear()
            pausePoints = null
        }
        if (tipPoints != null) {
            tipPoints!!.clear()
            tipPoints = null
        }
    }

    companion object {
        /**
         * 断点分隔线的宽度
         */
        private val DIVIDER_WIDTH = 2
    }


}
