package com.tsa.imagerecognition


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import common.helpers.DatabaseHelper
import kotlinx.android.synthetic.main.activity_intro.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class IntroActivity : AppCompatActivity() {


    private lateinit var screenPager: ViewPager
    var introViewPagerAdapter: IntroViewPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)


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

    }
}
