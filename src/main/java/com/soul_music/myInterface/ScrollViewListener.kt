package com.soul_music.myInterface


import com.soul_music.view.ObservableScrollView

/**
 * 接口监听 ScrollView的滑动
 * @author afnasdf
 */
interface ScrollViewListener {

    fun onScrollChanged(scrollView: ObservableScrollView, x: Int, y: Int, oldx: Int, oldy: Int, isByUser: Boolean)

}  