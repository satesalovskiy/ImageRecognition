package com.tsa.imagerecognition

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import kotlinx.android.synthetic.main.layout_screen.view.*

class IntroViewPagerAdapter (var mContext: Context, var mListScreen: List<ScreenItem>) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val layoutScreen = LayoutInflater.from(mContext).inflate(R.layout.layout_screen, null)
        val imgSlide = layoutScreen.intro_img
        val title = layoutScreen.intro_title
        val description = layoutScreen.intro_description

        title.text = mListScreen[position].title
        description.text = mListScreen[position].description
        imgSlide.setImageResource(mListScreen[position].screenImg)

        container.addView(layoutScreen)

        return layoutScreen
    }

    override fun getCount(): Int {
        return mListScreen.size
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === o
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

        container.removeView(`object` as View)
    }
}