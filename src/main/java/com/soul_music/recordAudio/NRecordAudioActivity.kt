package com.soul_music.recordAudio

import android.Manifest
import android.annotation.TargetApi
import android.app.AppOpsManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.floatview.FloatingMusicService
import com.kotlin_baselib.mvvmbase.BaseViewModelActivity
import com.kotlin_baselib.mvvmbase.EmptyViewModel
import com.kotlin_baselib.utils.*
import com.soul_music.R
import com.soul_music.myInterface.ScrollViewListener
import com.soul_music.utils.AudioTrackPlayer
import com.soul_music.utils.SoundFile
import com.soul_music.view.ObservableScrollView
import com.soul_music.view.WaveCanvas
import kotlinx.android.synthetic.main.activity_nrecord_audio.*
import java.io.File
import java.util.*

@Route(path = Constants.NRECORD_AUDIO_ACTIVITY_PATH)
class NRecordAudioActivity : BaseViewModelActivity<EmptyViewModel>() {

    override fun providerVMClass(): Class<EmptyViewModel>? = EmptyViewModel::class.java

    override fun getResId(): Int {
        return R.layout.activity_nrecord_audio
    }

    private var positions: String = ""
    private var swidth: Int = 0
    private var mDensity: Float = 0f
    private val STATUS_INIT = 0         //初始状态
    private val STATUS_START = 1         //开始状态
    private val STATUS_PAUSE = 2         //暂停状态
    private val STATUS_FINISHED = 3         //重新开始状态

    private var currentStatus: Int = STATUS_INIT
    private var mTimeCounter: Int = 0
    private var waveCanvas: WaveCanvas? = null
    private var recordBufSize: Int = 0
    private var audioRecord: AudioRecord? = null
    private val timeFlag = ArrayList<Int>()//存储的是时间节点
    private val cutPostion_time = ArrayList<Float>()

    private val cut_times = ArrayList<FloatArray>()
    private var currentX1: Int = 0
    private var currentX = 0

    private var isEdit = false

    private val FREQUENCY = 16000// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private val CHANNELCONGIFIGURATION = AudioFormat.CHANNEL_IN_MONO// 设置单声道声道
    private val AUDIOENCODING = AudioFormat.ENCODING_PCM_16BIT// 音频数据格式：每个样本16位
    val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC// 音频获取源

    private val maxRecordTime = 10 * 60 * 60    //10分钟

