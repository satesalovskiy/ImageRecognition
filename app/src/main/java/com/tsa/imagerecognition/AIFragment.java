package com.tsa.imagerecognition;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import common.helpers.SnackbarHelper;

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
public class AIFragment extends ArFragment {
    private static final String TAG = "AugmentedImageFragment";

    // This is the name of the image in the sample database.  A copy of the image is in the assets
    // directory.  Opening this image on your computer is a good quick way to test the augmented image
    // matching.
    private static final String DEFAULT_IMAGE_NAME = "images/default.jpg";
    private static final int IMAGE_NUMBER = 6;


    private static final float VIDEO_HEIGHT_METERS = 0.85f;

    // This is a pre-created database containing the sample image.
    private static final String SAMPLE_IMAGE_DATABASE = "sample_database.imgdb";

    private static final String MY_IMAGE_DATABASE = "imagedb.imgdb";

    // Augmented image configuration and rendering.
    // Load a single image (true) or a pre-generated image database (false).
    private static final boolean USE_SINGLE_IMAGE = false;

    private static final Color CHROMA_KEY_COLOR = new Color(0.1843f, 1.0f, 0.098f);

    // Do a runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
    // application.
    private static final double MIN_OPENGL_VERSION = 3.0;


    private final Map<AugmentedImage, AnchorNode> augmentedImageMap = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private ExternalTexture externalTexture;
    private ModelRenderable videoRenderable;
    private String[] names;
    private int[] movieUrls;
    private StorageReference mStorageRef;
    private HashMap<String, Integer> moviesMap;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        names = new String[IMAGE_NUMBER];
        movieUrls = new int[IMAGE_NUMBER];
        moviesMap = new HashMap<>();

