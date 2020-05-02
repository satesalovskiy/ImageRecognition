package com.tsa.imagerecognition


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import common.helpers.DatabaseHelper
import kotlinx.android.synthetic.main.activity_intro.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.collections.ArrayList

class IntroActivity : AppCompatActivity() {
    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_SHOW_INTRO = "intro"


    private lateinit var screenPager: ViewPager
    var introViewPagerAdapter: IntroViewPagerAdapter? = null
    private lateinit var tabIndicator: TabLayout
    private lateinit var buttonNext: Button
    private lateinit var buttonStart: Button
    private var position = 0

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = applicationContext.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

        val wasIntro = sharedPreferences.getBoolean(APP_PREFERENCES_SHOW_INTRO, false)
        if(wasIntro) {
            //goToMain()
        }

        setContentView(R.layout.activity_intro)

        supportActionBar?.hide()


        buttonNext = btn_next
        buttonStart = btn_get_started
        tabIndicator = tab_indicator

        var skip = skip
        skip.setOnClickListener{
            saveIntroWasShown(true)
            goToMain()
        }


        val mList = ArrayList<ScreenItem>()
        mList.add(ScreenItem("Scan image",
                "Point your smartphone at the picture you want to recognize. If the picture is recognized, an icon will appear that indicates that you may have to turn the smartphone a little",
                R.drawable.intro_image_1))
        mList.add(ScreenItem("Video placing",
                "Wait for the video to be placed on top of the picture",
                R.drawable.intro_image_2))
        mList.add(ScreenItem("Description",
                "You could find additional information about the video at the bottom of screen",
                R.drawable.intro_image_3))
        mList.add(ScreenItem("Additional options",
                "You could access several interesting functions by clicking on the menu icon in the upper right corner",
                R.drawable.intro_image_4))



        screenPager = screen_viewpager
        introViewPagerAdapter = IntroViewPagerAdapter(this, mList)
        screenPager.adapter = introViewPagerAdapter



        tabIndicator.setupWithViewPager(screenPager)

        buttonNext.setOnClickListener{

            position = screenPager.currentItem

            if(position < mList.size-1 ){
                position++
                screenPager.currentItem = position
            }

            if(position == mList.size) {
                showLastScreen()
            }

        }

        buttonStart.setOnClickListener{
            goToMain()
            saveIntroWasShown(true)
        }

        tabIndicator.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(p0: TabLayout.Tab?) {
                if(p0?.position == mList.size-1) {
                    showLastScreen()
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {
            }
        })
    }


    private fun saveIntroWasShown(shown: Boolean) {

        var editor = sharedPreferences.edit()
        editor.putBoolean(APP_PREFERENCES_SHOW_INTRO, true)
        editor.apply()



    }

    private fun goToMain(){
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
