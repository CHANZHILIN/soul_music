package com.soul_music

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.audio.AudioTrackManager
import com.kotlin_baselib.base.BaseViewModelFragment
import com.kotlin_baselib.base.EmptyViewModel
import com.kotlin_baselib.recyclerview.SingleAdapter
import com.kotlin_baselib.recyclerview.setSingleUp
import com.kotlin_baselib.utils.SdCardUtil
import com.soul_music.entity.MusicEntity
import com.soul_picture.decoration.LinearLayoutDividerItemDecoration
import kotlinx.android.synthetic.main.fragment_music.*
import kotlinx.android.synthetic.main.layout_item_music.view.*
import kotlin.concurrent.thread


private const val ARG_PARAM = "param1"

/**
 *  Created by CHEN on 2019/6/20
 *  Email:1181785848@qq.com
 *  Package:com.soul_music
 *  Introduce: 音乐Fragment
 **/
class MusicFragment : BaseViewModelFragment<EmptyViewModel>() {


    override fun getResId(): Int = R.layout.fragment_music

    private lateinit var fileData: MutableList<String>
    private var param1: String? = null

    override fun providerVMClass(): Class<EmptyViewModel>? = EmptyViewModel::class.java

    var rotate: ObjectAnimator? = null
    var musicData: MutableList<MusicEntity> = ArrayList<MusicEntity>()

    var currentPlayPosition = 0        //播放的音频的位置


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM)
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            MusicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM, param1)
                }
            }
    }


    override fun initData() {

        //播放音频时候动画
        rotate = ObjectAnimator.ofFloat(fragment_music_circleImageView, "rotation", 0f, 360f)
        rotate!!.apply {
            duration = 5000
            interpolator = LinearInterpolator()
            repeatCount = -1
            repeatMode = ObjectAnimator.RESTART
        }


        fileData = SdCardUtil.getFilesAllName(SdCardUtil.DEFAULT_RECORD_PATH)


        for (fileDatum in fileData) {
            musicData.add(MusicEntity(fileDatum))
        }

        fragment_music_recyclerview.setSingleUp(
            musicData,
            R.layout.layout_item_music,
            LinearLayoutManager(mContext),
            { holder, item ->

                holder.itemView.item_music_tv_audio_name.text = item.path.split("/").last()

            },
            {
                /*     SnackbarUtil.ShortSnackbar(
                         fragment_picture_recyclerview,
                         "点击${it.picturePath}！",
                         SnackbarUtil.CONFIRM
                     )
                         .show()*/
                startPlayMusic(it.path)

            }
        )

        fragment_music_recyclerview.addItemDecoration(
            LinearLayoutDividerItemDecoration(
                mContext,
                Constants.ITEM_SPACE
            )
        )

    }

    override fun initListener() {

        fragment_music_refresh_layout.setOnRefreshListener {
            if (fileData.size >= 0) {
                fileData.clear()
                musicData.clear()
            }
            fileData = SdCardUtil.getFilesAllName(SdCardUtil.DEFAULT_RECORD_PATH)  //重新获取一次文件
            for (fileDatum in fileData) {
                musicData.add(MusicEntity(fileDatum))
            }
            fragment_music_recyclerview.adapter!!.notifyDataSetChanged()

            fragment_music_refresh_layout.isRefreshing = false
        }

        fragment_music_play.setOnClickListener {
            startPlayMusic(musicData.get(currentPlayPosition).path)
        }
        fragment_music_pause.setOnClickListener {
            AudioTrackManager.getInstance().stopPlay()
        }
        fragment_music_edit.setOnClickListener {
            ARouter.getInstance().build(Constants.EDIT_AUDIO_ACTIVITY_PATH)
                .withString("fileName", musicData.get(currentPlayPosition).path.split("/").last())
                .navigation()
        }

    }

    fun startPlayMusic(path: String) {
        fragment_music_tv_audio_name.text = path
        for (i in 0 until musicData.size) {
            if (musicData.get(i).path.equals(path)) currentPlayPosition = i
        }
        AudioTrackManager.getInstance().startPlay(path)
        startPlayAnimation()
        AudioTrackManager.getInstance()
            .setOnAudioStatusChangeListener(object : AudioTrackManager.onAudioStatusChange {
                override fun onPlay() {
//                    startPlayAnimation()
                }

                override fun onStop() {
                    stopPlayAnimation()

                }


            })
    }


    fun startPlayAnimation() {
        if (rotate!!.isStarted) {
            rotate!!.resume()
        } else {
            rotate!!.start()
        }
    }

    fun stopPlayAnimation() {
        activity!!.runOnUiThread {
            rotate!!.pause()
//            if (currentPlayPosition == musicData.size - 1) currentPlayPosition = 0 else currentPlayPosition++
//            startPlayMusic(musicData.get(currentPlayPosition).path)
        }

    }

    override fun onPause() {
        super.onPause()
        if (AudioTrackManager.getInstance().isPlaying()) AudioTrackManager.getInstance().stopPlay()
    }


}
