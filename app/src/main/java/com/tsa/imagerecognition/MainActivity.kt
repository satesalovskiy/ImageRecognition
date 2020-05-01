
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
import com.google.firebase.auth.FirebaseAuth
import common.helpers.DatabaseHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import java.io.File
import java.io.FileInputStream
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



    private lateinit var listFrag: ListOfDefaultImagesFragment

    private lateinit var mDatabaseHelper: DatabaseHelper



    public lateinit var arFragment: ARFragment

    lateinit var addImageFragment: AddNewImagesFragment



    val listImages: ArrayList<Bitmap> = ArrayList()

    private lateinit var animation1: ObjectAnimator
    private lateinit var animation2: ObjectAnimator
    private lateinit var animation3: ObjectAnimator
    private lateinit var animation4: ObjectAnimator


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeAnimations()


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


        toolbar.title = ""
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

//
//        closeFragment.setOnClickListener{
//            dropFragment()
//        }



        listFrag = ListOfDefaultImagesFragment()
        addImageFragment = AddNewImagesFragment()

       // showMovePhoneAnimation()

        kek.setOnClickListener{
            stopMovePhoneAnimation()
        }

    }


    private fun initializeAnimations() {
        animation1 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", 100F)
        animation2 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", -100F)
        animation3 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", 0F)
        animation4 = ObjectAnimator.ofFloat(image_view_fit_to_scan, "translationX", 0F)
    }


    val set = AnimatorSet()
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

            override fun onAnimationCancel(animation: Animator?) {
                super.onAnimationCancel(animation)
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
        if(fragment is ListOfDefaultImagesFragment) getImage(this)

        var frt: FragmentTransaction = supportFragmentManager.beginTransaction()
        frt.replace(R.id.frgmCont2, fragment)
        frt.commit()
        Log.d("jij", "Fragment commiteed")

    }

    public fun dropFragment(listFragment: Boolean) {

        var frt: FragmentTransaction = supportFragmentManager.beginTransaction()

        if(listFragment){
            frt.remove(listFrag)
        } else {
            frt.remove(addImageFragment)
        }
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
                setupFragment(listFrag)
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
        //showEnterDataDialog()

        setupFragment(addImageFragment)


    }

     fun showImageDescriptionMain(name:String, default: Boolean ){
        if(default){

            val description = descriptionByName.get(name.substringBeforeLast("."))

            Log.d("pop", description.toString())

            image_desc.visibility = View.VISIBLE

            image_desc.bottom_sheet_description.text = description

            var newText = "Information about: <b> $name </b>"
            image_desc.bottom_sheet_topic.text = (Html.fromHtml(newText))
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

            var newText = "Description for image: <b> $name </b>"
            image_desc.bottom_sheet_topic.text = (Html.fromHtml(newText))
        }
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



    private fun showSwitchDialog() {
        val listItems = arrayOf("Custom", "Default")
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        mBuilder.setTitle("Choose an database")

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
                    Toast.makeText(this, "Changes will be committed after restart your app", Toast.LENGTH_LONG).show()
                }
                1 ->{
                    writeInPrefs("default")
                    Toast.makeText(this, "Changes will be committed after restart your app", Toast.LENGTH_LONG).show()
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
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE),
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
                            Toast.makeText(this,"You can't use app without CAMERA permission", Toast.LENGTH_LONG).show()
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

        descriptionByName.put("lays", "Web site: hßttps://lays.com \n\n " +
                "Video link: https://www.youtube.com/watch?v=lSaZkOX-FWw")
    }
}
