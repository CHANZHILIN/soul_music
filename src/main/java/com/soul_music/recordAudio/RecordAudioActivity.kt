package com.soul_music.recordAudio

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import com.alibaba.android.arouter.facade.annotation.Route
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.base.BaseActivity
import com.kotlin_baselib.base.EmptyModelImpl
import com.kotlin_baselib.base.EmptyPresenterImpl
import com.kotlin_baselib.base.EmptyView
import com.kotlin_baselib.floatview.FloatingMusicService
import com.kotlin_baselib.utils.*
import com.soul_music.R
import kotlinx.android.synthetic.main.activity_record_audio.*
import java.io.*
import java.util.*


@Route(path = Constants.RECORD_AUDIO_ACTIVITY_PATH)
class RecordAudioActivity : BaseActivity<EmptyView, EmptyModelImpl, EmptyPresenterImpl>(), EmptyView {

    private var isRecording: Boolean = false;
    private var audioSource: Int = 0
    private var frequency: Int = 0
    private var channelConfig: Int = 0
    private var audioFormat: Int = 0
    private var recordBufSize: Int = 0
    private lateinit var audioRecord: AudioRecord
    private lateinit var parent: File
//    private lateinit var mLock: Object


    private lateinit var fileName: String

    private var currentRecordMilliSeconds: Long = 0//当前录音的毫秒数

    override fun createPresenter(): EmptyPresenterImpl {
        return EmptyPresenterImpl(this)
    }

    override fun getResId(): Int {
        return R.layout.activity_record_audio
    }

    override fun initData() {
        mTimer.schedule(taskOne, 0, 1000)

    }

    override fun initListener() {
        record_audio.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecord()
                }
                MotionEvent.ACTION_UP -> {
                    pauseRecord()
                }
            }
            false
        }

    }


    /**
     * 初始化和配置AudioRecord
     */
    private fun initAudio() {
//        mLock = Object()
        //指定音频源
        audioSource = MediaRecorder.AudioSource.MIC
        //指定采样率(MediaRecoder 的采样率通常是8000Hz CD的通常是44100Hz 不同的Android手机硬件将能够以不同的采样率进行采样。其中11025是一个常见的采样率)
        frequency = 44100
        //指定捕获音频的通道数目.在AudioFormat类中指定用于此的常量
        channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO
        //指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT PCM代表的是脉冲编码调制，它实际上是原始音频样本。
        //因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
        audioFormat = AudioFormat.ENCODING_PCM_16BIT
        //设置缓存buffer
        recordBufSize = AudioRecord.getMinBufferSize(frequency, channelConfig, audioFormat)
        //构建AudioRecord对象
        audioRecord = AudioRecord(audioSource, frequency, channelConfig, audioFormat, recordBufSize)
        //构建存放音频文件的文件夹
        parent = SdCardUtil.recordDir
    }


    /**
     * 开始录音
     */
    private fun startRecord() {

        PermissionUtils.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.SYSTEM_ALERT_WINDOW)
                .callBack(object : PermissionUtils.PermissionCallBack {
                    override fun onGranted(permissionUtils: PermissionUtils) {
                        //            fileName = "soul_audio_" + DateUtil.parseToString(System.currentTimeMillis(), DateUtil.yyyyMMddHHmmss) + ".pcm"
                        fileName = "soul_audio.pcm"
                        getAudio(fileName)
                    }

                    override fun onDenied(permissionUtils: PermissionUtils) {
                        SnackbarUtil.ShortSnackbar(
                                window.decorView,
                                "拒绝了权限，将无法使用部分功能",
                                SnackbarUtil.WARNING
                        ).show()
                    }
                }).request()

    }

    private fun getAudio(fileName: String) {
        initAudio()
        isRecording = true
        tv_record_duration.setText(DateUtil.getFormatHMS(currentRecordMilliSeconds))
        object : Thread() {
            override fun run() {
                super.run()
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                //生成的文件名
                val file = File(parent, fileName)
                if (file.exists()) {
                    file.delete()
                }
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val outputStream: DataOutputStream
                try {
                    outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
                    val buffer = ByteArray(recordBufSize)
                    //开始录音
                    audioRecord.startRecording()
                    var r = 0
                    while (isRecording) {
                        val readResult = audioRecord.read(buffer, 0, recordBufSize)
                        var sumVolume = 0.0
                        try {
                            for (i in 0 until readResult) {
                                //数据写入文件中
                                outputStream.write(buffer[i].toInt())
                                sumVolume += Math.abs(buffer[i].toDouble());
                            }

                            /*   // 大概一秒5次
                               synchronized(mLock) {

                                   mLock.wait(210);
                                   for (i in 0 until readResult) {
                                       sumVolume += Math.abs(buffer[i].toDouble());
                                   }
                               }*/

                            // 平方和除以数据总长度，得到音量大小。
                            val avgVolume = sumVolume / readResult
                            val volume = 10 * Math.log10(1 + avgVolume)
                            runOnUiThread(Runnable { record_line_view.setMaxHeight(volume * 3) })
                            r++
                            Log.e(Constants.DEBUG_TAG, "pcm录制中...")
                        } catch (e: InterruptedException) {
                            e.printStackTrace();
                        }
                    }

                    //录制完之后，释放AudioRecord
                    audioRecord.stop()
                    audioRecord.release()
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }.start()
    }


    /**
     * 暂停录音
     */
    private fun pauseRecord() {
        isRecording = false
        currentRecordMilliSeconds = 0
        Log.e(Constants.DEBUG_TAG, "录制完成...")
        record_line_view.setMaxHeight(0.0)
        startFloatMusicService()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        currentRecordMilliSeconds = 0
        taskOne.cancel()
        mTimer.cancel()
    }


    var mTimer: Timer = Timer()
    /**
     * 计时器
     */
    val taskOne: TimerTask = object : TimerTask() {

        override fun run() {
            if (isRecording) {
                currentRecordMilliSeconds += 1000
                Log.e(Constants.DEBUG_TAG, currentRecordMilliSeconds.toString())
                Log.e(Constants.DEBUG_TAG, DateUtil.getFormatHMS(currentRecordMilliSeconds))
                runOnUiThread { tv_record_duration.setText(DateUtil.getFormatHMS(currentRecordMilliSeconds)) }
            }
        }
    }


    fun startFloatMusicService() {
        if (FloatingMusicService.isStarted) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialogUtil.getInstance(mContext).showAlertDialog("播放录音界面需要悬浮窗权限，请授权", "取消", "授权",
                        DialogInterface.OnClickListener { dialog, which ->
                            dialog.dismiss()
                        },
                        DialogInterface.OnClickListener { dialog, which ->
                            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), 1002)
                        })

            } else {
                startFloatMusicService(fileName)
            }
        } else {
            startFloatMusicService(fileName)
        }
    }

    fun startFloatMusicService(recordFileName: String) {
        val intent = Intent(this, FloatingMusicService::class.java)
        intent.putExtra("fileName", recordFileName)
        startService(intent)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            1002 -> {
                val intent = Intent(this, FloatingMusicService::class.java)
                intent.putExtra("fileName", fileName)
                startService(intent)
            }
        }
    }

}
