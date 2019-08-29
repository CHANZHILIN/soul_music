package com.soul_music.view

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

import com.soul_music.myInterface.ScrollViewListener

class ObservableScrollView : HorizontalScrollView {

    private var scrollViewListener: ScrollViewListener? = null
    private val isTouch = false//默认是手势控制滑动

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet,
                defStyle: Int) : super(context, attrs, defStyle) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    fun setScrollViewListener(scrollViewListener: ScrollViewListener) {
        this.scrollViewListener = scrollViewListener
    }

    override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
        super.onScrollChanged(x, y, oldx, oldy)
        if (scrollViewListener != null) {
            scrollViewListener!!.onScrollChanged(this, x, y, oldx, oldy, isTouch)
        }
    }


}  