        for(int i=0; i<IMAGE_NUMBER; i++){
            names[i] = i+".jpg";
        }
        movieUrls[0] = R.raw.vidoe0;
        movieUrls[1] =  R.raw.video1;
        movieUrls[2] = R.raw.video2;
        movieUrls[3] = R.raw.video3;
        movieUrls[4] = R.raw.video4;
        movieUrls[5] = R.raw.video5;
        moviesMap.put(names[0], movieUrls[0]);
        moviesMap.put(names[1], movieUrls[1]);
        moviesMap.put(names[2], movieUrls[2]);
        moviesMap.put(names[3], movieUrls[3]);
        moviesMap.put(names[4], movieUrls[4]);
        moviesMap.put(names[5], movieUrls[5]);

    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Turn off the plane discovery since we're only looking for images
        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);

        //Texture that will contain a video
        externalTexture = new ExternalTexture();

        //Media player always shows one video :(


        //3D model that is used as a canvas
        ModelRenderable.builder()
                .setSource(getContext(), R.raw.augmented_video_model)
                .build()
                .thenAccept(renderable -> {
                    videoRenderable = renderable;
                    renderable.getMaterial().setExternalTexture("videoTexture", externalTexture);
                    renderable.getMaterial().setFloat4("keyColor", CHROMA_KEY_COLOR);
                })
                .exceptionally(
                        throwable -> {
                            Toast toast = Toast.makeText(getContext(), "Unable to load video renderable", Toast.LENGTH_SHORT);
                            toast.show();
                            return null;
                        });

        return view;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = getArSceneView().getArFrame();

        // If there is no frame, just return.
        if (frame == null) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.

                    String text = "Detected Image " + augmentedImage.getName();
                    SnackbarHelper.getInstance().showMessage(getActivity(), text);
                    break;

                case TRACKING:
                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        //New node with anchor in the center of the detected image
                        AnchorNode anchorNode = new AnchorNode();
                        anchorNode.setAnchor(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
                        anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1.0f, augmentedImage.getExtentZ()));
                        anchorNode.setParent(getArSceneView().getScene());

                        //Node for media player
                        Node videoNode = new Node();
                        videoNode.setParent(anchorNode);

                        augmentedImageMap.put(augmentedImage, anchorNode);

                        int res = moviesMap.get(augmentedImage.getName());

                        createPlayer(res, augmentedImage.getName());

                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();
                            externalTexture
                                    .getSurfaceTexture()
                                    .setOnFrameAvailableListener(
                                            (SurfaceTexture surfaceTexture) -> {
                                                videoNode.setRenderable(videoRenderable);
                                                externalTexture.getSurfaceTexture().setOnFrameAvailableListener(null);
                                            });

                            Toast toast = Toast.makeText(getContext(), "Video is situated! For augmented image " + augmentedImage.getIndex(), Toast.LENGTH_LONG);
                            toast.show();
                        } else {
                            videoNode.setRenderable(videoRenderable);
                        }
                    }
                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    break;
            }
        }
    }

    private void createPlayer(int resId, String name) {

//
//        final StorageReference imagesRef = mStorageRef.child("movies/"+name);
//
//        imagesRef.getDownloadUrl();
//
//        Task uploadTask = imagesRef.getDownloadUrl();
//
//        uploadTask.addOnCompleteListener(new OnCompleteListener<Uri>() {
//            @Override
//            public void onComplete(@NonNull Task<Uri> task) {
//                if(task.isSuccessful()){
//                    Uri downloadUri = task.getResult();
//                    Log.d("JOPAPOPOPPOPO", downloadUri.toString());
//                }
//            }
//        });

//        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//            @Override
//            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                if (!task.isSuccessful()) {
//                    throw task.getException();
//                }
//
//                // Continue with the task to get the download URL
//                return imagesRef.getDownloadUrl();
//            }
//        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//            @Override
//            public void onComplete(@NonNull Task<Uri> task) {
//                if (task.isSuccessful()) {
//                    Uri downloadUri = task.getResult();
//
//                    if (downloadUri != null) {
//
//                        //=====================
//                        String photoStringLink = downloadUri.toString();
//                        setImageUrl(photoStringLink);
//
//                    }
//
//                }
//            }
//        });





       // Log.d("JOPAPOPA", url);
        mediaPlayer = MediaPlayer.create(getContext(), resId);
        mediaPlayer.setSurface(externalTexture.getSurface());
        mediaPlayer.setLooping(true);
    }


    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        if (!setupAugmentedImageDatabase(config, session)) {
            SnackbarHelper.getInstance()
                    .showError(getActivity(), "Could not setup augmented image database");
        }
        config.setFocusMode(Config.FocusMode.AUTO);
        return config;
    }

    private boolean setupAugmentedImageDatabase(Config config, Session session) {
        AugmentedImageDatabase augmentedImageDatabase;

        AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
        if (assetManager == null) {
            Log.e(TAG, "Context is null, cannot intitialize image database.");
            return false;
        }

        if (USE_SINGLE_IMAGE) {
            Bitmap augmentedImageBitmap = loadAugmentedImageBitmap(assetManager);
            if (augmentedImageBitmap == null) {
                return false;
            }
            augmentedImageDatabase = new AugmentedImageDatabase(session);
            augmentedImageDatabase.addImage(DEFAULT_IMAGE_NAME, augmentedImageBitmap);
        } else {
            try (InputStream is = getContext().getAssets().open(MY_IMAGE_DATABASE)) {
                augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
            } catch (IOException e) {
                Log.e(TAG, "IO exception loading augmented image database.", e);
                return false;
            }
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Bitmap loadAugmentedImageBitmap(AssetManager assetManager) {
        try (InputStream is = assetManager.open(DEFAULT_IMAGE_NAME)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Check for Sceneform being supported on this device.  This check will be integrated into
        // Sceneform eventually.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");

            SnackbarHelper.getInstance()
                    .showError(getActivity(), "Sceneform requires Android N or later");
        }

        String openGlVersionString =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
            SnackbarHelper.getInstance()
                    .showError(getActivity(), "Sceneform requires OpenGL ES 3.0 or later");
        }
    }
}
