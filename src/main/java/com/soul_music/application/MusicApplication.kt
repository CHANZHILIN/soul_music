package com.soul_music.application

import android.app.Application
import com.kotlin_baselib.application.BaseApplication
import com.kotlin_baselib.application.IComponentApplication

/**
 *  Created by CHEN on 2019/6/20
 *  Email:1181785848@qq.com
 *  Package:com.soul_music.application
 *  Introduce:
 **/
class MusicApplication : IComponentApplication {

    lateinit var instance: BaseApplication
    override val application: Application
        get() = instance

    override fun onCreate(application: BaseApplication) {
        instance = application
    }
}