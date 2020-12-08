package com.soul_music.editAudio

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.base.BaseToolbarViewModelActivity
import com.kotlin_baselib.base.EmptyViewModel
import com.kotlin_baselib.utils.*
import com.soul_music.R
import com.soul_music.myInterface.ScrollViewListener
import com.soul_music.utils.AudioTrackPlayer
import com.soul_music.utils.AudioUtils
import com.soul_music.utils.CheapWAV
import com.soul_music.utils.SoundFile
import com.soul_music.view.ObservableScrollView
import kotlinx.android.synthetic.main.activity_edit_audio.*
import java.io.File
import java.util.*

@Route(path = Constants.EDIT_AUDIO_ACTIVITY_PATH)
class EditAudioActivity : BaseToolbarViewModelActivity<EmptyViewModel>(), ScrollViewListener {

    override fun providerVMClass(): Class<EmptyViewModel>? = EmptyViewModel::class.java

    override fun setToolbarTitle(): String? = "编辑音频"

    override fun getResId(): Int {
        return R.layout.activity_edit_audio
    }


    private var positions: String = ""
    private var isEdit: Boolean = false         //是否编辑状态
    private var totalTime: Int = 60
    private var mDensity: Float = 0f

    @JvmField
    @Autowired(name = "fileName")
    var mFilename: String? = null

    private var filename: String? = null

    private var width: Int = 0
    private var height: Int = 0

    private var mPlayer: MediaPlayer? = null

    private var currentPosition = 0


    private var clipPosition: MutableList<FloatArray>? = null
    private var clipPosition_temp = ArrayList<LongArray>()

    private val clipPosition_use = ArrayList<LongArray>()
    private val clipPosition_use1 = ArrayList<LongArray>()

    private var flagPositions: MutableList<Int> = ArrayList()           //标记点

    private var flags: String? = null
    private var flagsPositions: Array<String?>? = null
    private var flagsPositions_sub: Array<String?>? = null

