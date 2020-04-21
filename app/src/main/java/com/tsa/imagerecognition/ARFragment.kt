package com.tsa.imagerecognition

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import common.helpers.SnackbarHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import java.io.*

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
open class ARFragment : ArFragment() {

    private val DEFAULT_IMAGE_NAME = "images/default.jpg"
    private val DEFAULT_IMAGE_DATABASE = "imagedb.imgdb"
    private val CUSTOM_IMAGE_DATABASE = "custom.imgdb"
    private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)
    private val MIN_OPENGL_VERSION = 3.0
    private val USE_PRELOAD_DB = true
    private lateinit var WHAT_DB_USE: String

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoRenderable: ModelRenderable
    private lateinit var videoAnchorNode: AnchorNode

    private var activeAugmentedImage: AugmentedImage? = null
    private lateinit var augmentedImageDB: AugmentedImageDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer = MediaPlayer()

        WHAT_DB_USE = "custom"
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

    public fun addNewImage(){
        val file: File = File( ""+ activity?.getExternalFilesDir(null) + "/custom.imgdb")
        val outputStr: FileOutputStream = FileOutputStream(file)
        augmentedImageDB.addImage("video1.png", loadAugmentedImageBitmap(TEST_IMAGE_2))
        augmentedImageDB.serialize(outputStr)
        outputStr.close()
        Toast.makeText(activity, "Image added", Toast.LENGTH_LONG).show()
    }

    override fun getSessionConfiguration(session: Session): Config {
        val config = super.getSessionConfiguration(session)
        if (!setupAugmentedImageDatabase(config, session)) {
            SnackbarHelper.getInstance()
                    .showError(activity, "Could not setup augmented image database")
        }
        config.focusMode = Config.FocusMode.AUTO
        return config
    }

    private fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {

        if (USE_PRELOAD_DB) {
            if(WHAT_DB_USE == "custom"){

                //Вот так при первом запуске нужно делать!!
               // val inputStr: InputStream? = context?.assets?.open(CUSTOM_IMAGE_DATABASE)

                val file: File = File( ""+ activity?.getExternalFilesDir(null) + "/custom.imgdb")
                val inputStr: FileInputStream = FileInputStream(file)
                augmentedImageDB = AugmentedImageDatabase.deserialize(session, inputStr)

                config.augmentedImageDatabase = augmentedImageDB
                return true
            } else {
                val inputStr: InputStream? = context?.assets?.open(DEFAULT_IMAGE_DATABASE)
                augmentedImageDB = AugmentedImageDatabase.deserialize(session, inputStr)
                config.augmentedImageDatabase = augmentedImageDB
                return true
            }
        } else {
            try {
                config.augmentedImageDatabase = AugmentedImageDatabase(session).also { db ->
                    db.addImage(TEST_VIDEO_1, loadAugmentedImageBitmap(TEST_IMAGE_1))
                    db.addImage(TEST_VIDEO_2, loadAugmentedImageBitmap(TEST_IMAGE_2))
                    db.addImage(TEST_VIDEO_3, loadAugmentedImageBitmap(TEST_IMAGE_3))
                }
                return true
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Could not add bitmap to augmented image database")
            } catch (e: IOException) {
                Log.e(TAG, "IO exception loading augmented image bitmap.")
            }
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
                .exceptionally { Log.e(TAG, "Could not create ModelRenderable")
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
                playbackArVideo(augmentedImage)
            } catch (e: Exception) {
                Log.e(TAG, "Could not play video [${augmentedImage.name}]")
            }
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

        val videoName = augmentedImage.name.substringBeforeLast('.') + ".mp4"

        requireContext().assets.openFd(videoName)
                .use { descriptor ->
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(descriptor)
                }.also {
                    mediaPlayer.isLooping = true
                    mediaPlayer.prepare()
                    mediaPlayer.start()
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

            SnackbarHelper.getInstance()
                    .showError(activity, "Sceneform requires Android N or later")
        }

        val openGlVersionString = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later")
            SnackbarHelper.getInstance()
                    .showError(activity, "Sceneform requires OpenGL ES 3.0 or later")
        }
    }

    companion object {
        private const val TAG = "ArVideoFragment"

        private const val TEST_IMAGE_1 = "0.jpg"
        private const val TEST_IMAGE_2 = "1.jpg"
        private const val TEST_IMAGE_3 = "2.jpg"

        private const val TEST_VIDEO_1 = "video0.mp4"
        private const val TEST_VIDEO_2 = "video1.mp4"
        private const val TEST_VIDEO_3 = "video2.mp4"
    }
}