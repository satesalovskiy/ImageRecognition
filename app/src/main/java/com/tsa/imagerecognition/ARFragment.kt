package com.tsa.imagerecognition

import android.app.ActivityManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.core.exceptions.ImageInsufficientQualityException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import common.helpers.DatabaseHelper
import java.io.*

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
open class ARFragment : ArFragment() {

    private val DEFAULT_IMAGE_NAME = "/default.jpg"
    private val DEFAULT_IMAGE_DATABASE = "newdefault.imgdb"
    private val CUSTOM_IMAGE_DATABASE = "custom.imgdb"
    private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)
    private val MIN_OPENGL_VERSION = 3.0
    private val USE_PRELOAD_DB = true
    private var WHAT_DB_USE: String? = "defualt"

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoRenderable: ModelRenderable
    private lateinit var videoAnchorNode: AnchorNode
    private val databaseHelper: DatabaseHelper = DatabaseHelper(activity)

    private var activeAugmentedImage: AugmentedImage? = null
    public lateinit var augmentedImageDB: AugmentedImageDatabase


    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_FIRST_LAUNCH = "first_launch"
    private val APP_PREFERENCES_WHAT_DB_USE = "what_db_use"
    private val APP_PREFERENCES_CUSTOM_FIRST_TIME = "custom_first_time"
    private var isFirstLaunch: Boolean = true
    private var isCustomFirstTime: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer = MediaPlayer()

        val pref: SharedPreferences? = activity?.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)
        if (pref!!.contains(APP_PREFERENCES_WHAT_DB_USE)) {
            WHAT_DB_USE = pref.getString(APP_PREFERENCES_WHAT_DB_USE, "")
            Log.d("FIRST_LAUNCH", "We will use $WHAT_DB_USE")
        }
        if (pref!!.contains(APP_PREFERENCES_FIRST_LAUNCH)) {
            isFirstLaunch = pref.getBoolean(APP_PREFERENCES_FIRST_LAUNCH, false)
        }
        if (pref!!.contains(APP_PREFERENCES_CUSTOM_FIRST_TIME)) {
            isCustomFirstTime = pref.getBoolean(APP_PREFERENCES_CUSTOM_FIRST_TIME, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.planeRenderer.isEnabled = false
        arSceneView.isLightEstimationEnabled = false
        createArScene()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onStop() {
        super.onStop()
        //serialazeDB()
    }

    private fun serialazeDB() {
        //val file: File = File( ""+ activity?.getExternalFilesDir(null) + "/custom.imgdb")
        var file = File(Environment.getExternalStorageDirectory(), "custom.imgdb")

        val outputStr: FileOutputStream = FileOutputStream(file)
        augmentedImageDB.serialize(outputStr)
        outputStr.close()
    }

    public fun addNewImage(bitmap: Bitmap, name: String) {

        try {
            augmentedImageDB.addImage(name, bitmap)
        } catch (ex: ImageInsufficientQualityException) {
            Toast.makeText(activity, "Insufficient image quality, choose another image", Toast.LENGTH_LONG).show()
            return
        }

        serialazeDB()

        arSceneView.session?.apply {
            val changedConfig = config
            changedConfig.augmentedImageDatabase = augmentedImageDB
            configure(changedConfig)
        }

        Toast.makeText(activity, "Image added", Toast.LENGTH_LONG).show()
    }


    override fun getSessionConfiguration(session: Session): Config {
        val config = super.getSessionConfiguration(session)
        if (!setupAugmentedImageDatabase(config, session)) {
            Toast.makeText(activity, "Could not setup augmented image database", Toast.LENGTH_LONG).show()
        }
        config.focusMode = Config.FocusMode.AUTO
        return config
    }

    private fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {

        if (USE_PRELOAD_DB) {
            if (WHAT_DB_USE == "custom") {
                //val file: File = File( ""+ activity?.getExternalFilesDir(null) + "/custom.imgdb")

                var file = File(Environment.getExternalStorageDirectory(), "custom.imgdb")



                var inputStr: InputStream?

                try {
                    FileInputStream(file)
                } catch (ex: FileNotFoundException){
                    Toast.makeText(activity,"Unable to get access to storage. Check storage permissions", Toast.LENGTH_LONG).show()
                }

                if (isCustomFirstTime) {
                    inputStr = context?.assets?.open(CUSTOM_IMAGE_DATABASE)
                    val pref: SharedPreferences? = activity?.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)
                    val editor = pref!!.edit()
                    editor.putBoolean(APP_PREFERENCES_CUSTOM_FIRST_TIME, false)
                    editor.apply()
                    Log.d("FIRST_LAUNCH", "in first")
                } else {
                    if (file.exists()) {
                        inputStr = FileInputStream(file)
                        Log.d("FIRST_LAUNCH", "in second 1")
                    } else {
                        file.mkdir()
                        inputStr = FileInputStream(file)
                        Log.d("FIRST_LAUNCH", "in second 2")
                    }
                }
                augmentedImageDB = AugmentedImageDatabase.deserialize(session, inputStr)

                Log.d("Jopka", augmentedImageDB.numImages.toString())

                config.augmentedImageDatabase = augmentedImageDB
                return true
            } else {
                val inputStr: InputStream? = context?.assets?.open(DEFAULT_IMAGE_DATABASE)
                augmentedImageDB = AugmentedImageDatabase.deserialize(session, inputStr)
                config.augmentedImageDatabase = augmentedImageDB
                return true
            }
        } else {
//            try {
//                config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->
//                    db.addImage(TEST_VIDEO_1, loadAugmentedImageBitmap(TEST_IMAGE_1))
//                    db.addImage(TEST_VIDEO_2, loadAugmentedImageBitmap(TEST_IMAGE_2))
//                    db.addImage(TEST_VIDEO_3, loadAugmentedImageBitmap(TEST_IMAGE_3))
//                }
//                return true
//            } catch (e: IllegalArgumentException) {
//                Log.e(TAG, "Could not add bitmap to augmented image database")
//            } catch (e: IOException) {
//                Log.e(TAG, "IO exception loading augmented image bitmap.")
//            }
            return false
        }
    }

    private fun loadAugmentedImageBitmap(imageName: String): Bitmap =
            requireContext().assets.open(imageName).use { return BitmapFactory.decodeStream(it) }

    private fun createArScene() {
        externalTexture = ExternalTexture().also {
            mediaPlayer.setSurface(it.surface)
        }

        ModelRenderable.builder()
                .setSource(requireContext(), R.raw.augmented_video_model)
                .build()
                .thenAccept { renderable ->
                    videoRenderable = renderable
                    renderable.isShadowCaster = false
                    renderable.isShadowReceiver = false
                    renderable.material.setExternalTexture("videoTexture", externalTexture)
                    renderable.material.setFloat4("keyColor", CHROMA_KEY_COLOR)
                }
                .exceptionally {
                    Log.e(TAG, "Could not create ModelRenderable")
                    return@exceptionally null
                }

        videoAnchorNode = AnchorNode().apply {
            setParent(arSceneView.scene)
        }
    }

    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView.arFrame ?: return

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

       // Log.d("pipka", updatedAugmentedImages.size.toString())

        val nonFullTrackingImages = updatedAugmentedImages.filter { it.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING }

        val pausedImages = updatedAugmentedImages.filter { it.trackingState == TrackingState.PAUSED }

        if(pausedImages.isNotEmpty()) {
            val act = activity as MainActivity
            act.showMovePhoneAnimation()
        }


        activeAugmentedImage?.let { activeAugmentedImage ->
            if (isArVideoPlaying() && nonFullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                pauseArVideo()
            }
        }

        val fullTrackingImages = updatedAugmentedImages.filter { it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING }
        if (fullTrackingImages.isEmpty()) return



        activeAugmentedImage?.let { activeAugmentedImage ->
            if (fullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                if (!isArVideoPlaying()) {
                    resumeArVideo()
                }
                return
            }
        }

        fullTrackingImages.firstOrNull()?.let { augmentedImage ->
            try {
                showImageDescription(augmentedImage)
                playbackArVideo(augmentedImage)

            } catch (e: Exception) {
                Log.e(TAG, "Could not play video [${augmentedImage.name}]")
            }
        }
    }

    private fun showImageDescription(augmentedImage: AugmentedImage){
        var act = activity as MainActivity
        if(WHAT_DB_USE == "custom"){
            act.showImageDescriptionMain(augmentedImage.name, false)
        } else {
            act.showImageDescriptionMain(augmentedImage.name, true)
        }
    }


    private fun isArVideoPlaying() = mediaPlayer.isPlaying

    private fun pauseArVideo() {
        videoAnchorNode.renderable = null
        mediaPlayer.pause()
    }

    private fun resumeArVideo() {
        mediaPlayer.start()
        videoAnchorNode.renderable = videoRenderable
    }

    private fun dismissArVideo() {
        videoAnchorNode.anchor?.detach()
        videoAnchorNode.renderable = null
        activeAugmentedImage = null
        mediaPlayer.reset()
    }

    private fun playbackArVideo(augmentedImage: AugmentedImage) {

        var act = activity as MainActivity
        act.stopMovePhoneAnimation()

        if (WHAT_DB_USE != "custom") {
            val videoName = "videos/"+augmentedImage.name.substringBeforeLast('.') + ".mp4"
            requireContext().assets.openFd(videoName)
                    .use { descriptor ->
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(descriptor)
                    }.also {
                        mediaPlayer.isLooping = true
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    }
        } else {
            val videoName = augmentedImage.name.substringBeforeLast('.') + ".mp4"
            mediaPlayer.reset()

            var file = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + videoName)
            if(!file.exists()){
                Toast.makeText(activity, "Could not find video file! Try to re-create augmented image", Toast.LENGTH_LONG).show()
            } else {
                mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().absolutePath + File.separator + videoName)
                mediaPlayer.isLooping = true
                mediaPlayer.prepare()
                mediaPlayer.start()
            }
        }

        videoAnchorNode.anchor?.detach()
        videoAnchorNode.anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
        videoAnchorNode.localScale = Vector3(
                augmentedImage.extentX,
                1.0f,
                augmentedImage.extentZ
        )

        activeAugmentedImage = augmentedImage

        externalTexture.surfaceTexture.setOnFrameAvailableListener {
            it.setOnFrameAvailableListener(null)
            videoAnchorNode.renderable = videoRenderable
        }
    }

    override fun onPause() {
        super.onPause()
        dismissArVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Check for Sceneform being supported on this device.  This check will be integrated into
        // Sceneform eventually.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")

          Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
        }

        val openGlVersionString = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "ArVideoFragment"
    }
}