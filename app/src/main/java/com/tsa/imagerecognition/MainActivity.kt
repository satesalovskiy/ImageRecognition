
package com.tsa.imagerecognition

//import android.support.v7.app.AppCompatActivity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import common.helpers.DatabaseHelper
import common.helpers.SnackbarHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import kotlinx.android.synthetic.main.enter_data_dialog.view.*
import kotlinx.android.synthetic.main.fragment_list_of_default_images.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


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

    private lateinit var listFrag: ListOfDefaultImagesFragment


    lateinit var arFragment: ARFragment


    private lateinit var mDatabaseHelper: DatabaseHelper

    val listImages: ArrayList<Bitmap> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDatabaseHelper = DatabaseHelper(this)
        enterDataToMap()

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
       // supportActionBar?.setDisplayHomeAsUpEnabled(true)

       // supportFragmentManager.findFragmentById(R.id.ux_fragment)
        //auth = FirebaseAuth.getInstance()
        //Toast.makeText(this, auth.uid, Toast.LENGTH_LONG).show()


        checkPermissions()


//        kek.setOnClickListener{
//            setupFragment()
//
//
//           // Picasso.get().load("https://vk.com/im?peers=c257&sel=44403965&z=photo44403965_457242635%2Fmail1170545").into(image_view_fit_to_scan)
//
//
//        }
        closeFragment.setOnClickListener{
            dropFragment()
        }
        listFrag = ListOfDefaultImagesFragment()

        image_description.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                image_description.visibility = View.GONE
            }

            override fun onSwipeTop() {

            }
        })
    }



    private fun setupFragment() {
        getImage(this)

        closeFragment.visibility = View.VISIBLE
        var frt: FragmentTransaction = supportFragmentManager.beginTransaction()
        frt.add(R.id.frgmCont2, listFrag)
        frt.commit()
        Log.d("jij", "Fragment commiteed")

    }

    private fun dropFragment() {
        closeFragment.visibility = View.GONE
        var frt: FragmentTransaction = supportFragmentManager.beginTransaction()
        frt.remove(listFrag)
        frt.commit()
    }

    private fun getImage(context: Context) {
        var `is`: InputStream

        var files = context.assets.list("imagess")

        Log.d("jij", "list" + files?.size.toString())

        for(i in files!!.indices)
        {
            `is` = context.assets.open("imagess/" + files[i])
            val bitmap = BitmapFactory.decodeStream(`is`)
            listImages.add(i, bitmap)
        }

        Log.d("jij", "Images loaded" + listImages.size.toString())
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

                if(checkedPosition == "default"){
                    showThatUserCannotAddImage()
                    return true
                } else {
                    addNewPhotoToDB()
                    return true
                }
            }
            R.id.setup_fragment -> {
                setupFragment()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    private fun showThatUserCannotAddImage() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Change database?")
                .setMessage("You can't add new augmented image into Default database. Would you like to change active database?")
                .setCancelable(true)
                .setPositiveButton("Change") { dialog, id ->
                    showSwitchDialog()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, id ->
                    dialog.cancel()
                }
        val dialog = builder.create()
        dialog.show()
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
                .setPositiveButton("Add"){ dialogInterface, i ->

                    val name = view1.edit_name.text.toString()
                    val description = view1.edit_description.text.toString()

                    if(name.isEmpty() || !this::pickedImage.isInitialized || !this::pickedVideoPath.isInitialized || description.isEmpty()){
                        SnackbarHelper.getInstance().showMessage(this, "You forgot to initialize something!")
                    } else {
                        saveEverythingInStorage(name, description)
                    }
                }
                .setNegativeButton("Cancel"){ dialogInterface, _ -> dialogInterface.cancel() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun saveEverythingInStorage(name: String, description: String) {
        addDataToSQL(name, description)
        saveImageToDB(name)
        saveVideoToInternalStorage(name)
    }

    private fun addDataToSQL(name: String, description: String) {
        var insertData = mDatabaseHelper.addData(name, description)

        if(insertData){
            Toast.makeText(this, "Data added", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show()
        }
    }

     fun showImageDescriptionMain(name:String, default: Boolean ){
        if(default){

            val description = descriptionByName.get(name.substringBeforeLast("."))

            Log.d("pop", description.toString())

            image_desc.visibility = View.VISIBLE
            image_desc.bottom_sheet_description.text = description
            image_desc.bottom_sheet_topic.text = "Description for image: " + name
        } else {
            var data = mDatabaseHelper.data
            var description = "No description"


            while(data.moveToNext()) {
                Log.d("pipka", data.getString(0))
                if(name == data.getString(0)){
                    description = data.getString(1)
                    break
                }
            }
//        image_description.image_description_text.text = description
//        image_description.visibility = View.VISIBLE
            image_desc.visibility = View.VISIBLE
            image_desc.bottom_sheet_description.text = description
            image_desc.bottom_sheet_topic.text = "Description for image: " + name
        }
    }

    private fun getStringFromResoursesByName(name: String) : Int {
        val resourseId = resources.getIdentifier(name.substringBeforeLast("."), "string", packageName)
        Log.d("pop", name)
        return resourseId
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
                0 -> {
                    writeInPrefs("custom")
                    SnackbarHelper.getInstance()
                            .showMessage(this, "Changes will be committed after restart your app")
                }
                1 ->{
                    writeInPrefs("default")
                    SnackbarHelper.getInstance()
                            .showMessage(this, "Changes will be committed after restart your app")
                }
            }

            dialogInterface.dismiss()
        }
        // Set the neutral/cancel button click listener
        mBuilder.setNeutralButton("Cancel") { dialog, _ ->
            // Do something when click the neutral button
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }


    private fun checkPermissions() {
        if(!hasPermissions(this,android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){

            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    321)

        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            321 -> {
                permissions.forEachIndexed{ index, i ->
                    if(grantResults.isNotEmpty() && (grantResults[index] != PackageManager.PERMISSION_GRANTED)) {
                        if(i == android.Manifest.permission.CAMERA){
                            SnackbarHelper.getInstance().showError(this,"You can't use app without CAMERA permission")
                        } else if (i == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                            Toast.makeText(this, "You can't use custom image database without STORAGE permission", Toast.LENGTH_LONG).show()
                            writeInPrefs("default")
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun writeInPrefs(what: String){

        Log.d("FIRST_LAUNCH", "Saved" + what)

        val editor = pref.edit()
        editor.putString(APP_PREFERENCES_WHAT_DB_USE, what)
        editor.apply()
    }

    private var descriptionByName: HashMap<String, String> = HashMap()
    private fun enterDataToMap(){
        descriptionByName.put("nike", "Web site: https://www.nike.com \n " +
                "Video link: https://www.youtube.com/watch?v=IHcWPVbDArU")

        descriptionByName.put("adidas", "Web site: https://www.adidas.com \n " +
                "Video link: https://www.youtube.com/watch?v=_sIaPgpM2v0")

        descriptionByName.put("puma", "Web site: https://puma.com \n " +
                "Video link: https://www.youtube.com/watch?v=gnaH-R3yTDk")

        descriptionByName.put("converse", "Web site: https://converse.com \n " +
                "Video link: https://www.youtube.com/watch?v=nEZrlU9mO58")

        descriptionByName.put("skechers", "Web site: https://www.skechers.com \n " +
                "Video link: https://www.youtube.com/watch?v=ZEv-W-Y3-8E")

        descriptionByName.put("columbia", "Web site: https://www.columbia.com \n " +
                "Video link: https://www.youtube.com/watch?v=6asVmQZ2B5w&t=88s")

        descriptionByName.put("thenorthface", "Web site: https://thenorthface.com \n " +
                "Video link: https://www.youtube.com/watch?v=gPPzWqAaV88&t=1s")

        descriptionByName.put("reebok", "Web site: https://www.reebok.com \n " +
                "Video link: https://www.youtube.com/watch?v=W0X66M1MgHU&t=3s")

        descriptionByName.put("kappa", "Web site: https://kappa-usa.com \n " +
                "Video link: https://www.youtube.com/watch?v=oxzwaStxxkU")

        descriptionByName.put("santacruz", "Web site: https://www.santacruzbicycles.com \n " +
                "Video link: https://www.youtube.com/watch?v=P8db5IFErho")

        descriptionByName.put("intel", "Web site: https://www.intel.com \n " +
                "Video link: https://www.youtube.com/watch?v=_VMYPLXnd7E")

        descriptionByName.put("huawei", "Web site: https://shop.huawei.com \n " +
                "Video link: https://www.youtube.com/watch?v=_m3PIkZW6o8")

        descriptionByName.put("harman", "Web site: https://harmankardon.com \n " +
                "Video link: https://www.youtube.com/watch?v=LKdBU7p9178")

        descriptionByName.put("epam", "Web site: https://www.epam-group.ru \n " +
                "Video link: https://www.youtube.com/watch?v=sBst40WlH74")

        descriptionByName.put("microsoft", "Web site: https://www.microsoft.com \n " +
                "Video link: https://www.youtube.com/watch?v=miM6mBAfA8g")

        descriptionByName.put("google", "Web site: https://www.google.com \n " +
                "Video link: https://www.youtube.com/watch?v=nP7JtxdxdwI")

        descriptionByName.put("jetbrains", "Web site: https://www.jetbrains.com \n " +
                "Video link: https://www.youtube.com/watch?v=rhAunB7UQFQ")

        descriptionByName.put("oracle", "Web site: https://www.oracle.com \n " +
                "Video link: https://www.youtube.com/watch?v=C7Bp07T5img")

        descriptionByName.put("samsung", "Web site: https://www.samsung.com \n " +
                "Video link: https://www.youtube.com/watch?v=NwFvlwe7HPo")

        descriptionByName.put("nestle", "Web site: https://www.nestle.ru \n " +
                "Video link: https://www.youtube.com/watch?v=jZtEXMBbaZg&feature=emb_title")

        descriptionByName.put("danone", "Web site: http://www.danone.com \n " +
                "Video link: https://www.youtube.com/watch?v=DpoSFf-oNdo")

        descriptionByName.put("cocacola", "Web site: https://www.coca-cola.com \n " +
                "Video link: https://www.youtube.com/watch?v=8mKFF5K4aUI")

        descriptionByName.put("pepsico", "Web site: https://pepsi.com \n " +
                "Video link: https://www.youtube.com/watch?v=_LMySyD0WGc")

        descriptionByName.put("snickers", "Web site:  https://snickers.com \n " +
                "Video link: https://www.youtube.com/watch?v=2Acdwxhec1s")

        descriptionByName.put("mcdonalds", "Web site:  https://mcdonalds.com \n " +
                "Video link: https://www.youtube.com/watch?v=2alwcW6Bvso")

        descriptionByName.put("burgerking", "Web site: https://burgerking.com \n " +
                "Video link: https://www.youtube.com/watch?v=KKlDNsPBTxg")

        descriptionByName.put("subway", "Web site: https://subway.com \n " +
                "Video link: https://www.youtube.com/watch?v=gYNYq5MTGx8")

        descriptionByName.put("starbucks", "Web site: https://www.starbucks.com \n " +
                "Video link: https://www.youtube.com/watch?v=SuEjGt-TPK0")

        descriptionByName.put("kfc", "Web site: https://www.kfc.com \n " +
                "Video link: https://www.youtube.com/watch?v=pWRYvUZhBsA&feature=emb_title")

        descriptionByName.put("lays", "Web site: hßttps://lays.com \n " +
                "Video link: https://www.youtube.com/watch?v=lSaZkOX-FWw")
    }
}
