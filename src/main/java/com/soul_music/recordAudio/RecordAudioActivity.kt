package com.soul_music.recordAudio

import android.Manifest
import android.annotation.TargetApi
import android.app.AppOpsManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.audio.AudioRecordManager
import com.kotlin_baselib.floatview.FloatingMusicService
import com.kotlin_baselib.base.BaseViewModelActivity
import com.kotlin_baselib.base.EmptyViewModel
import com.kotlin_baselib.utils.AlertDialogUtil
import com.kotlin_baselib.utils.PermissionUtils
import com.kotlin_baselib.utils.SnackbarUtil
import com.soul_music.R
import kotlinx.android.synthetic.main.activity_record_audio.*


@Route(path = Constants.RECORD_AUDIO_ACTIVITY_PATH)
class RecordAudioActivity : BaseViewModelActivity<EmptyViewModel>() {

    override fun providerVMClass(): Class<EmptyViewModel>? = EmptyViewModel::class.java

    private lateinit var fileName: String


    override fun preSetContentView() {
        super.preSetContentView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    override fun getResId(): Int {
        return R.layout.activity_record_audio
    }

    override fun initData() {

    }

    override fun initListener() {
        record_audio.setOnLongClickListener {
            startRecord()
            true
        }
        record_audio.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    AudioRecordManager.getInstance().stopRecord()
                }
            }
            false
        }

    }


    /**
     * 开始录音
     */
    private fun startRecord() {
        if (PermissionUtils.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)) {
            //            fileName = "soul_audio_" + DateUtil.parseToString(System.currentTimeMillis(), DateUtil.yyyyMMddHHmmss) + ".pcm"
            fileName = "soul_audio.pcm"
            AudioRecordManager.getInstance().startRecord(fileName)
            AudioRecordManager.getInstance().setOnRecordStatusChangeListener(object : AudioRecordManager.onRecordStatusChange {
                override fun onRecordStart() {
                }

                override fun onVolume(volume: Double) {
                    runOnUiThread { record_line_view.setMaxHeight(volume * 3) }

                }

                override fun onRecording(time: String) {
                    runOnUiThread { tv_record_duration.setText(time) }

                }

                override fun onRecordStop() {
                    runOnUiThread { startFloatMusicService() }

                }
            })
        } else {
            PermissionUtils.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO/*,Manifest.permission.SYSTEM_ALERT_WINDOW*/)
                    .callBack(object : PermissionUtils.PermissionCallBack {
                        override fun onGranted(permissionUtils: PermissionUtils) {
                            SnackbarUtil.ShortSnackbar(
                                    window.decorView,
                                    "已授权",
                                    SnackbarUtil.WARNING
                            ).show()
                        }

                        override fun onDenied(permissionUtils: PermissionUtils) {
                            SnackbarUtil.ShortSnackbar(
                                    window.decorView,
                                    "拒绝了权限，将无法使用录音功能",
                                    SnackbarUtil.WARNING
                            ).show()
                        }
                    }).request()
        }

    }


    /**
     * 开启播放录音悬浮窗口服务
     */
    fun startFloatMusicService() {
        if (FloatingMusicService.isStarted) {
            return
        }
        if (checkFloatWindowPermission(mContext)) {  //有悬浮窗权限，直接开启悬浮窗
            startFloatMusicService(fileName)
        } else {  //没有悬浮窗权限
            AlertDialogUtil.getInstance(mContext).showAlertDialog("播放录音界面需要悬浮窗权限，请授权", "取消", "授权",
                    DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    },
                    DialogInterface.OnClickListener { dialog, which ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //6.0以上
                            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), 1002)
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
                val method = clazz.getDeclaredMethod("checkOp", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                return AppOpsManager.MODE_ALLOWED == method.invoke(manager, op, Binder.getCallingUid(), context.getPackageName()) as Int
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
                        SnackbarUtil.ShortSnackbar(window.decorView, "悬浮窗口权限未授权，无法使用播放功能", SnackbarUtil.WARNING).show()
                    } else {
                        startFloatMusicService(fileName)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//4.4-5.1)
                    if (checkOp(mContext, 24)) {  //OP_SYSTEM_ALERT_WINDOW = 24;
                        startFloatMusicService(fileName)
                    } else {//拒绝授权
                        SnackbarUtil.ShortSnackbar(window.decorView, "悬浮窗口权限未授权，无法使用播放功能", SnackbarUtil.WARNING).show()
                    }
                }
            }
        }
    }


}
