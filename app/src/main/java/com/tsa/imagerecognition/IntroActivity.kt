package com.tsa.imagerecognition

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_intro.*
import kotlin.collections.ArrayList

class IntroActivity : AppCompatActivity() {

    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_SHOW_INTRO = "intro"

    private lateinit var screenPager: ViewPager
    private lateinit var tabIndicator: TabLayout
    private lateinit var buttonNext: Button
    private lateinit var buttonStart: Button
    private lateinit var sharedPreferences: SharedPreferences

    private var position = 0
    private var introViewPagerAdapter: IntroViewPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        sharedPreferences = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val wasIntro = sharedPreferences.getBoolean(APP_PREFERENCES_SHOW_INTRO, false)
        if (wasIntro) {
            goToMain()
            finish()
        }

        setContentView(R.layout.activity_intro)

        supportActionBar?.hide()

        buttonNext = btn_next
        buttonStart = btn_get_started
        tabIndicator = tab_indicator


        val skip = skip
        skip.setOnClickListener {
            saveIntroWasShown()
            goToMain()
        }

        val mList = ArrayList<ScreenItem>()
        mList.add(ScreenItem(getString(R.string.intro_activity_scan_image),
                getString(R.string.intro_activity_scan_image_description),
                R.drawable.intro_image_1))
        mList.add(ScreenItem(getString(R.string.intro_activity_video_placing),
                getString(R.string.intro_activity_video_placing_description),
                R.drawable.intro_image_2))
        mList.add(ScreenItem(getString(R.string.intro_activity_description),
                getString(R.string.intro_activity_description_description),
                R.drawable.intro_image_3))
        mList.add(ScreenItem(getString(R.string.intro_activity_additional_options),
                getString(R.string.intro_activity_additional_options_description),
                R.drawable.intro_image_4))

        screenPager = screen_viewpager
        introViewPagerAdapter = IntroViewPagerAdapter(this, mList)
        screenPager.adapter = introViewPagerAdapter

        tabIndicator.setupWithViewPager(screenPager)

        buttonNext.setOnClickListener {

            position = screenPager.currentItem

            if (position < mList.size - 1) {
                position++
                screenPager.currentItem = position
            }

            if (position == mList.size) {
                showLastScreen()
            }
        }

        buttonStart.setOnClickListener {
            goToMain()
            saveIntroWasShown()
        }

        tabIndicator.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(p0: TabLayout.Tab?) {
                if (p0?.position == mList.size - 1) {
                    showLastScreen()
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })
    }

    private fun saveIntroWasShown() {

        val editor = sharedPreferences.edit()
        editor.putBoolean(APP_PREFERENCES_SHOW_INTRO, true)
        editor.apply()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLastScreen() {
        buttonNext.visibility = View.GONE
        tabIndicator.visibility = View.GONE
        buttonStart.visibility = View.VISIBLE
    }
}