    private var myHandler: Handler = Handler {
        when (it.what) {
            1 -> {
                try {
                    hlv_scroll.scrollTo(
                        totalLength * mPlayer!!.getCurrentPosition() / mPlayer!!.getDuration(),
                        0
                    )
                    hlv_scroll1.scrollTo(
                        totalLength * mPlayer!!.getCurrentPosition() / mPlayer!!.getDuration(),
                        0
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            4 -> {
                totalTime = waveview.pixelsToMillisecsTotal() / 1000
                waveview1.setFlag(flagPositions)
                timeSize()
            }
            10 -> {
                SnackBarUtil.shortSnackBar(btn_edit_audio_clip, "编辑成功！", SnackBarUtil.CONFIRM)
                    .show()
//                val absolutePath = outFile?.getAbsolutePath()
                val file_pcm = File(outFile?.getAbsolutePath()?.replace(".wav", ".pcm"))
                if (outFile!!.exists() && file_pcm.exists()) {
                    val newNameFile_wav = File(SdCardUtil.DEFAULT_RECORD_PATH + filename + ".wav")
                    val newNameFile_pcm = File(SdCardUtil.DEFAULT_RECORD_PATH + filename + ".pcm")
                    outFile?.renameTo(newNameFile_wav)
                    file_pcm.renameTo(newNameFile_pcm)

                }
                waveview1.setFlag(flagPositions)
                //需要从新加载界面
//					mFilename=outFile.getAbsolutePath();
                initWaveView()
                waveview.clearCutPoint()
            }
            100 -> { //删除所有
                deleteAll() //切割段全选，则退出当前界面或者取消停留在本页面
            }
        }
        true
    }

    /**
     * 删除所有音频
     */
    private fun deleteAll() {
        AlertDialogUtil.showAlertDialog(mContext, "确定删除所有音频吗？", "取消", "确认",
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            },
            DialogInterface.OnClickListener { dialog, which ->
                finish()
            })
    }

    override fun initData() {
        ARouter.getInstance().inject(this)
        setTitle("编辑")
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        width = metrics.widthPixels
        height = metrics.heightPixels

        mDensity = metrics.density

        ll_wave_content.setPadding(
            width / 2 - Dp2PxUtil.dip2px(this, 10f),
            0,
            width / 2 - Dp2PxUtil.dip2px(this, 10f),
            0
        )
        ll_wave_content1.setPadding(
            width / 2 - Dp2PxUtil.dip2px(this, 10f),
            0,
            width / 2 - Dp2PxUtil.dip2px(this, 10f),
            0
        )

        filename = mFilename?.replace(".pcm", "")

        flags = getSharedPreferences(filename, Context.MODE_PRIVATE).getString("flags", null)

        if (flags != null) {    //标记点
            flagsPositions =
                flags!!.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            for (i in flagsPositions!!.indices) {
                flagPositions.add(Integer.valueOf(flagsPositions!![i]!!))
            }
        }

        timeSize()

        if (wavesfv != null && wavesfv1 != null) {
            wavesfv.line_off = 42
            //解决surfaceView黑色闪动效果
            wavesfv.setZOrderOnTop(true)
            wavesfv.getHolder().setFormat(PixelFormat.TRANSLUCENT)
            wavesfv1.line_off = 42
            //解决surfaceView黑色闪动效果
            wavesfv1.setZOrderOnTop(true)
            wavesfv1.getHolder().setFormat(PixelFormat.TRANSLUCENT)
        }
        waveview.line_offset = 42
        waveview1.line_offset = 42
        initWaveView()
        timerCounter.start()
    }

    override fun initListener() {
        iv_play.setOnClickListener {
            startPlay()
        }
        hlv_scroll.setScrollViewListener(this)
        hlv_scroll1.setScrollViewListener(this)
        waveview.setOnClickListener {
            waveview.showSelectArea(false)  //设置点击选择裁剪区域
        }
        btn_edit_audio_clip.setOnClickListener {
            setCurrentPosition()    //设置裁剪的线
        }
        btn_edit_audio_delete.setOnClickListener {
            clipAudio()
        }
        btn_edit_audio_done.setOnClickListener {
            saveAs()
        }
    }

    /**
     * 另存为
     */
    private fun saveAs() {
        val dialog = AlertDialog.Builder(mContext).setTitle("另存为").setMessage("请输入保存的名称：")
        val editText = EditText(mContext)
        dialog.setView(editText)
        dialog.setPositiveButton("保存", DialogInterface.OnClickListener { dialog1, which ->
            val saveName = editText.text.toString().trim()
            if (TextUtils.isEmpty(saveName)) {
                SnackBarUtil.shortSnackBar(btn_edit_audio_clip, "音频文件名称不能为空！", SnackBarUtil.WARNING)
                    .show()
                return@OnClickListener
            }
            saveAs(saveName)
            dialog1.dismiss()
            SnackBarUtil.shortSnackBar(btn_edit_audio_clip, "保存成功", SnackBarUtil.CONFIRM).show()
            Handler().postDelayed({ finish() }, 2000)

        })
            .setNegativeButton("取消") { dialog1, which -> dialog1.dismiss() }.show()
    }

    /**
     * 另存为--fileName
     */
    private fun saveAs(newFileName: String) {
        val oldWav = File(SdCardUtil.DEFAULT_RECORD_PATH + filename + ".wav")
        val newNameFile_wav: File?
        if (oldWav.exists()) {
            newNameFile_wav = File(SdCardUtil.DEFAULT_RECORD_PATH + newFileName + ".wav")
            oldWav.renameTo(newNameFile_wav)    //更换名字
        } else {
            SnackBarUtil.shortSnackBar(btn_edit_audio_clip, "你操作的文件不存在！", SnackBarUtil.WARNING)
                .show()
        }
        //保存视频录制的总时长
        setInfor(newFileName)
        saveTimeFlag(newFileName)

    }


    private fun setInfor(newFileName: String) {
        val sp = getSharedPreferences(newFileName + "wav", Context.MODE_PRIVATE)
        val totalTime = waveview.pixelsToMillisecsTotal()
        sp.edit().apply {
            putInt("total_time", totalTime)
            apply()
        }
    }


    /**
     * 保存标记点
     */
    private fun saveTimeFlag(newFileName: String) {
        val sp = getSharedPreferences(newFileName, Context.MODE_PRIVATE)

        if (flagPositions.size > 0) {
            for (i in flagPositions.indices) {
                if (flagPositions[i] != 0) {
                    if (i != flagPositions.size - 1) {
                        positions = positions + flagPositions[i] + ","
                    } else {
                        positions = positions + flagPositions[i]
                    }
                }
            }
            sp.edit().apply {
                clear()
                putString("flags", positions)
                putInt("size", flagPositions.size)
                apply()
            }

        }
    }

    /**
     * 裁剪音频
     */
    private fun clipAudio() {
        clipPosition = waveview.clipPosition
        if (null != clipPosition && (clipPosition as MutableList<FloatArray>).size > 0) {
            onSaveClipFile()
        } else {
            SnackBarUtil.shortSnackBar(btn_edit_audio_clip, "请在下方选择要删除的音频段!", SnackBarUtil.ALERT)
                .show()
        }

    }

    private var outFile: File? = null
    private val cutPaths = ArrayList<String>()

    /**
     * 保存裁剪的音频文件
     */
    private fun onSaveClipFile() {
        isEdit = true
        val outPath = SdCardUtil.DEFAULT_RECORD_PATH + "temp_" + filename + ".wav"
        clipPosition_temp.clear()
        for (i in clipPosition!!.indices) {
            val temp_fs = LongArray(2)
            val fs = clipPosition!!.get(i)
            val pixelsToMillisecsTotal = waveview.pixelsToMillisecsTotal()

            val start = (fs[0] * pixelsToMillisecsTotal / totalLength.toFloat() / 1000f).toDouble()
            val end = (fs[1] * pixelsToMillisecsTotal / totalLength.toFloat() / 1000f).toDouble()

            temp_fs[0] = waveview.secondsToFrames(start).toLong()
            temp_fs[1] = waveview.secondsToFrames(end).toLong()
            clipPosition_temp.add(temp_fs)
        }

        //start
        if (flagsPositions != null) {
            val flagPositions_sub = ArrayList<Int>()
            //倒序遍历集合
            for (i in clipPosition!!.indices.reversed()) {
                val fs = clipPosition!!.get(i)
                val pixelsToMillisecsTotal = waveview.pixelsToMillisecsTotal()
                //最后的编辑区间
                val start =
                    (fs[0] * pixelsToMillisecsTotal / totalLength.toFloat() / 1000f).toDouble()
                val end =
                    (fs[1] * pixelsToMillisecsTotal / totalLength.toFloat() / 1000f).toDouble()
                //清除删除区域的标记点
                for (j in flagsPositions!!.indices.reversed()) {//必须保证每个元素都要遍历的到
                    var temp = false
                    val pos = Integer.valueOf(flagsPositions!![j]!!) / 1000
                    if (pos <= end && pos >= start) {
                        flagPositions.set(j, 0)
                        temp = true
                    }

                    if (pos > end && !temp) {//在删除区间的右侧（需进行相应时间点的操作运算）
                        flagPositions.set(j, (flagPositions.get(j) - (end - start) * 1000).toInt())
                    }

                }
            }

            for (i in flagPositions.indices) {
                if (flagPositions.get(i) != 0) {
                    flagPositions_sub.add(flagPositions.get(i))
                }
            }
            flagPositions = flagPositions_sub

            flagsPositions_sub = arrayOfNulls<String>(flagPositions.size)

            for (i in flagPositions.indices) {
                flagsPositions_sub!![i] = flagPositions.get(i).toString()
            }
            flagsPositions = flagsPositions_sub
        }

        showLoading()

        object : Thread() {
            override fun run() {
                outFile = File(outPath)
                try {
                    val a = CheapWAV()
                    a.ReadFile(File(SdCardUtil.DEFAULT_RECORD_PATH + filename + ".wav"))
                    val numFrames = a.getNumFrames()//获取音频文件总帧数
                    clipPosition_use.clear()

                    //头部开始计算
                    val lg_f = LongArray(2)
                    lg_f[0] = 0
                    lg_f[1] = 0
                    clipPosition_use.add(lg_f)
                    //添加选中的区间
                    for (i in clipPosition_temp.indices) {
                        clipPosition_use.add(clipPosition_temp.get(i))
                    }
                    //最后的区间
                    val lg_e = LongArray(2)
                    lg_e[0] = numFrames.toLong()
                    lg_e[1] = numFrames.toLong()
                    clipPosition_use.add(lg_e)

                    clipPosition_use1.clear()
                    for (i in clipPosition_use.indices) {
                        if (i + 1 < clipPosition_use.size) {
                            //不超边界
                            if (clipPosition_use.get(i + 1)[0] - clipPosition_use.get(i)[1] != 0L) {
                                //所取区域的帧数不能为0
                                val lon = LongArray(2)
                                lon[0] = clipPosition_use.get(i)[1]
                                lon[1] = clipPosition_use.get(i + 1)[0]
                                clipPosition_use1.add(lon)
                            }
                        }
                    }

                    if (clipPosition_use1.size == 0) {
                        //全部删除
                        myHandler.sendEmptyMessage(100)
                        return
                    }


                    val out = File(SdCardUtil.DEFAULT_RECORD_PATH + "/clip_files/")
                    if (!out.exists()) {
                        out.mkdirs()
                    }
                    cutPaths.clear()
                    for (i in clipPosition_use1.indices) {
                        val outputFile =
                            File(SdCardUtil.DEFAULT_RECORD_PATH + "/clip_files/" + "clip_" + i + ".wav")
                        cutPaths.add(outputFile.getAbsolutePath())
                        a.WriteFile(
                            outputFile, clipPosition_use1.get(i)[0].toInt(),
                            (clipPosition_use1.get(i)[1] - clipPosition_use1.get(i)[0]).toInt()
                        )
                    }

                    val file = File(SdCardUtil.DEFAULT_RECORD_PATH + filename + ".wav")
                    if (file.exists()) {
                        file.delete()
                    }

                    //合并剪贴的片段文件
                    if (cutPaths.size > 0) {
                        AudioUtils.mergeAudioFiles(
                            SdCardUtil.DEFAULT_RECORD_PATH + filename + ".wav",
                            cutPaths
                        )
                    }

                    //遍历删除临时文件
                    for (i in cutPaths.indices) {
                        val f = File(cutPaths.get(i))
                        if (f.exists()) {
                            f.delete()
                        }
                    }
                    //删除文件夹
                    out.delete()
                    cutPaths.clear()
                    myHandler.sendEmptyMessage(10)

                } catch (e: Exception) {
                    runOnUiThread { hideLoading() }
                    e.printStackTrace()
                }
                runOnUiThread { hideLoading() }

            }
        }.start()


    }

    /**
     * 设置断点位置
     */
    private fun setCurrentPosition() {
        waveview.setCutPostion(currentPosition)
    }

    override fun onScrollChanged(
        scrollView: ObservableScrollView,
        x: Int,
        y: Int,
        oldx: Int,
        oldy: Int,
        isByUser: Boolean
    ) {
        waveview.showSelectArea(true)
        currentPosition = x

        hlv_scroll.scrollTo(x, 0)
        hlv_scroll1.scrollTo(x, 0)
    }


    /**
     * 开始播放音频文件
     */
    protected fun startPlay() {

        if (mPlayer != null && mPlayer!!.isPlaying()) {
            iv_play.setButtonText("播放")
            mPlayer!!.pause()
            mTimeCounter = -1
        } else {
            iv_play.setButtonText("暂停")
            if (mPlayer == null) {
                hlv_scroll.scrollTo(0, 0)
                hlv_scroll1.scrollTo(0, 0)
                try {
                    mPlayer = MediaPlayer()
                    mPlayer!!.setDataSource(mFile.getAbsolutePath())
                    mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mPlayer!!.prepare()
                    mPlayer!!.start()
                    totalTime = mPlayer!!.getDuration()
                    mTimeCounter = 0
                    mPlayer!!.setOnCompletionListener {
                        iv_play.setButtonText("播放")
                        hlv_scroll.scrollTo(totalLength, 0)
                        hlv_scroll1.scrollTo(totalLength, 0)
                        mTimeCounter = -1
                        mPlayer = null
                    }
                } catch (e: java.io.IOException) {
                    SnackBarUtil.shortSnackBar(btn_edit_audio_clip, "文件播放出错！", SnackBarUtil.WARNING)
                        .show()
//                    Toast.makeText(this, "文件播放出错！", Toast.LENGTH_SHORT).show()
                }

            } else {
                val start = currentPosition * waveview.pixelsToMillisecsTotal() / totalLength
                mTimeCounter = 0
                mPlayer!!.seekTo(start)
                mPlayer!!.start()
            }
        }
    }

    private fun initWaveView() {
        loadFromFile()
    }

    private lateinit var mFile: File
    internal var mLoadingKeepGoing: Boolean = false
    internal var mLoadSoundFileThread: Thread? = null
    internal var mSoundFile: SoundFile? = null
    internal var audioTrackPlayer: AudioTrackPlayer? = null

    /**
     * 载入wav文件显示波形
     */
    private fun loadFromFile() {
        try {
            Thread.sleep(300)//让文件写入完成后再载入波形 适当的休眠下
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mFile = File(SdCardUtil.DEFAULT_RECORD_PATH + filename + ".wav")
        mLoadingKeepGoing = true
        // 线程加载音频文件
        mLoadSoundFileThread = object : Thread() {
            override fun run() {
                try {
                    mSoundFile = SoundFile.create(mFile.absolutePath, null)
                    if (mSoundFile == null) {
                        return
                    }
                    audioTrackPlayer = AudioTrackPlayer(mSoundFile!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }

                if (mLoadingKeepGoing) {
                    val runnable = Runnable {
                        finishOpeningSoundFile()
                    }
                    this@EditAudioActivity.runOnUiThread(runnable)
                }
            }

        }
        mLoadSoundFileThread!!.start()
    }

    /**
     * WaveView载入波形完成
     */
    private fun finishOpeningSoundFile() {
        waveview1.setSoundFile(mSoundFile!!)
        waveview1.recomputeHeights(mDensity)


        waveview.setSoundFile(mSoundFile!!)
        waveview.recomputeHeights(mDensity)
        myHandler.sendEmptyMessage(4)
    }


    private var totalLength: Int = 0

    /**
     * 添加时间线
     */
    fun timeSize() {
        tv_total_time.text = DateUtil.formatSecond(totalTime)
        ll_time_counter.removeAllViews()
        totalLength = totalTime * Dp2PxUtil.dip2px(this, 60f)
        ll_wave_content.layoutParams =
            FrameLayout.LayoutParams(totalLength, FrameLayout.LayoutParams.MATCH_PARENT)
        ll_wave_content1.layoutParams =
            FrameLayout.LayoutParams(totalLength, FrameLayout.LayoutParams.MATCH_PARENT)
        ll_time_counter1.layoutParams =
            RelativeLayout.LayoutParams(totalLength, RelativeLayout.LayoutParams.MATCH_PARENT)
        for (i in 0 until totalTime) {
            val line1 = LinearLayout(this)
            line1.orientation = LinearLayout.HORIZONTAL
            line1.layoutParams = LinearLayout.LayoutParams(
                Dp2PxUtil.dip2px(this, 60f),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            line1.gravity = Gravity.CENTER

            val timeText = TextView(this)
            timeText.setText(DateUtil.formatSecond(i))
            timeText.width = Dp2PxUtil.dip2px(this, 60f) - 2
            timeText.gravity = Gravity.CENTER_HORIZONTAL
            val paint = timeText.paint
            paint.isFakeBoldText = true //字体加粗设置
            timeText.setTextColor(Color.rgb(204, 204, 204))
            val line2 = View(this)
            line2.setBackgroundColor(Color.rgb(204, 204, 204))
            line2.setPadding(0, 10, 0, 0)
            line1.addView(timeText)
            line1.addView(line2)
            ll_time_counter.addView(line1)
        }
    }

    private var mTimeCounter = -1
    private var timer_speed: Timer? = null
    private val timerCounter = Thread(Runnable {
        try {
            val timerTask_speed = object : TimerTask() {
                override fun run() {
                    if (mTimeCounter != -1) {
                        mTimeCounter = mTimeCounter + 1
                        myHandler.sendEmptyMessage(1)
                    }
                }
            }
            if (timer_speed == null) {
                timer_speed = Timer()
            }
            timer_speed!!.schedule(timerTask_speed, 0, 10)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    })


}
