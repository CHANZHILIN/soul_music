package com.soul_music.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kotlin_baselib.base.BaseViewModel
import com.soul_music.entity.MusicEntity

/**
 *  Created by CHEN on 2019/12/13
 *  Email:1181785848@qq.com
 *  Introduce:
 **/
class MusicViewModel : BaseViewModel() {
    private val data: MutableLiveData<MutableList<MusicEntity>> by lazy {
        MutableLiveData<MutableList<MusicEntity>>().also {
            loadDatas()
        }
    }


    private val repository = MusicRepository()

    fun getMusicListData(): LiveData<MutableList<MusicEntity>> {
        return data
    }

    private fun loadDatas() = launchUI {
        val result = repository.getMusicData()
        data.value = result
    }

}