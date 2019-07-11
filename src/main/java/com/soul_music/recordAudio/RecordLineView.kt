package com.soul_music.recordAudio

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.kotlin_baselib.api.Constants

/**
 *  Created by CHEN on 2019/7/2
 *  Email:1181785848@qq.com
 *  Package:com.soul_music
 *  Introduce:录音线
 **/
class RecordLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private val mContext: Context = context

    private val mCoo = Point(500, 500)//原点坐标
    private var mMaxHeight = 0.0//最到点       //默认为零
    private var min = -600.0//最小x
    private var max = 600.0//最大x
    private var Y = 0.0//初相
    private var A = mMaxHeight//振幅
    private var W: Double = 0.toDouble()//角频率
    private var mPaint: Paint//主画笔
    private var mPath: Path//主路径
    private var mReflexPath: Path//镜像路径
    private var mAnimator: ValueAnimator

    private var mHeight: Int = 0
    private var mWidth: Int = 0

    val colors: IntArray = intArrayOf(Color.parseColor("#F60C0C"), //红
            Color.parseColor("#F3B913"), //橙
            Color.parseColor("#E7F716"), //黄
            Color.parseColor("#3DF30B"), //绿
            Color.parseColor("#0DF6EF"), //青
            Color.parseColor("#0829FB"), //蓝
            Color.parseColor("#B709F4"))//紫

    val pos: FloatArray = floatArrayOf(1f / 7, 2f / 7, 3f / 7, 4f / 7, 5f / 7, 6f / 7, 1f)


    init {
        //初始化主画笔
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.setColor(Color.BLUE)
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setStrokeWidth(8f)
        //初始化主路径
        mPath = Path()
        mReflexPath = Path()


        //数字时间流
        mAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat())
        mAnimator.setDuration(200)
//        mAnimator.setRepeatCount(ValueAnimator.INFINITE)
//        mAnimator.setRepeatMode(ValueAnimator.REVERSE)
        mAnimator.setInterpolator(LinearInterpolator())
        mAnimator.addUpdateListener { a ->
            Y = (a.animatedValue as Float).toDouble()
            A = (mMaxHeight * (1 - a.animatedValue as Float / (2 * Math.PI))).toFloat().toDouble()
            invalidate()
        }

        mPaint.shader = LinearGradient(
                min.toFloat(), 0f, max.toFloat(), 0f,
                colors, pos,
                Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        mPath.reset()
        mReflexPath.reset()
        super.onDraw(canvas)
        canvas.save()
        canvas.translate((mWidth / 2).toFloat(), (mHeight / 2).toFloat())
        formPath()
        mPaint.alpha = 255
        canvas.drawPath(mPath, mPaint)
        mPaint.alpha = 66
        canvas.drawPath(mReflexPath, mPaint)
        canvas.restore()
    }
/*
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN ->*//* mAnimator.start()*//* setMaxHeight(Math.random() * 200);
        }//                setMaxHeight(RandomUtils.getRandom(200,  500));
        return true
    }*/

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
        mHeight = MeasureSpec.getSize(heightMeasureSpec)
        mMaxHeight = mHeight / 2 * 0.5
        min = (-mWidth / 2).toDouble()
        max = (mWidth / 2).toDouble()
        //        handleColor();
        setMeasuredDimension(mWidth, mHeight)
    }


    private fun formPath() {
        mPath.moveTo(min.toFloat(), f(min).toFloat())
        mReflexPath.moveTo(min.toFloat(), f(min).toFloat())
        var x = min
        while (x <= max) {
            val y = f(x)
            mPath.lineTo(x.toFloat(), y.toFloat())
            mReflexPath.lineTo(x.toFloat(), -y.toFloat())
            x++
        }
    }

    /**
     * 对应法则
     *
     * @param x 原像(自变量)
     * @return 像(因变量)
     */
    private fun f(x: Double): Double {
        val len = max - min
        val a = 4 / (4 + Math.pow(rad(x / Math.PI * 800 / len), 4.0))
        val aa = Math.pow(a, 2.5)
        W = 2 * Math.PI / (rad(len) / 2)
        return aa * A * Math.sin(W * rad(x) - Y)
    }

    private fun rad(deg: Double): Double {
        return deg / 180 * Math.PI
    }

    /**
     * 设置高度
     *
     * @param maxHeight
     */
    fun setMaxHeight(maxHeight: Double) {
        mMaxHeight = maxHeight
        mAnimator.start()
    }

}