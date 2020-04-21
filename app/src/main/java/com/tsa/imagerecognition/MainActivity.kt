
package com.tsa.imagerecognition

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
//import android.support.v7.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_FIRST_LAUNCH = "first_launch"
    private val APP_PREFERENCES_WHAT_DB_USE = "what_db_use"
    private lateinit var pref: SharedPreferences
    private var isFirstLaunch: Boolean = true

    private var checkedPosition: String? = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)
        if(pref.contains(APP_PREFERENCES_FIRST_LAUNCH)){
            isFirstLaunch = pref.getBoolean(APP_PREFERENCES_FIRST_LAUNCH, false)
        }
        if(pref.contains(APP_PREFERENCES_WHAT_DB_USE)){
            checkedPosition = pref.getString(APP_PREFERENCES_WHAT_DB_USE,"")
        }
        if (isFirstLaunch){

            Log.d("FIRST_LAUNCH", "Real first launch")
            val editor = pref.edit()

            editor.putBoolean(APP_PREFERENCES_FIRST_LAUNCH, false)

            editor.putString(APP_PREFERENCES_WHAT_DB_USE, "default")

            editor.apply()
        }




        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.findFragmentById(R.id.ux_fragment)
        //auth = FirebaseAuth.getInstance()
        //Toast.makeText(this, auth.uid, Toast.LENGTH_LONG).show()

        kek.setOnClickListener{
            val fragment: ARFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ARFragment
            fragment.addNewImage()
        }

        checkPermissions()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.switch_db -> {
                Log.d("MyApp", "Jopasa")
                showSwitchDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showSwitchDialog() {
        val listItems = arrayOf("Custom", "Default")
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        mBuilder.setTitle("Choose an item")

        var checkedItem = 1
        if(checkedPosition == "default"){
            checkedItem = 1
        } else {
            checkedItem = 0
        }

        mBuilder.setSingleChoiceItems(listItems, checkedItem) { dialogInterface, i ->

            when(i){
                0 -> writeInPrefs("custom")
                1 -> writeInPrefs("default")
            }

            dialogInterface.dismiss()
        }
        // Set the neutral/cancel button click listener
        mBuilder.setNeutralButton("Cancel") { dialog, which ->
            // Do something when click the neutral button
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }


    private fun checkPermissions() {
        if(ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), 1)
            return
        }
    }

    private fun writeInPrefs(what: String){

        Log.d("FIRST_LAUNCH", "Saved" + what)

        val editor = pref.edit()
        editor.putString(APP_PREFERENCES_WHAT_DB_USE, what)
        editor.apply()
    }


}
