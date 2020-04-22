
package com.tsa.imagerecognition

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
//import android.support.v7.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.enter_data_dialog.view.*
import java.util.jar.Manifest
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment.getExternalStorageDirectory
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.*
import java.net.URI
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_FIRST_LAUNCH = "first_launch"
    private val APP_PREFERENCES_WHAT_DB_USE = "what_db_use"
    private lateinit var pref: SharedPreferences
    private var isFirstLaunch: Boolean = true

    private var checkedPosition: String? = "default"

    private lateinit var pickedImage: Bitmap
    private lateinit var pickedVideoPath: String


    lateinit var arFragment: ARFragment

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

        arFragment = ARFragment()
        var frt: FragmentTransaction = supportFragmentManager.beginTransaction()
        frt.add(R.id.frgmCont, arFragment)
        frt.commit()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

       // supportFragmentManager.findFragmentById(R.id.ux_fragment)
        //auth = FirebaseAuth.getInstance()
        //Toast.makeText(this, auth.uid, Toast.LENGTH_LONG).show()


        checkPermissions()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.switch_db -> {
                showSwitchDialog()
                return true
            }
            R.id.add_photo -> {
                addNewPhotoToDB()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun addNewPhotoToDB() {
        //arFragment.addNewImage()
        showEnterDataDialog()
    }


    private fun showEnterDataDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val view1 = inflater.inflate(R.layout.enter_data_dialog, null)

        view1.edit_image.setOnClickListener {
            chooseImage()
        }

        view1.edit_video.setOnClickListener {
            chooseVideo()
        }


        builder.setView(view1)
                .setPositiveButton("Add", DialogInterface.OnClickListener { dialogInterface, i ->

                    val name = view1.edit_name.text.toString()
                    saveEverythingInStorage(name)
                })
                .setNegativeButton("Cancel"){ dialogInterface, _ -> dialogInterface.cancel() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun saveEverythingInStorage(name: String) {
        saveImageToDB(name)
        saveVideoToInternalStorage(name)
    }

    private fun chooseVideo() {

//        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//        startActivityForResult(galleryIntent, 222)

        val videoPickerIntent = Intent(Intent.ACTION_PICK)
        videoPickerIntent.type = "video/*"
        startActivityForResult(videoPickerIntent, 222)
    }

    private fun chooseImage() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, 111)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            111 -> {
                if(resultCode == Activity.RESULT_OK && data != null){
                    val selectedImage = data.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    pickedImage = bitmap
                    Toast.makeText(this, "Image picked",Toast.LENGTH_LONG).show()

                }
            }
            222 -> {
                if(resultCode == Activity.RESULT_OK && data != null){
                    val selectedVideo = data.data
                    val selectedVideoPath = getPath(selectedVideo!!)
                    pickedVideoPath = selectedVideoPath
                    Log.d("path ",selectedVideoPath)
                    //saveVideoToInternalStorage(selectedVideoPath)
                }
            }
        }
    }


    private fun saveVideoToInternalStorage (name: String) {

        val selectedVideoFile : File = File(pickedVideoPath)  // 2
        val selectedVideoFileExtension : String = selectedVideoFile.extension  // 3
        val internalStorageVideoFileName : String = name +"."+ selectedVideoFileExtension

        var resultFile = File(Environment.getExternalStorageDirectory(), internalStorageVideoFileName)
        var fos = FileOutputStream(resultFile)
        fos.write(selectedVideoFile.readBytes())
        fos.close()
        Log.d("File", "File saved")

        var file = File(Environment.getExternalStorageDirectory()
                          .getAbsolutePath(), internalStorageVideoFileName)
        Log.d("File", "File reed" + file.path)

       // storeFileInInternalStorage(selectedVideoFile, internalStorageVideoFileName)

//        val file : File = application.getFileStreamPath("" + filesDir + "/" +internalStorageVideoFileName)

       // var file = File( getFilesDir(), internalStorageVideoFileName)
      //  Log.d("savein", file.path.toString())
        //videovvv.setVideoPath(file.path.toString())
    }


    private fun storeFileInInternalStorage(selectedFile: File, internalStorageFileName: String) {

        Log.d("savein", internalStorageFileName)
        Log.d("savein", selectedFile.path)
        val inputStream = FileInputStream(selectedFile) // 1
        val outputStream = application.openFileOutput(internalStorageFileName, Context.MODE_PRIVATE)  // 2
        val buffer = ByteArray(1024)
        inputStream.use {  // 3
            while (true) {
                val byeCount = it.read(buffer)  // 4
                if (byeCount < 0) break
                outputStream.write(buffer, 0, byeCount)  // 5
            }
            outputStream.close()  // 6
        }
    }
    private fun saveImageToDB (name: String) {
        arFragment.addNewImage(pickedImage, name)
    }

    private fun getPath(uri: Uri): String {
        val projection = arrayOf(MediaStore.Video.Media.DATA)

        //Cursor cursor = getContentResolver().query(uri, projection, null, null, null)

        val cursor = contentResolver.query(uri,projection, null, null, null)

        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA

            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index);
        } else {
            Toast.makeText(this, "Cursor is null", Toast.LENGTH_LONG).show()
            return ""
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
