package com.firstclass.camera;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private Database database = new Database();
    private Map<String, String> docData = new HashMap<>();
    private androidx.camera.view.PreviewView viewFinder;
    private ImageView foto;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder =  findViewById(R.id.viewFinder);
        foto = findViewById(R.id.foto);

        //request de permissao
        if(allPermissionsGranted()){
            startCamera();
        } else {
            requestPermissions();
        }

        ImageButton lente = findViewById(R.id.lente);
        lente.setOnClickListener(v -> {
            if(cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA){
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            }else {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            }
            startCamera();
        });

        Button captureBt = findViewById(R.id.image_capture_button);
        captureBt.setOnClickListener(v -> {
            takePhoto();
        });

        foto.setOnClickListener(v -> {
            foto.setVisibility(View.INVISIBLE);
        });

        ImageButton gallery = findViewById(R.id.gallery);
        gallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            resultLauncherGallery.launch(intent);
        });

        Button firebase = findViewById(R.id.video_capture_button);
        firebase.setOnClickListener(v -> {
            foto.setVisibility(View.VISIBLE);
            database.downloadGallery(foto, Uri.parse(docData.get("url")));
        });
    }


    private void takePhoto(){
        if(imageCapture == null){
            return;
        }

        //definir nome e caminho
        String name = "IMG_SALAF_" + System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SALAF");

        //carregar imagem com as confgs
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        ).build();

        //Orientação da imagem
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;

                if(orientation >= 45 && orientation < 135){
                    rotation = Surface.ROTATION_270;

                } else if (orientation < 224) {
                    rotation = Surface.ROTATION_180;

                } else if (orientation < 314) {
                    rotation = Surface.ROTATION_90;

                } else {
                    rotation = Surface.ROTATION_0;
                }
                imageCapture.setTargetRotation(rotation);
            }
        };

        orientationEventListener.enable();



        //salvat imagem
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                foto.setVisibility(View.VISIBLE);
                foto.setImageURI(outputFileResults.getSavedUri());
                Toast.makeText(MainActivity.this, "FOTO SALVA", Toast.LENGTH_SHORT).show();
                database.uploadGallary(MainActivity.this, foto, docData);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("log", "ERRO AO CAPTURAR A IMAGEM" + exception.getMessage());
            }
        });

    }

    private void requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS);
    }
    private final ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                // Handle Permission granted/rejected
                boolean permissionGranted = true;
                for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                    if (Arrays.asList(REQUIRED_PERMISSIONS).contains(entry.getKey()) && !entry.getValue()) {
                        permissionGranted = false;
                        break;
                    }
                }
                if (!permissionGranted) {
                    Toast.makeText(getApplicationContext(),"Permissão NEGADA.",Toast.LENGTH_SHORT).show();
                } else {
                     startCamera();
                }
            });

    private boolean allPermissionsGranted(){
        for (String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    // Configura a camera
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // ImageCapture
                imageCapture = new ImageCapture.Builder().build();

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageCapture
                    );
                } catch (Exception exc) {
                    Log.e(TAG, "Camera binding failed", exc);
                }

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private final ActivityResultLauncher<Intent> resultLauncherGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    foto.setVisibility(View.VISIBLE);
                    foto.setImageURI(imageUri);
                }
        });
}