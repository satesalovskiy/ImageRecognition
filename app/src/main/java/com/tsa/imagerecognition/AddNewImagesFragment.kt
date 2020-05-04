package com.tsa.imagerecognition

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.squareup.picasso.Picasso
import common.helpers.DatabaseHelper
import kotlinx.android.synthetic.main.fragment_add_new_images.*
import kotlinx.android.synthetic.main.fragment_add_new_images.view.*
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream

class AddNewImagesFragment : Fragment() {

    private lateinit var pickedImage: Bitmap
    private lateinit var pickedVideoPath: String
    private lateinit var mDatabaseHelper: DatabaseHelper
    private lateinit var videoUri: Uri

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_new_images, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDatabaseHelper = DatabaseHelper(activity)

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
                Toast.makeText(activity, R.string.add_new_image_fragment_forgot_initialize, Toast.LENGTH_LONG).show()
            } else {
                view.edit_name.setText("")
                view.edit_description.setText("")
                saveEverythingInStorage(name, description)
            }
        }

        closeAdding.setOnClickListener{
            val act = activity as MainActivity
            act.dropFragment(false)
        }
    }

    private fun chooseVideo() {
        val videoPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
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
                    Toast.makeText(activity, R.string.add_new_image_fragment_image_picked, Toast.LENGTH_LONG).show()
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
                    val selectedVideoPath = selectedVideo?.path
                    pickedVideoPath = selectedVideoPath!!
                    videoUri = data.data!!
                    Toast.makeText(activity, R.string.add_new_image_fragment_video_picked, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveEverythingInStorage(name: String, description: String) {
        addDataToSQL(name, description)
        saveImageToDB(name)
        saveVideoToExternalStorage(name, videoUri)
    }

    private fun addDataToSQL(name: String, description: String) {
        val insertData = mDatabaseHelper.addData(name, description)

        if(insertData){
            Toast.makeText(activity, R.string.add_new_image_fragment_data_added, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(activity, R.string.add_new_image_fragment_data_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveVideoToExternalStorage (name: String, uri: Uri) {

        val resultFile = File(context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "$name.mp4")

        val outputLL = FileOutputStream(resultFile)

        val inputStream = context?.contentResolver?.openInputStream(uri)

        IOUtils.copy(inputStream, outputLL)
    }

    private fun saveImageToDB (name: String) {

        val act = activity as MainActivity
        act.arFragment.addNewImage(pickedImage, name)
    }
}
