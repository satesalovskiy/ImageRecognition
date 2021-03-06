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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import common.helpers.DatabaseHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_FIRST_LAUNCH = "first_launch"
    private val APP_PREFERENCES_WHAT_DB_USE = "what_db_use"

    private lateinit var pref: SharedPreferences
    private lateinit var animation1: ObjectAnimator
    private lateinit var animation2: ObjectAnimator
    private lateinit var animation3: ObjectAnimator
    private lateinit var animation4: ObjectAnimator
    private lateinit var listFrag: ListOfDefaultImagesFragment
    private lateinit var mDatabaseHelper: DatabaseHelper
    lateinit var arFragment: ARFragment
    private lateinit var addImageFragment: AddNewImagesFragment

    private val set = AnimatorSet()

    private var isFirstLaunch: Boolean = true

    private var checkedPosition: String? = "default"

    val listImages: ArrayList<Bitmap> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        toolbar.title = ""
        setSupportActionBar(toolbar)

        pref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)

        initializeAnimations()

        mDatabaseHelper = DatabaseHelper(this)
        enterDataToMap()

        if (pref.contains(APP_PREFERENCES_FIRST_LAUNCH)) {
            isFirstLaunch = pref.getBoolean(APP_PREFERENCES_FIRST_LAUNCH, false)
        }
        if (pref.contains(APP_PREFERENCES_WHAT_DB_USE)) {
            checkedPosition = pref.getString(APP_PREFERENCES_WHAT_DB_USE, "")
        }
        if (isFirstLaunch) {
            val editor = pref.edit()
            editor.putBoolean(APP_PREFERENCES_FIRST_LAUNCH, false)
            editor.putString(APP_PREFERENCES_WHAT_DB_USE, "default")
            editor.apply()
        }

        arFragment = ARFragment()
        listFrag = ListOfDefaultImagesFragment()
        addImageFragment = AddNewImagesFragment()

        val frt: FragmentTransaction = supportFragmentManager.beginTransaction()
        frt.add(R.id.frgmCont, arFragment)
        frt.commit()
    }

    private fun initializeAnimations() {
        animation1 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", 100F)
        animation2 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", -100F)
        animation3 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", 0F)
        animation4 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", 0F)
    }

    fun showMovePhoneAnimation() {

        image_view_fit_to_scan.visibility = View.VISIBLE

        set.playSequentially(animation1, animation3, animation2, animation4)
        set.interpolator = DecelerateInterpolator()
        set.duration = 2000

        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                set.start()
            }
        })
        set.start()
    }

    fun stopMovePhoneAnimation() {
        animation1.cancel()
        animation2.cancel()
        animation3.cancel()
        animation4.cancel()
        set.cancel()
        image_view_fit_to_scan.visibility = View.GONE
    }

    private fun setupFragment(fragment: Fragment) {
        if (fragment is ListOfDefaultImagesFragment) getImage(this)

        val frt: FragmentTransaction = supportFragmentManager.beginTransaction()
        frt.replace(R.id.frgmCont2, fragment)
        frt.commit()
    }

    fun dropFragment(listFragment: Boolean) {

        val frt: FragmentTransaction = supportFragmentManager.beginTransaction()

        if (listFragment) {
            frt.remove(listFrag)
        } else {
            frt.remove(addImageFragment)
        }
        frt.commit()
    }

    private fun getImage(context: Context) {
        var `is`: InputStream

        val files = context.assets.list("imagess")

        for (i in files!!.indices) {
            `is` = context.assets.open("imagess/" + files[i])
            val bitmap = BitmapFactory.decodeStream(`is`)
            listImages.add(i, bitmap)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.switch_db -> {
                showSwitchDialog()
                return true
            }
            R.id.add_photo -> {
                if (checkedPosition == "default") {
                    showThatUserCannotAddImage()
                    return true
                } else {
                    addNewPhotoToDB()
                    return true
                }
            }
            R.id.setup_fragment -> {
                setupFragment(listFrag)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showThatUserCannotAddImage() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.main_activity_add_dialog_title)
                .setMessage(R.string.main_activity_add_dialog_message)
                .setCancelable(true)
                .setPositiveButton(R.string.main_activity_add_dialog_positive) { dialog, _ ->
                    showSwitchDialog()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.main_activity_add_dialog_negative) { dialog, _ ->
                    dialog.cancel()
                }
        val dialog = builder.create()
        dialog.show()
    }

    private fun addNewPhotoToDB() {
        setupFragment(addImageFragment)
    }

    fun showImageDescriptionMain(name: String, default: Boolean) {
        if (default) {

            val description = descriptionByName[name.substringBeforeLast(".")]
            image_desc.bottom_sheet_description.text = description
            image_desc.visibility = View.VISIBLE

            val info = getString(R.string.main_activity_information_about)
            val newText = "$info <b>$name</b>"
            image_desc.bottom_sheet_topic.text = (Html.fromHtml(newText))
        } else {
            val data = mDatabaseHelper.data

            var description = getString(R.string.main_activity_no_description)

            while (data.moveToNext()) {
                if (name == data.getString(0)) {
                    description = data.getString(1)
                    break
                }
            }

            image_desc.visibility = View.VISIBLE
            image_desc.bottom_sheet_description.text = description

            val descr = getString(R.string.main_activity_description_for)
            val newText = "$descr <b>$name</b>"
            image_desc.bottom_sheet_topic.text = (Html.fromHtml(newText))
        }
    }

    private fun storeFileInInternalStorage(selectedFile: File, internalStorageFileName: String) {

        val inputStream = FileInputStream(selectedFile)
        val outputStream = application.openFileOutput(internalStorageFileName, Context.MODE_PRIVATE)
        val buffer = ByteArray(1024)
        inputStream.use {
            while (true) {
                val byeCount = it.read(buffer)
                if (byeCount < 0) break
                outputStream.write(buffer, 0, byeCount)
            }
            outputStream.close()
        }
    }

    private fun showSwitchDialog() {

        val listItems = arrayOf(getString(R.string.main_activity_dialog_custom), getString(R.string.main_activity_dialog_default))
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        mBuilder.setTitle(getString(R.string.main_activity_dialog_title))

        var checkedItem = 1
        if (checkedPosition == "default") {
            checkedItem = 1
        } else {
            checkedItem = 0
        }

        mBuilder.setSingleChoiceItems(listItems, checkedItem) { dialogInterface, i ->

            when (i) {
                0 -> {
                    writeInPrefs("custom")
                    Toast.makeText(this, R.string.main_activity_dialog_toast_restart, Toast.LENGTH_LONG).show()
                }
                1 -> {
                    writeInPrefs("default")
                    Toast.makeText(this,  R.string.main_activity_dialog_toast_restart, Toast.LENGTH_LONG).show()
                }
            }

            dialogInterface.dismiss()
        }

        mBuilder.setNeutralButton(getString(R.string.main_activity_dialog_button_cancel)) { dialog, _ ->
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }


    private fun checkPermissions() {
        if (!hasPermissions(this, android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    321)

        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            321 -> {
                permissions.forEachIndexed { index, i ->
                    if (grantResults.isNotEmpty() && (grantResults[index] != PackageManager.PERMISSION_GRANTED)) {
                        if (i == android.Manifest.permission.CAMERA) {
                            Toast.makeText(this, R.string.main_activity_no_camera_perm, Toast.LENGTH_LONG).show()
                        } else if (i == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                            Toast.makeText(this, R.string.main_activity_no_storage_perm, Toast.LENGTH_LONG).show()
                            writeInPrefs("default")
                        }
                    }
                }
            }
            else -> {
            }
        }
    }

    private fun writeInPrefs(what: String) {
        val editor = pref.edit()
        editor.putString(APP_PREFERENCES_WHAT_DB_USE, what)
        editor.apply()
    }

    private var descriptionByName: HashMap<String, String> = HashMap()
    private fun enterDataToMap() {
        descriptionByName.put("nike", "Web site: https://www.nike.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=IHcWPVbDArU")

        descriptionByName.put("adidas", "Web site: https://www.adidas.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=_sIaPgpM2v0")

        descriptionByName.put("puma", "Web site: https://puma.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=gnaH-R3yTDk")

        descriptionByName.put("converse", "Web site: https://converse.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=nEZrlU9mO58")

        descriptionByName.put("skechers", "Web site: https://www.skechers.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=ZEv-W-Y3-8E")

        descriptionByName.put("columbia", "Web site: https://www.columbia.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=6asVmQZ2B5w&t=88s")

        descriptionByName.put("thenorthface", "Web site: https://thenorthface.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=gPPzWqAaV88&t=1s")

        descriptionByName.put("reebok", "Web site: https://www.reebok.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=W0X66M1MgHU&t=3s")

        descriptionByName.put("kappa", "Web site: https://kappa-usa.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=oxzwaStxxkU")

        descriptionByName.put("santacruz", "Web site: https://www.santacruzbicycles.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=P8db5IFErho")

        descriptionByName.put("intel", "Web site: https://www.intel.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=_VMYPLXnd7E")

        descriptionByName.put("huawei", "Web site: https://shop.huawei.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=_m3PIkZW6o8")

        descriptionByName.put("harman", "Web site: https://harmankardon.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=LKdBU7p9178")

        descriptionByName.put("epam", "Web site: https://www.epam-group.ru \n\n " +
                "Video link: https://www.youtube.com/watch?v=sBst40WlH74")

        descriptionByName.put("microsoft", "Web site: https://www.microsoft.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=miM6mBAfA8g")

        descriptionByName.put("google", "Web site: https://www.google.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=nP7JtxdxdwI")

        descriptionByName.put("jetbrains", "Web site: https://www.jetbrains.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=rhAunB7UQFQ")

        descriptionByName.put("oracle", "Web site: https://www.oracle.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=C7Bp07T5img")

        descriptionByName.put("samsung", "Web site: https://www.samsung.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=NwFvlwe7HPo")

        descriptionByName.put("nestle", "Web site: https://www.nestle.ru \n\n " +
                "Video link: https://www.youtube.com/watch?v=jZtEXMBbaZg&feature=emb_title")

        descriptionByName.put("danone", "Web site: http://www.danone.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=DpoSFf-oNdo")

        descriptionByName.put("cocacola", "Web site: https://www.coca-cola.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=8mKFF5K4aUI")

        descriptionByName.put("pepsico", "Web site: https://pepsi.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=_LMySyD0WGc")

        descriptionByName.put("snickers", "Web site:  https://snickers.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=2Acdwxhec1s")

        descriptionByName.put("mcdonalds", "Web site:  https://mcdonalds.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=2alwcW6Bvso")

        descriptionByName.put("burgerking", "Web site: https://burgerking.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=KKlDNsPBTxg")

        descriptionByName.put("subway", "Web site: https://subway.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=gYNYq5MTGx8")

        descriptionByName.put("starbucks", "Web site: https://www.starbucks.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=SuEjGt-TPK0")

        descriptionByName.put("kfc", "Web site: https://www.kfc.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=pWRYvUZhBsA&feature=emb_title")

        descriptionByName.put("lays", "Web site: https://lays.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=lSaZkOX-FWw")
    }
}
