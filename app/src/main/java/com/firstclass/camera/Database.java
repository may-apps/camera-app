package com.firstclass.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class Database {
    public void uploadGallary(Context context, ImageView foto, Map<String, String> docData){

        Bitmap bitmap = ((BitmapDrawable) foto.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] databyte = baos.toByteArray();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storage.getReference("galeria").child("fotoF_"+ System.currentTimeMillis()+".jpg")
                .putBytes(databyte).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(context, "Upload feito com sucesso!", Toast.LENGTH_SHORT).show();
                        taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                docData.put("url", uri.toString());
                                Toast.makeText(context, uri.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    public void downloadGallery(ImageView img, Uri urlFirebase) {
        img.setRotation(0);
        Glide.with(img.getContext()).asBitmap().load(urlFirebase).into(img);
    }
}
