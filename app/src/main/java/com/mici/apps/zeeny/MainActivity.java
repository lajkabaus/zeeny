package com.mici.apps.zeeny;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    ImageView image;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
        setContentView(R.layout.activity_main);

        /** helpful references */
        Button click = (Button)findViewById(R.id.sendButton);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage();
            }
        });

        image = (ImageView)findViewById(R.id.cameraView);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }

    /**
     * mici API
     */

    public void sendImage() {
        Log.i("Send email", "");

        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        EditText doctorEditText = (EditText)findViewById(R.id.doctorEmailEditText);

        String to [] = { doctorEditText.getText().toString() };
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, to );
        emailIntent.putExtra(Intent.EXTRA_CC, "");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Slika grla");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "evo slike grla"+"\n\n"+"Mici");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send e-mail with"));
            finish();
            Log.i("Sending email done", "");

        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "There is no e-mail client installed.", Toast.LENGTH_SHORT).show();
        }

//        emailIntent.setType("image/jpeg");
//        File bitmapFile = new File(Environment.getExternalStorageDirectory()+
//                "/"+FOLDER_NAME+"/picture.jpg");
//        myUri = Uri.fromFile(bitmapFile);
//        emailIntent.putExtra(Intent.EXTRA_STREAM, myUri);
    }

    /**
     * google API
     */

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            image.setImageBitmap(imageBitmap);
        }
    }
}
