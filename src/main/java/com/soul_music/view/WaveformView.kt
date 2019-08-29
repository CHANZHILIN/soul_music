/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soul_music.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.soul_music.R
import com.soul_music.utils.SoundFile
import java.util.*


/**
 * WaveformView 这个根据你的音频进行处理成完整的波形
 * 如果文件很大可能会很慢哦
 */
class WaveformView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    // Colors
    var line_offset: Int = 0
    private var mGridPaint: Paint? = null
    private var mSelectedLinePaint: Paint? = null
    private var mUnselectedLinePaint: Paint? = null
    private var mUnselectedBkgndLinePaint: Paint? = null
    private var mBorderLinePaint: Paint? = null
    private var mPlaybackLinePaint: Paint? = null
    private var mTimecodePaint: Paint? = null
    private var circlePaint: Paint? = null
    internal lateinit var paintLine: Paint
    var playFinish: Int = 0


    private var markTextPaint: Paint? = null
    private var markIcon: Bitmap? = null
    private var bitWidth: Int = 0
    private var bitHeight: Int = 0
    private var bottomHalfPaint: Paint? = null

    private val cutPoint = ArrayList<Float>()


    private val selectAreas = HashMap<Float, FloatArray>()

    private val selectPoints = ArrayList<FloatArray>()

    private var flags: MutableList<Int>? = null


    private var mSoundFile: SoundFile? = null
    private var mLenByZoomLevel: IntArray? = null
    private var mValuesByZoomLevel: Array<DoubleArray?>? = null
    private var mZoomFactorByZoomLevel: DoubleArray? = null
    private var mHeightsAtThisZoomLevel: IntArray? = null
    private var mZoomLevel: Int = 0
    private var mNumZoomLevels: Int = 0
    private var mSampleRate: Int = 0
    private var mSamplesPerFrame: Int = 0
    var offset: Int = 0
        private set
    var start: Int = 0
        private set
    var end: Int = 0
        private set
    private var mPlaybackPos: Int = 0
    private var mDensity: Float = 0.toFloat()
    private val mInitialScaleSpan: Float = 0.toFloat()
    var isInitialized: Boolean = false
        private set
    var state = 0
    private var cutPaint: Paint? = null
    private var mSelectedPaint: Paint? = null

    private var mAudioLineColor = Color.WHITE
    private var mAudioBgColor = Color.BLACK

    var zoomLevel: Int
        get() = mZoomLevel
        set(zoomLevel) {
            while (mZoomLevel > zoomLevel) {
                zoomIn()
            }
            while (mZoomLevel < zoomLevel) {
                zoomOut()
            }
        }


    private var startPostion = 0

    /**
     * 返回选择区域的坐标
     *
     * @return
     */
    var selcetPoint = 0f
        private set
    private var touchX1: Float = 0.toFloat()

    /**
     * 返回分割点的集合
     *
     * @return
     */
    //获取key值
    val clipPosition: MutableList<FloatArray>
        get() {

            val tempArray = FloatArray(selectAreas.size)

            val iterator = selectAreas.keys.iterator()
            var flag = 0
            while (iterator.hasNext()) {
                val next = iterator.next()
                tempArray[flag] = next
                flag = flag + 1
            }
            for (i in tempArray.indices) {
                for (j in tempArray.indices) {
                    if (tempArray[i] < tempArray[j]) {
                        var temp = 0f
                        temp = tempArray[j]
                        tempArray[j] = tempArray[i]
                        tempArray[i] = temp
                    }
                }
            }
            for (i in tempArray.indices) {
                selectAreas[tempArray[i]]?.let { selectPoints.add(it) }
            }
            return selectPoints
        }


    init {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // We don't want keys, the markers get these
        isFocusable = false
        circlePaint = Paint()//画圆
        circlePaint!!.color = Color.rgb(246, 131, 126)
        circlePaint!!.isAntiAlias = true


        //TODO
        cutPaint = Paint()//切割线一
        cutPaint!!.color = Color.rgb(255, 0, 0)
        cutPaint!!.isAntiAlias = true

        mSelectedPaint = Paint()//选择区域
        mSelectedPaint!!.isAntiAlias = false
        mSelectedPaint!!.color = Color.argb(200, 255, 0, 0)


        mGridPaint = Paint()
        mGridPaint!!.isAntiAlias = false
        mGridPaint!!.color = resources.getColor(R.color.grid_line)
        mSelectedLinePaint = Paint()
        mSelectedLinePaint!!.isAntiAlias = false
        mSelectedLinePaint!!.color = resources.getColor(R.color.waveform_selected)
        mUnselectedLinePaint = Paint()
        mUnselectedLinePaint!!.isAntiAlias = false
        mUnselectedLinePaint!!.color = resources.getColor(R.color.waveform_unselected)
        mUnselectedBkgndLinePaint = Paint()
        mUnselectedBkgndLinePaint!!.isAntiAlias = false
        mUnselectedBkgndLinePaint!!.color = resources.getColor(
                R.color.waveform_unselected_bkgnd_overlay)
        mBorderLinePaint = Paint()
        mBorderLinePaint!!.isAntiAlias = true
        mBorderLinePaint!!.strokeWidth = 1.5f
        mBorderLinePaint!!.pathEffect = DashPathEffect(floatArrayOf(3.0f, 2.0f), 0.0f)
        mBorderLinePaint!!.color = resources.getColor(R.color.selection_border)
        mPlaybackLinePaint = Paint()
        mPlaybackLinePaint!!.isAntiAlias = false
        mPlaybackLinePaint!!.color = resources.getColor(R.color.playback_indicator)
        mTimecodePaint = Paint()
        mTimecodePaint!!.textSize = 12f
        mTimecodePaint!!.isAntiAlias = true
        mTimecodePaint!!.color = resources.getColor(R.color.timecode)
        mTimecodePaint!!.setShadowLayer(
                2f, 1f, 1f,
                resources.getColor(R.color.timecode_shadow))


        markTextPaint = Paint()    //标记
        markTextPaint!!.color = context.resources.getColor(R.color.colorAccent)
        markTextPaint!!.textSize = 24f

        markIcon = (context.resources.getDrawable(R.mipmap.mark) as BitmapDrawable).bitmap
        bitWidth = markIcon!!.width
        bitHeight = markIcon!!.height

        bottomHalfPaint = Paint()
        bottomHalfPaint!!.strokeWidth = 1f
        bottomHalfPaint!!.color = context.resources.getColor(R.color.hui)


        val a = context.obtainStyledAttributes(attrs,
                R.styleable.WaveformView, defStyleAttr, 0)

        mAudioLineColor = a.getColor(R.styleable.WaveformView_audio_line_color, Color.WHITE)
        mAudioBgColor = a.getColor(R.styleable.WaveformView_audio_bg_color, Color.BLACK)


        mSoundFile = null
        mLenByZoomLevel = null
        mValuesByZoomLevel = null
        mHeightsAtThisZoomLevel = null
        offset = 0
        mPlaybackPos = -1
        start = 0
        end = 0
        mDensity = 1.0f
        isInitialized = false
    }

    fun hasSoundFile(): Boolean {
        return mSoundFile != null
    }

    fun setSoundFile(soundFile: SoundFile) {
        mSoundFile = soundFile
        mSampleRate = mSoundFile!!.sampleRate
        mSamplesPerFrame = mSoundFile!!.samplesPerFrame
        computeDoublesForAllZoomLevels()
        mHeightsAtThisZoomLevel = null
    }

    fun canZoomIn(): Boolean {
        return mZoomLevel > 0
    }

    fun zoomIn() {
        if (canZoomIn()) {
            mZoomLevel--
            start *= 2
            end *= 2
            mHeightsAtThisZoomLevel = null
            var offsetCenter = offset + measuredWidth / 2
            offsetCenter *= 2
            offset = offsetCenter - measuredWidth / 2
            if (offset < 0)
                offset = 0
            invalidate()
        }
    }

    fun canZoomOut(): Boolean {
        return mZoomLevel < mNumZoomLevels - 1
    }

    fun zoomOut() {
        if (canZoomOut()) {
            mZoomLevel++
            start /= 2
            end /= 2
            var offsetCenter = offset + measuredWidth / 2
            offsetCenter /= 2
            offset = offsetCenter - measuredWidth / 2
            if (offset < 0)
                offset = 0
            mHeightsAtThisZoomLevel = null
            invalidate()
        }
    }

    fun maxPos(): Int {
        return mLenByZoomLevel!![mZoomLevel]
    }

    fun secondsToFrames(seconds: Double): Int {
        return (1.0 * seconds * mSampleRate.toDouble() / mSamplesPerFrame + 0.5).toInt()
    }

    fun secondsToPixels(seconds: Double): Int {
        val z = mZoomFactorByZoomLevel!![mZoomLevel]
        return (z * seconds * mSampleRate.toDouble() / mSamplesPerFrame + 0.5).toInt()
    }

    fun pixelsToSeconds(pixels: Int): Double {
        val z = mZoomFactorByZoomLevel!![0]
        return mSoundFile!!.getmNumFramesFloat().toDouble() * 2.0 * mSamplesPerFrame.toDouble() / (mSampleRate * z)
    }

    fun millisecsToPixels(msecs: Int): Int {
        val z = mZoomFactorByZoomLevel!![mZoomLevel]
        return (msecs.toDouble() * 1.0 * mSampleRate.toDouble() * z / (1000.0 * mSamplesPerFrame) + 0.5).toInt()
    }

    fun pixelsToMillisecs(pixels: Int): Int {
        val z = mZoomFactorByZoomLevel!![mZoomLevel]
        return (pixels * (1000.0 * mSamplesPerFrame) / (mSampleRate * z) + 0.5).toInt()
    }

    fun pixelsToMillisecsTotal(): Int {
        val z = mZoomFactorByZoomLevel!![mZoomLevel]
        return (mSoundFile!!.getmNumFramesFloat().toDouble() * 1.0 * (1000.0 * mSamplesPerFrame) / (mSampleRate * 1) + 0.5).toInt()
    }

    fun setParameters(start: Int, end: Int, offset: Int) {
        this.start = start
        this.end = end
        this.offset = offset
    }

    fun setPlayback(pos: Int) {
        mPlaybackPos = pos
    }


    fun recomputeHeights(density: Float) {
        mHeightsAtThisZoomLevel = null
        mDensity = density
        mTimecodePaint!!.textSize = (12 * density).toInt().toFloat()
        invalidate()
    }


    protected fun drawWaveformLine(canvas: Canvas,
                                   x: Int, y0: Int, y1: Int,
                                   paint: Paint) {

        val pos = maxPos()
        val rat = measuredWidth.toFloat() / pos
        canvas.drawLine((x * rat).toInt().toFloat(), y0.toFloat(), (x * rat).toInt().toFloat(), y1.toFloat(), paint)
    }

    fun setStartPostion(x: Int) {
        this.startPostion = x
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        var height = measuredHeight - line_offset

        //        canvas.drawARGB(0, 42, 53, 82);
        canvas.drawColor(mAudioBgColor)    //背景颜色
        var centerLine = Paint()
        centerLine.color = Color.rgb(39, 199, 175)
        canvas.drawLine(0f, height * 0.5f + line_offset / 2, measuredWidth.toFloat(), height * 0.5f + line_offset / 2, centerLine)//中心线

        paintLine = Paint()
        paintLine.color = mAudioLineColor        //和音频谱线同色
        canvas.drawLine(0f, (line_offset / 2).toFloat(), measuredWidth.toFloat(), (line_offset / 2).toFloat(), paintLine)//最上面的那根线
        //            canvas.drawLine(0, height*0.25f+20, measuredWidth, height*0.25f+20, paintLine);//第二根线
        //            canvas.drawLine(0, height*0.75f+20, measuredWidth, height*0.75f+20, paintLine);//第3根线
        canvas.drawLine(0f, (measuredHeight - line_offset / 2 - 1).toFloat(), measuredWidth.toFloat(), (measuredHeight - line_offset / 2 - 1).toFloat(), paintLine)//最下面的那根线
        //        }
        if (state == 1) {
            mSoundFile = null
            state = 0
            return
        }


        for (i in cutPoint.indices) {
            canvas.drawLine(cutPoint[i], 0f, cutPoint[i], measuredHeight.toFloat(), cutPaint!!)
        }

        //        if (cutPoint.size() > 2) {
        //            canvas.drawRect(cutPoint.get(0), 0.0F, cutPoint.get(1),
        //                    measuredHeight, mSelectedPaint);
        //        }


        //选中区域选择
        for (j in cutPoint.indices) {
            if (j == 0 && selcetPoint != 0f) {
                if (selcetPoint < cutPoint[j]) {
                    val cutPostion = FloatArray(2)
                    if (selectAreas.containsKey(0.0f)) {
                        selectAreas.remove(0.0f)

                    } else {
                        cutPostion[0] = 0f
                        cutPostion[1] = cutPoint[j]
                        selectAreas[0.0f] = cutPostion
                    }
                    selcetPoint = 0f
                    break
                }
            }
            if (j == cutPoint.size - 1) {
                if (selcetPoint > cutPoint[j]) {
                    val cutPostion = FloatArray(2)
                    if (selectAreas.containsKey(cutPoint[j])) {
                        selectAreas.remove(cutPoint[j])

                    } else {
                        cutPostion[0] = cutPoint[j]
                        cutPostion[1] = width.toFloat()
                        selectAreas[cutPoint[j]] = cutPostion
                    }
                    selcetPoint = 0f
                    break
                }
            }

            if (j < cutPoint.size) {
                if (selcetPoint > cutPoint[j] && selcetPoint < cutPoint[j + 1] && selcetPoint != 0f) {
                    val cutPostion = FloatArray(2)
                    if (selectAreas.containsKey(cutPoint[j])) {
                        selectAreas.remove(cutPoint[j])
                    } else {
                        //        					canvas.drawRect(cutPoint.get(j), 0.0F, cutPoint.get(j+1),
                        //            	        			measuredHeight, mSelectedPaint);
                        cutPostion[0] = cutPoint[j]
                        cutPostion[1] = cutPoint[j + 1]
                        selectAreas[cutPoint[j]] = cutPostion
                    }
                    selcetPoint = 0f
                    break
                }
            }
        }

        selectPoints.clear()//每次进行刷新的操作，重新填充集合
        val iterator = selectAreas.keys.iterator()
        while (iterator.hasNext()) {
            //获取key值
            val next = iterator.next()
            val fs = selectAreas[next]
            canvas.drawRect(fs!![0], 0.0f, fs[1],
                    measuredHeight.toFloat(), mSelectedPaint!!)
        }


        //TODO标记点的绘制
        if (flags != null && flags!!.size > 0) {
            for (i in flags!!.indices) {
                var pos = flags!![i]
                if (pos > 0) {
                    pos = pos * measuredWidth / pixelsToMillisecsTotal()
                    val destRect = Rect(pos - bitWidth / 4, bitHeight / 2, pos - bitWidth / 4 + bitWidth / 2, bitHeight)
                    canvas.drawBitmap(markIcon!!, null, destRect, null)
                    val text = (i + 1).toString() + ""
                    val textWidth = markTextPaint!!.measureText(text)
                    val fontMetricsInt = markTextPaint!!.fontMetricsInt
                    val fontHeight = fontMetricsInt.bottom - fontMetricsInt.top
                    canvas.drawText(text, pos - textWidth / 2, (fontHeight + bitHeight / 2).toFloat(), markTextPaint!!)
                    canvas.drawLine(pos - textWidth / 2 + 5, bitHeight.toFloat(), pos - textWidth / 2 + 5, (measuredHeight - bitHeight / 2).toFloat(), bottomHalfPaint!!)
                }

            }

        }


        if (mSoundFile == null) {
            height = measuredHeight - line_offset
            centerLine = Paint()
            centerLine.color = Color.rgb(39, 199, 175)
            canvas.drawLine(0f, height * 0.5f + line_offset / 2, measuredWidth.toFloat(), height * 0.5f + line_offset / 2, centerLine)//中心线,音频线
            paintLine = Paint()
            paintLine.color = Color.rgb(169, 169, 169)
            //            canvas.drawLine(0, line_offset/2,measuredWidth, line_offset/2, paintLine);//最上面的那根线
            //           canvas.drawLine(0, height*0.25f+20, measuredWidth, height*0.25f+20, paintLine);//第二根线
            //           canvas.drawLine(0, height*0.75f+20, measuredWidth, height*0.75f+20, paintLine);//第3根线
            //            canvas.drawLine(0, measuredHeight-line_offset/2-1, measuredWidth, measuredHeight-line_offset/2-1, paintLine);//最下面的那根线
            return
        }
        if (mHeightsAtThisZoomLevel == null)
            computeIntsForThisZoomLevel()

        // Draw waveform
        val start = offset
        var width = mHeightsAtThisZoomLevel!!.size - start
        val ctr = measuredHeight / 2
        if (width > measuredWidth)
            width = measuredWidth

        // Draw grid
        val onePixelInSecs = pixelsToSeconds(1)
        val onlyEveryFiveSecs = onePixelInSecs > 1.0 / 50.0
        var fractionalSecs = offset * onePixelInSecs
        var integerSecs = fractionalSecs.toInt()
        var i = 0
        while (i < width) {
            i++
            fractionalSecs += onePixelInSecs
            val integerSecsNew = fractionalSecs.toInt()
            if (integerSecsNew != integerSecs) {
                integerSecs = integerSecsNew
            }
        }


        // Draw waveform
        i = 0
        while (i < maxPos()) {
            val paint: Paint
            if (i + start >= this.start && i + start < end) {
                paint = this!!.mSelectedLinePaint!!
            } else {
                paint = this!!.mUnselectedLinePaint!!
            }
            paint.color = mAudioLineColor        //音频谱线的颜色
            paint.strokeWidth = 1f

            drawWaveformLine(
                    canvas, i,
                    ctr - mHeightsAtThisZoomLevel!![start + i],
                    ctr + 1 + mHeightsAtThisZoomLevel!![start + i],
                    paint)

            if (i + start == mPlaybackPos && playFinish != 1) {
                canvas.drawCircle((i * getMeasuredWidth() / maxPos()).toFloat(), (line_offset / 4).toFloat(), (line_offset / 4).toFloat(), circlePaint!!)// 上圆
                canvas.drawCircle((i * getMeasuredWidth() / maxPos()).toFloat(), (getMeasuredHeight() - line_offset / 4).toFloat(), (line_offset / 4).toFloat(), circlePaint!!)// 下圆
                canvas.drawLine((i * getMeasuredWidth() / maxPos()).toFloat(), 0f, (i * getMeasuredWidth() / maxPos()).toFloat(), getMeasuredHeight().toFloat(), circlePaint!!)//垂直的线
                //画正在播放的线
                //canvas.drawLine(i*getMeasuredWidth()/maxPos(), 0, i*getMeasuredWidth()/maxPos(), measuredHeight, paint);
            }
            i++
        }

        // Draw timecode
        var timecodeIntervalSecs = 1.0
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 5.0
        }
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 15.0
        }
    }

    /**
     * 设置剪辑位置
     *
     * @param position
     */
    fun setCutPostion(position: Int) {
        selcetPoint = 0f
        var temp = cutPoint.size
        for (i in cutPoint.indices) {
            if (cutPoint[i] > position) {
                temp = i
                break
            }
        }
        cutPoint.add(temp, position.toFloat())
        invalidate()
    }

    /**
     * 显示选中的区域
     *
     * @param isFling
     */
    fun showSelectArea(isFling: Boolean) {
        if (!isFling) {
            //非滑动状态
            selcetPoint = touchX1
            invalidate()
        }
    }


    /**
     * 清空分割点
     */
    fun clearCutPoint() {
        cutPoint?.clear()
        selcetPoint = 0f
        //TODO
        selectAreas.clear()
        selectPoints.clear()
        invalidate()//重新绘制界面
    }

    /**
     * 设置时间标记点
     */
    fun setFlag(flagPositions: MutableList<Int>) {
        this.flags = flagPositions
        invalidate()//重新绘制音频
    }


    /**
     * 清除当前标记点
     */
    fun clearFlag() {
        flags!!.clear()//清除所有标记点
    }

    /**
     * 进行控件的触摸事件处理（记录触摸的坐标）
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchX1 = event.x
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_MOVE -> {
            }

            else -> {
            }
        }

        return super.onTouchEvent(event)
    }


    /**
     * Called once when a new sound file is added
     */
    private fun computeDoublesForAllZoomLevels() {
        val numFrames = mSoundFile!!.numFrames
        val frameGains = mSoundFile!!.frameGains
        val smoothedGains = DoubleArray(numFrames)
        if (numFrames == 1) {
            smoothedGains[0] = frameGains!!.get(0).toDouble()
        } else if (numFrames == 2) {
            smoothedGains[0] = frameGains!![0].toDouble()
            smoothedGains[1] = frameGains[1].toDouble()
        } else if (numFrames > 2) {
            smoothedGains[0] = frameGains!![0] / 2.0 + frameGains[1] / 2.0
            for (i in 1 until numFrames - 1) {
                smoothedGains[i] = frameGains[i - 1] / 3.0 +
                        frameGains[i] / 3.0 +
                        frameGains[i + 1] / 3.0
            }
            smoothedGains[numFrames - 1] = frameGains[numFrames - 2] / 2.0 + frameGains[numFrames - 1] / 2.0
        }

        // Make sure the range is no more than 0 - 255
        var maxGain = 1.0
        for (i in 0 until numFrames) {
            if (smoothedGains[i] > maxGain) {
                maxGain = smoothedGains[i]
            }
        }
        var scaleFactor = 1.0
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0.0
        val gainHist = IntArray(256)
        for (i in 0 until numFrames) {
            var smoothedGain = (smoothedGains[i] * scaleFactor).toInt()
            if (smoothedGain < 0)
                smoothedGain = 0
            if (smoothedGain > 255)
                smoothedGain = 255
            if (smoothedGain > maxGain)
                maxGain = smoothedGain.toDouble()

            gainHist[smoothedGain]++
        }

        // Re-calibrate the min to be 5%
        var minGain = 0.0
        var sum = 0
        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[minGain.toInt()]
            minGain++
        }

        // Re-calibrate the max to be 99%
        sum = 0
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[maxGain.toInt()]
            maxGain--
        }
        if (maxGain <= 50) {
            maxGain = 80.0
        } else if (maxGain > 50 && maxGain < 120) {
            maxGain = 142.0
        } else {
            maxGain += 10.0
        }


        // Compute the heights
        val heights = DoubleArray(numFrames)
        val range = maxGain - minGain
        for (i in 0 until numFrames) {
            var value = (smoothedGains[i] * scaleFactor - minGain) / range
            if (value < 0.0)
                value = 0.0
            if (value > 1.0)
                value = 1.0
            heights[i] = value * value
        }

        mNumZoomLevels = 5
        mLenByZoomLevel = IntArray(5)
        mZoomFactorByZoomLevel = DoubleArray(5)
        mValuesByZoomLevel = arrayOfNulls(5)

        // Level 0 is doubled, with interpolated values
        mLenByZoomLevel!![0] = numFrames * 2
        mZoomFactorByZoomLevel!![0] = 2.0
        mValuesByZoomLevel!![0] = DoubleArray(mLenByZoomLevel!![0])
        if (numFrames > 0) {
            mValuesByZoomLevel!![0]?.set(0, 0.5 * heights[0])
            mValuesByZoomLevel!![0]?.set(1, heights[0])
        }
        for (i in 1 until numFrames) {
            mValuesByZoomLevel!![0]?.set(2 * i, 0.5 * (heights[i - 1] + heights[i]))
            mValuesByZoomLevel!![0]?.set(2 * i + 1, heights[i])
        }

        // Level 1 is normal
        mLenByZoomLevel!![1] = numFrames
        mValuesByZoomLevel!![1] = DoubleArray(mLenByZoomLevel!![1])
        mZoomFactorByZoomLevel!![1] = 1.0
        for (i in 0 until mLenByZoomLevel!![1]) {
            mValuesByZoomLevel!![1]?.set(i, heights[i])
        }

        // 3 more levels are each halved
        for (j in 2..4) {
            mLenByZoomLevel!![j] = mLenByZoomLevel!![j - 1] / 2
            mValuesByZoomLevel!![j] = DoubleArray(mLenByZoomLevel!![j])
            mZoomFactorByZoomLevel!![j] = mZoomFactorByZoomLevel!![j - 1] / 2.0
            for (i in 0 until mLenByZoomLevel!![j]) {
                mValuesByZoomLevel!![j]?.set(i, 0.5 * ((mValuesByZoomLevel!![j - 1]?.get(2 * i)!! + mValuesByZoomLevel!![j - 1]?.get(2 * i + 1)!!)))
            }
        }


        if (numFrames > 5000) {
            mZoomLevel = 3
        } else if (numFrames > 1000) {
            mZoomLevel = 2
        } else if (numFrames > 300) {
            mZoomLevel = 1
        } else {
            mZoomLevel = 0
        }

        isInitialized = true
    }

    /**
     * Called the first time we need to draw when the zoom level has changed
     * or the screen is resized
     */
    private fun computeIntsForThisZoomLevel() {

        val halfHeight = measuredHeight / 2 - 1
        mHeightsAtThisZoomLevel = IntArray(mLenByZoomLevel!![mZoomLevel])
        for (i in 0 until mLenByZoomLevel!![mZoomLevel]) {
            mHeightsAtThisZoomLevel!![i] = (mValuesByZoomLevel!![mZoomLevel]!!.get(i) * halfHeight).toInt()
        }
    }
}
