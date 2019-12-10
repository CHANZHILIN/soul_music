package com.soul_music

import android.os.Bundle
import com.alibaba.android.arouter.launcher.ARouter
import com.kotlin_baselib.api.Constants
import com.kotlin_baselib.mvvmbase.BaseViewModelFragment
import com.kotlin_baselib.mvvmbase.EmptyViewModel
import kotlinx.android.synthetic.main.fragment_music.*


private const val ARG_PARAM = "param1"

/**
 *  Created by CHEN on 2019/6/20
 *  Email:1181785848@qq.com
 *  Package:com.soul_music
 *  Introduce: 音乐Fragment
 **/
class MusicFragment : BaseViewModelFragment<EmptyViewModel>() {

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

    override fun getResId(): Int {
        return R.layout.fragment_music
    }

    override fun initData() {
        fragment_text.setText(param1)
    }

    override fun initListener() {
      /*  float_action_button.setOnClickListener {
            ARouter.getInstance().build(Constants.NRECORD_AUDIO_ACTIVITY_PATH).navigation()

        }*/
    }


}
