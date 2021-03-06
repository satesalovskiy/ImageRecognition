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
import java.io.*

class ARFragment : ArFragment() {

    private val APP_PREFERENCES = "image_recognition_prefs"
    private val APP_PREFERENCES_FIRST_LAUNCH = "first_launch"
    private val APP_PREFERENCES_WHAT_DB_USE = "what_db_use"
    private val APP_PREFERENCES_CUSTOM_FIRST_TIME = "custom_first_time"
    private val DEFAULT_IMAGE_DATABASE = "newdefault2.imgdb"
    private val CUSTOM_IMAGE_DATABASE = "custom.imgdb"
    private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)
    private val MIN_OPENGL_VERSION = 3.0
    private var WHAT_DB_USE: String? = "default"

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoRenderable: ModelRenderable
    private lateinit var videoAnchorNode: AnchorNode
    private lateinit var augmentedImageDB: AugmentedImageDatabase

    private var activeAugmentedImage: AugmentedImage? = null
    private var isFirstLaunch: Boolean = true
    private var isCustomFirstTime: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaPlayer = MediaPlayer()

        val pref: SharedPreferences? = activity?.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)
        if (pref!!.contains(APP_PREFERENCES_WHAT_DB_USE)) {
            WHAT_DB_USE = pref.getString(APP_PREFERENCES_WHAT_DB_USE, "")
        }
        if (pref.contains(APP_PREFERENCES_FIRST_LAUNCH)) {
            isFirstLaunch = pref.getBoolean(APP_PREFERENCES_FIRST_LAUNCH, false)
        }
        if (pref.contains(APP_PREFERENCES_CUSTOM_FIRST_TIME)) {
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

    private fun serialazeDB() {

        val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "custom.imgdb")
        val outputStr = FileOutputStream(file)
        augmentedImageDB.serialize(outputStr)
        outputStr.close()
    }

    fun addNewImage(bitmap: Bitmap, name: String) {

        try {
            augmentedImageDB.addImage(name, bitmap)
        } catch (ex: ImageInsufficientQualityException) {
            Toast.makeText(activity, R.string.ar_fragment_image_quality, Toast.LENGTH_LONG).show()
            return
        }

        serialazeDB()

        arSceneView.session?.apply {
            val changedConfig = config
            changedConfig.augmentedImageDatabase = augmentedImageDB
            configure(changedConfig)
        }

        Toast.makeText(activity, R.string.ar_fragment_image_added, Toast.LENGTH_LONG).show()
    }

    override fun getSessionConfiguration(session: Session): Config {
        val config = super.getSessionConfiguration(session)
        if (!setupAugmentedImageDatabase(config, session)) {
            Toast.makeText(activity, R.string.ar_fragment_error_setup_database, Toast.LENGTH_LONG).show()
        }
        config.focusMode = Config.FocusMode.AUTO
        return config
    }

    private fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {

        if (WHAT_DB_USE == "custom") {

            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "custom.imgdb")

            val inputStr: InputStream?

            try {
                FileInputStream(file)
            } catch (ex: FileNotFoundException) {
                if (!isCustomFirstTime)
                    Toast.makeText(activity, R.string.ar_fragment_error_access_to_storage, Toast.LENGTH_LONG).show()
            }

            if (isCustomFirstTime) {
                inputStr = context?.assets?.open(CUSTOM_IMAGE_DATABASE)
                val pref: SharedPreferences? = activity?.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE)
                val editor = pref!!.edit()
                editor.putBoolean(APP_PREFERENCES_CUSTOM_FIRST_TIME, false)
                editor.apply()

            } else {
                if (file.exists()) {
                    inputStr = FileInputStream(file)
                } else {
                    file.mkdirs()
                    inputStr = FileInputStream(file)
                }
            }

            augmentedImageDB = AugmentedImageDatabase.deserialize(session, inputStr)

            if (isCustomFirstTime)
                serialazeDB()

            config.augmentedImageDatabase = augmentedImageDB
            return true
        } else {
            val inputStr: InputStream? = context?.assets?.open(DEFAULT_IMAGE_DATABASE)
            augmentedImageDB = AugmentedImageDatabase.deserialize(session, inputStr)
            config.augmentedImageDatabase = augmentedImageDB
            return true
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
                    return@exceptionally null
                }

        videoAnchorNode = AnchorNode().apply {
            setParent(arSceneView.scene)
        }
    }

    override fun onUpdate(frameTime: FrameTime) {

        val frame = arSceneView.arFrame ?: return

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

        val nonFullTrackingImages = updatedAugmentedImages.filter { it.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING }

        val pausedImages = updatedAugmentedImages.filter { it.trackingState == TrackingState.PAUSED }

        if (pausedImages.isNotEmpty()) {
            val act = activity as MainActivity
            act.showMovePhoneAnimation()
            showImageDescription(pausedImages[0])
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
            }
        }
    }

    private fun showImageDescription(augmentedImage: AugmentedImage) {
        val act = activity as MainActivity
        if (WHAT_DB_USE == "custom") {
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

        val act = activity as MainActivity
        act.stopMovePhoneAnimation()

        if (WHAT_DB_USE != "custom") {
            val videoName = "videos/" + augmentedImage.name.substringBeforeLast('.') + ".mp4"
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

            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES), videoName)

            if (!file.exists()) {
                Toast.makeText(activity, R.string.ar_fragment_error_find_video, Toast.LENGTH_LONG).show()
            } else {
                mediaPlayer.setDataSource(context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath + File.separator + videoName)
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(activity, R.string.ar_fragment_sceneform_requires_android, Toast.LENGTH_LONG).show()
        }

        val openGlVersionString = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Toast.makeText(activity, R.string.ar_fragment_sceneform_requires_opengl, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "ArVideoFragment"
    }
}