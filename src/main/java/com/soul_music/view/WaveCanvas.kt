package com.soul_music.view

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Style
import android.graphics.drawable.BitmapDrawable
import android.media.AudioRecord
import android.os.AsyncTask
import android.os.Handler.Callback
import android.os.Message
import android.view.SurfaceView
import com.soul_music.R
import com.soul_music.myInterface.CurrentPosInterface
import com.soul_music.utils.Pcm2Wav
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.experimental.and


/**
 * 录音和写入文件使用了两个不同的线程，以免造成卡机现象
 * 录音波形绘制
 *
 * @author tcx
 */
class WaveCanvas {

    private val inBuf = ArrayList<Short>()//缓冲区数据
    private val write_data = ArrayList<ByteArray>()//写入文件数据
    var isRecording = false// 录音线程控制标记
    private var isWriting = false// 录音线程控制标记

    private var line_off: Int = 0//上下边距的距离
    var rateX = 30//控制多少帧取一帧
    var rateY = 1 //  Y轴缩小的比例 默认为1
    var baseLine = 0// Y轴基线
    private var audioRecord: AudioRecord? = null
    internal var recBufSize: Int = 0
    private var marginRight = 30//波形图绘制距离右边的距离
    private val draw_time = 1000 / 200//两次绘图间隔的时间
    private val divider = 0.2f//为了节约绘画时间，每0.2个像素画一个数据
    internal var c_time: Long = 0
    private var savePcmPath: String? = null//保存pcm文件路径
    private var saveWavPath: String? = null//保存wav文件路径
    private var circlePaint: Paint? = null
    private var center: Paint? = null
    private var paintLine: Paint? = null
    private var mPaint: Paint? = null
    private var mContext: Context? = null
    private val markList = ArrayList<Float>()
    private var readsize: Int = 0

    private var markMap: MutableMap<Int, Float> = HashMap()
    private var isPause = false
    private var mCurrentPosInterface: CurrentPosInterface? = null
    private var progressPaint: Paint? = null
    private var paint: Paint? = null
    private var bottomHalfPaint: Paint? = null
    private var darkPaint: Paint? = null
    private var markTextPaint: Paint? = null
    private var markIcon: Bitmap? = null
    private var bitWidth: Int = 0
    private var bitHeight: Int = 0
    private var start: Int = 0
    private var sfv: SurfaceView? = null
    private var canvas: Canvas? = null

    private var mDividerX = 0

    /**
     * 开始录音
     *
     * @param audioRecord
     * @param recBufSize
     * @param sfv
     * @param audioName
     */
    fun Start(audioRecord: AudioRecord, recBufSize: Int, sfv: SurfaceView, audioName: String, path: String, callback: Callback, width: Int, context: Context) {
        this.audioRecord = audioRecord
        isRecording = true
        isWriting = true
        this.sfv = sfv
        this.recBufSize = recBufSize
        savePcmPath = "$path$audioName.pcm"
        saveWavPath = "$path$audioName.wav"
        this.mContext = context
        init()
        Thread(WriteRunnable()).start()//开线程写文件
        RecordTask(audioRecord, recBufSize, sfv, mPaint!!, callback).execute()
        this.marginRight = width
    }

    fun init() {
        circlePaint = Paint()//画圆
        circlePaint!!.color = Color.rgb(246, 131, 126)//设置上圆的颜色
        center = Paint()
        center!!.color = Color.rgb(39, 199, 175)// 画笔为color
        center!!.strokeWidth = 1f// 设置画笔粗细
        center!!.isAntiAlias = true
        center!!.isFilterBitmap = true
        center!!.style = Style.FILL
        paintLine = Paint()
        paintLine!!.color = Color.rgb(255, 255, 255)
        paintLine!!.strokeWidth = 2f// 设置画笔粗细


        mPaint = Paint()
        mPaint!!.color = Color.rgb(39, 199, 175)// 画笔为color
        mPaint!!.strokeWidth = 1f// 设置画笔粗细
        mPaint!!.isAntiAlias = true
        mPaint!!.isFilterBitmap = true
        mPaint!!.style = Style.FILL


        //标记 部分画笔
        progressPaint = Paint()
        progressPaint!!.color = mContext!!.resources.getColor(R.color.vine_green)
        paint = Paint()
        bottomHalfPaint = Paint()
        darkPaint = Paint()
        darkPaint!!.color = mContext!!.resources.getColor(R.color.dark_black)
        bottomHalfPaint!!.color = mContext!!.resources.getColor(R.color.hui)
        markTextPaint = Paint()
        markTextPaint!!.color = mContext!!.resources.getColor(R.color.colorAccent)
        markTextPaint!!.textSize = 24f
        paint!!.isAntiAlias = true
        paint!!.isDither = true
        paint!!.isFilterBitmap = true
        markIcon = (mContext!!.resources.getDrawable(R.mipmap.mark) as BitmapDrawable).bitmap
        bitWidth = markIcon!!.width
        bitHeight = markIcon!!.height


    }


