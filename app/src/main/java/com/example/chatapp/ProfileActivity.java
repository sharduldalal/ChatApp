package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.media.MediaBrowserService;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.net.URI;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    CircleImageView image_profile;
    TextView username;

    DatabaseReference reference;
    FirebaseUser fuser;

    StorageReference storageReference;
    private static int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uplaodTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile2);

        image_profile = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        storageReference = FirebaseStorage.getInstance().getReference("uploads;");

        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    image_profile.setImageResource(R.mipmap.ic_launcher);
                }
                else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(image_profile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

    }

    private void openImage() {
        Intent intent =new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent , IMAGE_REQUEST);

    }

    public String getFileExtension(Uri uri){
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
    private Void uploadImage(){
        final ProgressDialog pd = new ProgressDialog(getApplicationContext());
        //pd.setMessage("Uploading");
        //pd.show();

        if (imageUri != null){
            final StorageReference fileRefrence = storageReference.child(System.currentTimeMillis()
                    + "." + getFileExtension(imageUri));

            uplaodTask = fileRefrence.putFile(imageUri);
            uplaodTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot , Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }

                    return fileRefrence.getDownloadUrl();

                }
            }).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Uri downloadUri = (Uri) task.getResult();//*******
                        String mUri = downloadUri.toString();

                        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                        HashMap<String , Object> map = new HashMap<>();
                        map.put("imageURL" , mUri);
                        reference.updateChildren(map);

                        pd.dismiss();

                    }
                    else {
                        Toast.makeText(getApplicationContext() , "Failed!" , Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage() , Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        }
        else {
            Toast.makeText(getApplicationContext() , "No image Selected" , Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){

            imageUri = data.getData();

            if (uplaodTask != null && uplaodTask.isInProgress()){

                Toast.makeText(getApplicationContext() , "Upload in progress" , Toast.LENGTH_SHORT).show();

            }
            else {
                uploadImage();
            }
        }
    }
}
