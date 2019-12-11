package com.soul_music

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.base.BaseViewModelFragment
import com.kotlin_baselib.base.EmptyViewModel
import com.kotlin_baselib.recyclerview.setSingleUp
import com.kotlin_baselib.utils.SdCardUtil
import com.soul_music.entity.MusicEntity
import com.soul_picture.decoration.LinearLayoutDividerItemDecoration
import kotlinx.android.synthetic.main.fragment_music.*
import kotlinx.android.synthetic.main.layout_item_music.view.*


private const val ARG_PARAM = "param1"

/**
 *  Created by CHEN on 2019/6/20
 *  Email:1181785848@qq.com
 *  Package:com.soul_music
 *  Introduce: 音乐Fragment
 **/
class MusicFragment : BaseViewModelFragment<EmptyViewModel>() {


    override fun getResId(): Int = R.layout.fragment_music

    private var param1: String? = null

    override fun providerVMClass(): Class<EmptyViewModel>? = EmptyViewModel::class.java

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

        val musicSrc = SdCardUtil.DEFAULT_RECORD_PATH;
        val fileData = SdCardUtil.getFilesAllName(musicSrc)

        val pictureData = ArrayList<MusicEntity>()
        for (fileDatum in fileData) {
            pictureData.add(MusicEntity(fileDatum))
        }

        fragment_music_recyclerview.setSingleUp(
            pictureData,
            R.layout.layout_item_music,
            LinearLayoutManager(mContext),
            { holder, item ->
                holder.itemView.item_music_tv_audio_name.text = item.path
            },
            {
                /*     SnackbarUtil.ShortSnackbar(
                         fragment_picture_recyclerview,
                         "点击${it.picturePath}！",
                         SnackbarUtil.CONFIRM
                     )
                         .show()*/
            }
        )

        fragment_music_recyclerview.addItemDecoration(
            LinearLayoutDividerItemDecoration(
                mContext,
                Constants.ITEM_SPACE
            )
        )
        /*    fragment_picture_recyclerview.addItemDecoration(
                PictureFragmentStaggeredDividerItemDecoration(
                    mContext,
                    ITEM_SPACE
                )
            )*/
    }

    override fun initListener() {
        /*  float_action_button.setOnClickListener {
              ARouter.getInstance().build(Constants.NRECORD_AUDIO_ACTIVITY_PATH).navigation()

          }*/
    }


}