    private val mFileName = "audio_soul_${DateUtil.parseToString(
        System.currentTimeMillis(),
        DateUtil.yyyyMMddHHmmss
    )}"//文件名
    private var totalTime = 0


    private var mHandler: Handler = Handler {
        when (it.what) {
            1 -> {
                if (mTimeCounter == -1) {
                    tv_record_time.text = "00:00:00"
                    observable_scrollView.scrollTo(0, 0)
                    progress_view_audio.clearPausePoints()//清除标记点
                } else {
                    tv_record_time.text = DateUtil.formatSecond(mTimeCounter / 1000)
                }
            }
            2 -> {
                if (isEdit) {
                    tv_record_time.text = DateUtil.formatSecond(totalTime / 1000)
                    observable_scrollView.scrollTo(currentX, 0)
                    isEdit = false
                } else {
                    observable_scrollView.scrollBy(Dp2PxUtil.dip2px(this, 6.0f), 0)
                }
            }
            3 -> {
                tv_record_time.text = DateUtil.formatSecond(totalTime / 1000)
            }
        }
        true
    }


    override fun initData() {
        setTitle("录音")
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mDensity = metrics.density
        swidth = metrics.widthPixels
        timerCounter.start()
        if (wave_surface_view != null) {
            wave_surface_view.line_off = 0
            //解决surfaceView黑色闪动效果
            wave_surface_view.setZOrderOnTop(true)
            wave_surface_view.getHolder().setFormat(PixelFormat.TRANSLUCENT)
        }
        wave_form_view.line_offset = 0
        progress_view_audio.maxRecordTime = maxRecordTime
        ll_audio_record_progress.setPadding(swidth / 2, 0, swidth / 2, 0)
    }

    override fun initListener() {
        observable_scrollView.setOnTouchListener { v, event -> true }
        observable_scrollView.setScrollViewListener(object : ScrollViewListener {
            override fun onScrollChanged(
                scrollView: ObservableScrollView,
                x: Int,
                y: Int,
                oldx: Int,
                oldy: Int,
                isByUser: Boolean
            ) {
                currentX = x /*获取当前滑动的距离*/
            }
        })
        btn_record_on.setOnClickListener {
            if (PermissionUtils.isGranted(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                if (waveCanvas == null || !waveCanvas!!.isRecording) {  //不在录音状态
                    currentStatus = STATUS_START       //更改为录音开始状态
                    mTimeCounter = 0           //录音时间从0开始
                    wave_surface_view.visibility = View.VISIBLE
                    wave_form_view.visibility = View.INVISIBLE
                    initAudio()
                    btn_record_on.setButtonText("暂停")
                    btn_record_control.setButtonText("完成")
                    btn_record_mark.setButtonText("标记")
                    btn_record_mark.visibility = View.VISIBLE
                    btn_record_control.visibility = View.VISIBLE
                } else {
                    when (currentStatus) {//录制(暂停操作)
                        STATUS_INIT -> {  //初始状态
                        }
                        STATUS_START -> {  //录制过程可对其进行暂停或开始操作
                            currentStatus = STATUS_PAUSE
                            totalTime = mTimeCounter
                            waveCanvas!!.pause()
                            btn_record_on.setButtonText("继续")
                            btn_record_mark.setButtonText("删除")
                            cutPostion_time.add(mTimeCounter * 1.0f / 1000)//记录暂停的时间点
                            progress_view_audio.addPausePoint(currentX.toFloat())

                        }
                        STATUS_PAUSE -> {   //暂停状态
                            currentStatus = STATUS_START
                            mTimeCounter = totalTime
                            waveCanvas!!.reStart()

                            btn_record_on.setButtonText("暂停")
                            btn_record_mark.setButtonText("标记")
                        }
                        STATUS_FINISHED -> {
                            mTimeCounter = -1
                            mHandler.sendEmptyMessage(1)
                            currentStatus = STATUS_START
                            mTimeCounter = 0
                            wave_surface_view.visibility = View.VISIBLE
                            btn_record_control.setButtonText("完成")
                            btn_record_mark.setButtonText("标记")
                            wave_form_view.visibility = View.INVISIBLE

                            initAudio()
                        }
                    }
                }
            } else {
                PermissionUtils.permission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )
                    .callBack(object : PermissionUtils.PermissionCallBack {
                        override fun onGranted(permissionUtils: PermissionUtils) {
                            SnackbarUtil.ShortSnackbar(
                                wave_form_view,
                                "已授权",
                                SnackbarUtil.WARNING
                            ).show()
                        }

                        override fun onDenied(permissionUtils: PermissionUtils) {
                            SnackbarUtil.ShortSnackbar(
                                wave_form_view,
                                "拒绝了权限，将无法使用录音功能",
                                SnackbarUtil.WARNING
                            ).show()
                        }
                    }).request()
            }


        }

        btn_record_mark.setOnClickListener {
            //进行音频的标记操作
            when (currentStatus) {
                STATUS_INIT -> {//初始状态
                    currentStatus = STATUS_INIT
                }
                STATUS_START -> {//录制状态(action 为打标记)
                    currentStatus = STATUS_START
                    timeFlag.add(mTimeCounter)
                    waveCanvas!!.addCurrentPostion()
                }
                STATUS_PAUSE -> {  //暂停状态
                    currentStatus = STATUS_PAUSE
                    //进行音频的回删操作
                    //删除最近的一段
                    val pausePoint = progress_view_audio.pausePoint
                    btn_record_mark.visibility = View.VISIBLE
                    btn_record_on.visibility = View.VISIBLE
                    btn_record_control.visibility = View.VISIBLE
                    delAudio(pausePoint!!)
                }
                STATUS_FINISHED -> {   //录制完成
                    timeFlag.clear()//清除时间点
                    currentStatus = STATUS_INIT
                    wave_surface_view.visibility = View.INVISIBLE
                    btn_record_mark.visibility = View.INVISIBLE
                    btn_record_control.visibility = View.INVISIBLE
                    val mFile1 = File(SdCardUtil.DEFAULT_RECORD_PATH + mFileName + ".wav")
                    val mFile2 = File(SdCardUtil.DEFAULT_RECORD_PATH + mFileName + ".pcm")
                    if (mFile1.exists() && mFile2.exists()) {
                        mFile1.delete()
                        mFile2.delete()
                    }
                    mHandler.sendEmptyMessage(1)
                    wave_surface_view.setVisibility(View.VISIBLE)
                    wave_form_view.setVisibility(View.INVISIBLE)
                }
            }
        }

        btn_record_control.setOnClickListener {
            //录制音频过程中的暂停和重新开始的操作
            //录制完成，音频进行展示界面
            when (currentStatus) {
                STATUS_INIT -> {  //初始状态
                    currentStatus = STATUS_INIT
                }
                STATUS_START, STATUS_PAUSE -> {  //录制状态
                    totalTime = mTimeCounter
                    mTimeCounter = -1
                    currentStatus = STATUS_FINISHED
                    btn_record_mark.visibility = View.GONE
                    btn_record_on.setButtonText("开始")
                    btn_record_control.visibility = View.GONE
                    waveCanvas!!.Stop()
                    waveCanvas!!.clearMarkPosition()//录制完成后需要清除标记点
                    waveCanvas = null
                    initWaveView()

//                    observable_scrollView.scrollTo(0, 0)
//                    progress_view_audio.clearPausePoints()//清除标记点
                    startFloatMusicService()    //开启悬浮窗
                    saveTimeFlag(mFileName) //保存标记点
                }
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
    internal var mPlayer: AudioTrackPlayer? = null
    /**
     * 载入wav文件显示波形
     */
    private fun loadFromFile() {
        try {
            Thread.sleep(300)//让文件写入完成后再载入波形 适当的休眠下
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mFile = File(SdCardUtil.DEFAULT_RECORD_PATH + mFileName + ".wav")
        mLoadingKeepGoing = true
        // 线程加载音频文件
        mLoadSoundFileThread = object : Thread() {
            override fun run() {
                try {
                    mSoundFile = SoundFile.create(mFile.absolutePath, null)
                    if (mSoundFile == null) {
                        return
                    }
                    mPlayer = AudioTrackPlayer(mSoundFile!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }

                if (mLoadingKeepGoing) {
                    val runnable = Runnable {
                        finishOpeningSoundFile()
                    }
                    this@NRecordAudioActivity.runOnUiThread(runnable)
                }
            }

        }
        mLoadSoundFileThread!!.start()
    }


    /**
     * WaveView载入波形完成
     */
    private fun finishOpeningSoundFile() {
        wave_form_view.setSoundFile(mSoundFile!!)
        wave_form_view.recomputeHeights(mDensity)
    }

    /**
     * 初始化AudioRecord
     */
    private fun initAudio() {
        timeFlag.clear()    //清空标志
        recordBufSize = AudioRecord.getMinBufferSize(
            FREQUENCY,
            CHANNELCONGIFIGURATION, AUDIOENCODING
        )//设置录音缓冲区(一般为20ms,1280)
        audioRecord = AudioRecord(
            AUDIO_SOURCE, // 指定音频来源，这里为麦克风
            FREQUENCY, // 16000HZ采样频率
            CHANNELCONGIFIGURATION, // 录制通道
            AUDIO_SOURCE, // 录制编码格式
            recordBufSize
        )

        waveCanvas = WaveCanvas()
        waveCanvas!!.baseLine = wave_surface_view.height / 2    //中间线
        waveCanvas!!.Start(
            audioRecord!!,
            recordBufSize,
            wave_surface_view,
            mFileName,
            SdCardUtil.DEFAULT_RECORD_PATH,
            Handler.Callback { true },
            swidth / 2,
            this
        )
    }

    /**
     * 音频回删操作
     * @param pausePoint
     */
    private fun delAudio(pausePoint: MutableList<Float>) {
        if (pausePoint.size > 1) {
            //多段音频
            val position1 = pausePoint[pausePoint.size - 1]
            val position2 = pausePoint[pausePoint.size - 2]
            currentX1 = (position2 - position1).toInt()
            currentX = position2.toInt()
            val cut_time = FloatArray(2)
            cut_time[1] = cutPostion_time[cutPostion_time.size - 1]
            cut_time[0] = cutPostion_time[cutPostion_time.size - 2]
            totalTime = (cut_time[0] * 1000).toInt()


            //删除区段标记的时间点
            if (timeFlag.size > 0) {//注意角标越界
                var timeFlagSize = timeFlag.size - 1
                while (timeFlagSize >= 0 && totalTime <= timeFlag[timeFlagSize]) {
                    timeFlag.removeAt(timeFlagSize)
                    timeFlagSize = timeFlag.size - 1
                }
            }
            cut_times.add(cut_time)
            cutPostion_time.removeAt(cutPostion_time.size - 1)//删除最后一个时间点
            pausePoint.removeAt(pausePoint.size - 1)//删除最后标记点
            progress_view_audio.pausePoint = pausePoint

        } else {
            //只有一段音频
            val position1 = pausePoint[pausePoint.size - 1]
            val cut_time = FloatArray(2)
            cut_time[1] = cutPostion_time[cutPostion_time.size - 1]
            cut_time[0] = 0f
            currentX = (0 - position1).toInt()
            totalTime = (cut_time[0] * 1000).toInt()
            cut_times.add(cut_time)
            pausePoint.removeAt(pausePoint.size - 1)//删除最后标记点
            progress_view_audio.pausePoint = pausePoint
            timeFlag.clear()//删除最后一段，需删除所标记时间点
            //所有状态归0
            totalTime = 0
            mTimeCounter = -1
            currentStatus = 0
            waveCanvas!!.Stop()
            waveCanvas!!.clear()//清除数据
            waveCanvas!!.clearMarkPosition()//录制完成后需要清除标记点
            wave_surface_view.visibility = View.INVISIBLE
            wave_form_view.visibility = View.INVISIBLE
            //最后一段删除后，从新开始录制
            btn_record_mark.visibility = View.GONE
            btn_record_on.setButtonText("开始")
            btn_record_control.visibility = View.GONE


            val mFile1 = File(SdCardUtil.DEFAULT_RECORD_PATH + mFileName + ".wav")
            val mFile2 = File(SdCardUtil.DEFAULT_RECORD_PATH + mFileName + ".pcm")
            if (mFile1.exists() && mFile2.exists()) {
                mFile1.delete()
                mFile2.delete()
            }
            cut_times.clear()//当最后一段删除的时候，重新录制，不需要进行合并操作
            cutPostion_time.clear()//删除暂停计时点
        }

        wave_surface_view.visibility = View.VISIBLE
        wave_form_view.visibility = View.INVISIBLE
        isEdit = true
        mHandler.sendEmptyMessage(3)
        mHandler.sendEmptyMessage(2)
    }

    private var timer_speed: Timer? = null
    private val timerCounter = Thread(Runnable {
        try {
            val timerTask_speed = object : TimerTask() {
                override fun run() {
                    if (mTimeCounter != -1 && currentStatus == STATUS_START) {
                        mTimeCounter = mTimeCounter + 100
                        mHandler.sendEmptyMessage(1)
                        mHandler.sendEmptyMessage(2)
                    }
                }
            }
            if (timer_speed == null) {
                timer_speed = Timer()
            }
            timer_speed!!.schedule(timerTask_speed, 0, 100)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    })

/*
    override fun onPause() {

        super.onPause()
    }
*/

    override fun onDestroy() {
        if (timer_speed != null) {
            timer_speed!!.cancel()
            timer_speed = null
        }

        if (waveCanvas != null) {
            waveCanvas!!.Stop()
            waveCanvas!!.clear()
            waveCanvas = null
        }
        super.onDestroy()
    }

    /**
     * 开启播放录音悬浮窗口服务
     */
    fun startFloatMusicService() {
        if (FloatingMusicService.isStarted) {
            return
        }
        if (checkFloatWindowPermission(mContext)) {  //有悬浮窗权限，直接开启悬浮窗
            startFloatMusicService(mFileName + ".pcm")
        } else {  //没有悬浮窗权限
            AlertDialogUtil.getInstance(mContext).showAlertDialog("播放录音界面需要悬浮窗权限，请授权", "取消", "授权",
                DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                },
                DialogInterface.OnClickListener { dialog, which ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //6.0以上
                        startActivityForResult(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            ), 1002
                        )
                    } else {        //6.0以下
                        val intent = Intent()
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
                        intent.setData(Uri.fromParts("package", getPackageName(), null))
                        startActivityForResult(intent, 1002)
                    }
                })
        }
    }

    /**
     * 开启播放录音悬浮窗口服务
     */
    private fun startFloatMusicService(recordFileName: String) {
        val intent = Intent(this, FloatingMusicService::class.java)
        intent.putExtra("fileName", recordFileName)
        startService(intent)
    }

    /**
     * 检测悬浮窗权限
     */
    fun checkFloatWindowPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {       //6.0以上
            return Settings.canDrawOverlays(this)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//4.4-5.1)
            return checkOp(context, 24)  //OP_SYSTEM_ALERT_WINDOW = 24;
        } else {  //低于4.4
            return true
        }
    }

    /**
     * android 4.4-5.1 的悬浮窗权限
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun checkOp(context: Context, op: Int): Boolean {
        val version = Build.VERSION.SDK_INT
        if (version >= 19) {
            val manager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            try {
                val clazz = AppOpsManager::class.java
                val method = clazz.getDeclaredMethod(
                    "checkOp",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                return AppOpsManager.MODE_ALLOWED == method.invoke(
                    manager,
                    op,
                    Binder.getCallingUid(),
                    context.getPackageName()
                ) as Int
            } catch (e: Exception) {
                Log.e(Constants.DEBUG_TAG, Log.getStackTraceString(e))
            }

        } else {
            Log.e(Constants.DEBUG_TAG, "Below API 19 cannot invoke!")
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1002 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {       //6.0以上
                    if (!Settings.canDrawOverlays(this)) {  //拒绝授权
                        SnackbarUtil.ShortSnackbar(
                            wave_form_view,
                            "悬浮窗口权限未授权，无法使用播放功能",
                            SnackbarUtil.WARNING
                        ).show()
                    } else {
                        startFloatMusicService(mFileName + ".pcm")
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//4.4-5.1)
                    if (checkOp(mContext, 24)) {  //OP_SYSTEM_ALERT_WINDOW = 24;
                        startFloatMusicService(mFileName + ".pcm")
                    } else {//拒绝授权
                        SnackbarUtil.ShortSnackbar(
                            wave_form_view,
                            "悬浮窗口权限未授权，无法使用播放功能",
                            SnackbarUtil.WARNING
                        ).show()
                    }
                }
            }
        }
    }


    /**
     * 保存打标记的点
     * @param name SP .xml
     */
    private fun saveTimeFlag(name: String) {

        val sp = getSharedPreferences(name, Context.MODE_PRIVATE)
        if (timeFlag.size > 0) {
            for (i in timeFlag.indices) {
                if (i != timeFlag.size - 1) {
                    positions = positions + timeFlag[i] + ","
                } else {
                    positions = positions + timeFlag[i]
                }
            }
            sp.edit().apply {
                putString("flags", positions)
                putInt("size", timeFlag.size)
                apply()
            }

        }


    }

}
