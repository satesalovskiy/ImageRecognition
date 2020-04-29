package com.tsa.imagerecognition


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.squareup.picasso.Picasso
import common.helpers.DatabaseHelper
import kotlinx.android.synthetic.main.fragment_add_new_images.*
import kotlinx.android.synthetic.main.fragment_add_new_images.view.*
import java.io.File
import java.io.FileOutputStream

/**
 * A simple [Fragment] subclass.
 */
class AddNewImagesFragment : Fragment() {

    private lateinit var pickedImage: Bitmap
    private lateinit var pickedVideoPath: String
    private lateinit var mDatabaseHelper: DatabaseHelper


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mDatabaseHelper = DatabaseHelper(activity)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_new_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edit_image.setOnClickListener{
            chooseImage()
        }
        edit_video.setOnClickListener{
            chooseVideo()
        }

        applyAdding.setOnClickListener{

            val name = view.edit_name.text.toString()
            val description = view.edit_description.text.toString()

            if(name.isEmpty() || !this::pickedImage.isInitialized || !this::pickedVideoPath.isInitialized || description.isEmpty()){
                Toast.makeText(activity, "You forgot to initialize something!", Toast.LENGTH_LONG).show()
            } else {
                saveEverythingInStorage(name, description)
            }
        }

        closeAdding.setOnClickListener{
            var act = activity as MainActivity
            act.dropFragment(false)
        }



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

                    Picasso.get()
                            .load(R.drawable.defaultimage2_ready)
                            .fit()
                            .centerInside()
                            .into(edit_image)

                    val selectedImage = data.data
                    val bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, selectedImage)
                    pickedImage = bitmap
                    Toast.makeText(activity, "Image picked", Toast.LENGTH_LONG).show()

                }
            }
            222 -> {
                if(resultCode == Activity.RESULT_OK && data != null){

                    Picasso.get()
                            .load(R.drawable.defaultvideo_ready)
                            .fit()
                            .centerInside()
                            .into(edit_video)

                    val selectedVideo = data.data
                    val selectedVideoPath = getPath(selectedVideo!!)
                    pickedVideoPath = selectedVideoPath
                    Log.d("path ",selectedVideoPath)
                    Toast.makeText(activity, "video picked", Toast.LENGTH_LONG).show()
                    //saveVideoToInternalStorage(selectedVideoPath)
                }
            }
        }
    }


    private fun getPath(uri: Uri): String {
        val projection = arrayOf(MediaStore.Video.Media.DATA)

        //Cursor cursor = getContentResolver().query(uri, projection, null, null, null)

        val cursor = activity?.contentResolver?.query(uri,projection, null, null, null)

        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA

            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else {
            Toast.makeText(activity, "Cursor is null", Toast.LENGTH_LONG).show()
            return ""
        }


    }

    private fun saveEverythingInStorage(name: String, description: String) {
        addDataToSQL(name, description)
        saveImageToDB(name)
        saveVideoToInternalStorage(name)
    }

    private fun addDataToSQL(name: String, description: String) {
        var insertData = mDatabaseHelper.addData(name, description)

        if(insertData){
            Toast.makeText(activity, "Data added", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(activity, "Failed", Toast.LENGTH_LONG).show()
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

    private fun saveImageToDB (name: String) {

        var act = activity as MainActivity
        act.arFragment.addNewImage(pickedImage, name)
    }


}
