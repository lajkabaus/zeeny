package com.mici.apps.zeeny;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    ImageView image;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PICK_CONTACT_REQUEST = 1;

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
                takeImage();
            }
        });

//        EditText drInput = (EditText)findViewById(R.id.doctorEmailEditText);
//        drInput.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                pickContact();
//            }
//        });
    }

    /**
     * mici API
     */

    public void pickContact() {
        Log.i("Pick contact!", "");
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    public void sendImage() {
        Log.i("Send email", "");

        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        EditText drInput = (EditText)findViewById(R.id.doctorEmailEditText);

        String to [] = { drInput.getText().toString() };
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("image/jpeg");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, to );
        emailIntent.putExtra(Intent.EXTRA_CC, "");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Slika grla");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "evo slike grla"+"\n\n"+"Mici");

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        String fname = "Slika grla" + ".jpg";
        File bitmapFile = new File(myDir, fname);

        Uri myUri = Uri.fromFile(bitmapFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, myUri);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send e-mail with"));
            finish();
            Log.i("Sending email done", "");

        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "There is no e-mail client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public static File saveBitmap(Bitmap bmp, String imageName) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        String fname = imageName + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);

        try {
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.flush();
            fo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * google API
     */

    public void takeImage() {
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

            /** save to file */
            File newfile;
            try {
                newfile = saveBitmap(imageBitmap, "slika_grla");
                Toast.makeText(MainActivity.this,
                        "Image saved with filename: " + newfile.getName().toString()
                        , Toast.LENGTH_SHORT).show();
            } catch (IOException ioe) {
                Toast.makeText(MainActivity.this,
                        "File I/O Error!", Toast.LENGTH_SHORT).show();
            }
        }

        else if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            Toast.makeText(MainActivity.this,
                    "HOORAY!", Toast.LENGTH_SHORT).show();
            sendImage();
        }

        else {
            Toast.makeText(MainActivity.this,
                    "BOO!", Toast.LENGTH_SHORT).show();
        }

    }
}