    /**
     * 停止录音
     */
    fun Stop() {
        isRecording = false
        isPause = true
        audioRecord!!.stop()

    }

    /**
     * pause recording audio
     */
    fun pause() {
        isPause = true
    }


    /**
     * restart recording audio
     */
    fun reStart() {
        isPause = false
    }


    /**
     * 清除数据
     */
    fun clear() {
        inBuf.clear()// 清除
    }


    /**
     * 异步录音程序
     *
     * @author cokus
     */
    internal inner class RecordTask constructor(private val audioRecord: AudioRecord, private val recBufSize: Int,
                                                private val sfv: SurfaceView// 画板
                                                , private val mPaint: Paint// 画笔
                                                , private val callback: Callback) : AsyncTask<Any, Any, Any>() {
        private val isStart = false
        private val srcRect: Rect? = null
        private var destRect: Rect? = null
        private val bottomHalfBgRect: Rect? = null


        init {
            line_off = (sfv as WaveSurfaceView).line_off
            inBuf.clear()// 清除
        }

        override fun doInBackground(vararg params: Any): Any? {
            try {
                val buffer = ShortArray(recBufSize)
                audioRecord.startRecording()// 开始录制
                while (isRecording) {

                    while (!isPause) {
                        // 从MIC保存数据到缓冲区
                        readsize = audioRecord.read(buffer, 0,
                                recBufSize)
                        synchronized(inBuf) {
                            var i = 0
                            while (i < readsize) {
                                inBuf.add(buffer[i])
                                i += rateX
                            }
                        }
                        publishProgress()//更新主线程中的UI
                        if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                            synchronized(write_data) {
                                val bys = ByteArray(readsize * 2)
                                //因为arm字节序问题，所以需要高低位交换
                                for (i in 0 until readsize) {
                                    val ss = getBytes(buffer[i])
                                    bys[i * 2] = ss[0]
                                    bys[i * 2 + 1] = ss[1]
                                }
                                write_data.add(bys)
                            }
                        }
                    }
                }
                isWriting = false

            } catch (t: Throwable) {
                val msg = Message()
                msg.arg1 = -2
                msg.obj = t.message
                callback.handleMessage(msg)
            }

            return null
        }

        override fun onProgressUpdate(vararg values: Any) {
            val time = Date().time
            if (time - c_time >= draw_time) {
                var buf = ArrayList<Short>()
                synchronized(inBuf) {
                    if (inBuf.size == 0)
                        return
                    mDividerX = 0//每次计算需进行清零操作
                    while (inBuf.size > (sfv.width - marginRight) / divider) {
                        inBuf.removeAt(0)
                        mDividerX = mDividerX + 1
                    }
                    buf = inBuf.clone() as ArrayList<Short>// 保存
                }
                SimpleDraw(buf, sfv.height / 2)// 把缓冲区数据画出来
                c_time = Date().time
            }
            super.onProgressUpdate(*values)
        }


        fun getBytes(s: Short): ByteArray {
            var s = s
            val buf = ByteArray(2)
            for (i in buf.indices) {
                buf[i] = (s and 0x00ff).toByte()
                s = (s.toInt() shr 8).toShort()
            }
            return buf
        }

