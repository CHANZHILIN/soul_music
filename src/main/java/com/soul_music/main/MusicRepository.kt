package com.soul_music.main

import com.kotlin_baselib.base.BaseRepository
import com.kotlin_baselib.utils.SdCardUtil
import com.soul_music.entity.MusicEntity

/**
 *  Created by CHEN on 2019/12/13
 *  Email:1181785848@qq.com
 *  Introduce:
 **/
class MusicRepository : BaseRepository() {
/*    suspend fun getPictureData():ResponseData<EmptyEntity> = request {
        ApiEngine.apiService.getVersionData()
    }*/

    suspend fun getMusicData(): MutableList<MusicEntity> = requestLocal {
        val fileData = SdCardUtil.getFilesAllName(SdCardUtil.DEFAULT_RECORD_PATH)
        val musicData = ArrayList<MusicEntity>()
        for (fileDatum in fileData) {   //封装实体类，加入随机高度，解决滑动过程中位置变换的问题
            musicData.add(MusicEntity(fileDatum))
        }
        musicData
    }
}