        /**
         * 绘制指定区域
         *
         * @param buf      缓冲区
         * @param baseLine Y轴基线
         */
        fun SimpleDraw(buf: ArrayList<Short>, baseLine: Int) {
            if (!isRecording)
                return
            rateY = 65535 / 2 / (sfv.height - line_off)

            for (i in buf.indices) {
                val bus = getBytes(buf[i])
                buf[i] = (0x0000 or bus[1].toInt() shl 8 or bus[0].toInt()).toShort()//高低位交换
            }
            canvas = sfv.holder.lockCanvas(
                    Rect(0, 0, sfv.width, sfv.height))
            if (canvas == null)
                return
            canvas!!.drawColor(Color.rgb(0, 0, 0))// 清除背景(黑色)
            //            canvas.drawARGB(255, 0, 0, 0);// 清除背景(黑色)


            start = (buf.size * divider).toInt()
            val py = baseLine.toFloat()
            var y: Float

            if (sfv.width - start <= marginRight) {//如果超过预留的右边距距离
                start = sfv.width - marginRight//画的位置x坐标
            }
            canvas!!.drawLine(marginRight.toFloat(), 0f, marginRight.toFloat(), sfv.height.toFloat(), circlePaint!!)//垂直的线


            val height = sfv.height - line_off
            canvas!!.drawLine(0f, (line_off / 2).toFloat(), sfv.width.toFloat(), (line_off / 2).toFloat(), paintLine!!)//最上面的那根线

            //	        mCurrentPosInterface.onCurrentPosChanged(start);

            canvas!!.drawLine(0f, height * 0.5f + line_off / 2, sfv.width.toFloat(), height * 0.5f + line_off / 2, center!!)//中心线
            canvas!!.drawLine(0f, (sfv.height - line_off / 2 - 1).toFloat(), sfv.width.toFloat(), (sfv.height - line_off / 2 - 1).toFloat(), paintLine!!)//最下面的那根线


            val newMarkMap = HashMap<Int, Float>()
            val iterator = markMap.keys.iterator()

            while (iterator.hasNext()) {
                val key = iterator.next()
                val pos = markMap[key]!!
                destRect = Rect((pos - bitWidth / 4).toInt(), bitHeight / 2, (pos - bitWidth / 4).toInt() + bitWidth / 2, bitHeight)
                canvas!!.drawBitmap(markIcon!!, null, destRect!!, null)
                val text = (key + 1).toString() + ""
                val textWidth = markTextPaint!!.measureText(text)
                val fontMetricsInt = markTextPaint!!.fontMetricsInt
                val fontHeight = fontMetricsInt.bottom - fontMetricsInt.top
                canvas!!.drawText(text, pos - textWidth / 2, (bitHeight / 2 + fontHeight).toFloat(), markTextPaint!!)
                canvas!!.drawLine(pos - textWidth / 2 + 5, bitHeight.toFloat(), pos - textWidth / 2 + 5, (sfv.width - bitHeight / 2).toFloat(), bottomHalfPaint!!)
                newMarkMap[key] = pos - mDividerX * divider
            }
            markMap = newMarkMap

            for (i in buf.indices) {
                y = (buf[i] / rateY + baseLine).toFloat()// 调节缩小比例，调节基准线
                var x = i * divider
                if (sfv.width - (i - 1) * divider <= marginRight) {
                    x = (sfv.width - marginRight).toFloat()
                }
                canvas!!.drawLine(x, y, x, sfv.height - y, mPaint)//中间出波形
                if (mCurrentPosInterface != null) {
                    mCurrentPosInterface!!.onCurrentPosChanged(x)//监听波形图的位置
                }
            }
            sfv.holder.unlockCanvasAndPost(canvas)// 解锁画布，提交画好的图像
        }
    }


    /**
     * 添加音频的标记点
     */
    fun addCurrentPostion() {
        markMap[markMap.size] = start.toFloat()
    }


    /**
     * 清除标记位置点
     */
    fun clearMarkPosition() {
        markMap.clear()
    }


    /**
     * 波形线的位置监听
     */
    fun setScrollViewListener(currentPosInterface: CurrentPosInterface) {
        this.mCurrentPosInterface = currentPosInterface
    }


    /**
     * 异步写文件
     *
     * @author cokus
     */
    internal inner class WriteRunnable : Runnable {
        override fun run() {
            try {
                var fos2wav: FileOutputStream? = null
                var file2wav: File? = null
                try {
                    file2wav = File(savePcmPath)
                    if (file2wav.exists()) {
                        file2wav.delete()
                    }
                    fos2wav = FileOutputStream(file2wav)// 建立一个可存取字节的文件
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                while (isWriting || write_data.size > 0) {

                    var buffer: ByteArray? = null
                    synchronized(write_data) {
                        if (write_data.size > 0) {
                            buffer = write_data[0]
                            write_data.removeAt(0)
                        }
                    }
                    try {
                        if (buffer != null) {
                            fos2wav!!.write(buffer)
                            fos2wav.flush()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
                fos2wav!!.close()
                val p2w = Pcm2Wav()//将pcm格式转换成wav 其实就加了一个44字节的头信息
                p2w.convertAudioFiles(savePcmPath!!, saveWavPath!!)
            } catch (t: Throwable) {
            }

        }
    }


}   
