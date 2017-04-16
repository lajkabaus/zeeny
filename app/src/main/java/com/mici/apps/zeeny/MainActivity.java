package com.mici.apps.zeeny;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{
    ImageView image;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int PICK_CONTACT_REQUEST = 1;

    boolean _isPictureTaken   = false;
    boolean _isFlashAvailable = false;
    boolean _isFlashOn        = false;

    private static CameraManager _cameraManager = null;
    private static String _cameraId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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

        initFlash();

        image = (ImageView)findViewById(R.id.cameraView);
        image.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                takeImage();
            }
        });

        final EditText drInput = (EditText)findViewById(R.id.doctorEmailEditText);
        drInput.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER))
                {
                    if ( true == drInput.getText().toString().isEmpty() ) {
                        pickContact();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * mici API
     */

    /**
     * pick the contact from the contact-list
     */
    public void pickContact()
    {
        Log.i("Pick contact!", "");
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE); // Show user only contacts w/ phone numbers
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    /**
     * send the image via e-mail client
     */
    public void sendImage()
    {
        if ( false == _isPictureTaken )
        {
            return;
        }

        Log.i("Send email", "");

        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        EditText drInput = (EditText)findViewById(R.id.doctorEmailEditText);

        String to [] = { drInput.getText().toString() };
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("image/jpeg");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, to );
        emailIntent.putExtra(Intent.EXTRA_CC, "");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Slika grla");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Evo slike grla."+"\n\n"+"Mici");

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        String fname = "Slika grla" + ".jpg";
        File bitmapFile = new File(myDir, fname);

        Uri myUri = Uri.fromFile(bitmapFile);
        emailIntent.putExtra(Intent.EXTRA_STREAM, myUri);

        try
        {
            startActivity(Intent.createChooser(emailIntent, "Send e-mail with"));
            finish();
            Log.i("Sending email done", "");

        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this,
                    "There is no e-mail client installed.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * save the bitmap with given a file-name
     * @param bmp
     * @param imageName
     * @return
     * @throws IOException
     */
    public static File saveBitmap(Bitmap bmp, String imageName) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        String fname = imageName + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);

        try
        {
            FileOutputStream out = new FileOutputStream(file);
            out.write(bytes.toByteArray());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    public  void initFlash()
    {
        if ( false == getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) ) {
            _isFlashAvailable = false;
            return;
        }

        _cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try
        {
            _cameraId = _cameraManager.getCameraIdList()[0];
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        _isFlashAvailable = true;
    }

    public void turnFlashON()
    {
        if ( true == _isFlashOn )
        {
            return;
        }

        if ( true == _cameraId.isEmpty() )
        {
            return;
        }

        try
        {
            if ( VERSION.SDK_INT >= Build.VERSION_CODES.M )
            {
                _cameraManager.setTorchMode(_cameraId, true);
                _isFlashOn = true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void turnFlashOFF()
    {
        if ( false == _isFlashOn )
        {
            return;
        }

        if ( true == _cameraId.isEmpty() )
        {
            return;
        }

        try
        {
            if ( VERSION.SDK_INT >= Build.VERSION_CODES.M )
            {
                _cameraManager.setTorchMode(_cameraId, false);
                _isFlashOn = false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * google API
     */

    /**
     * start the camera
     */
    public void takeImage()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if ( null != takePictureIntent.resolveActivity( getPackageManager() ) )
        {
            turnFlashON();
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * activity callbacks
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        /** callback after taking taking the image */
        if ( REQUEST_IMAGE_CAPTURE == requestCode )
        {
            turnFlashOFF();
            if ( RESULT_OK == resultCode )
            {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                image.setImageBitmap(imageBitmap);

                /** save to file */
                try
                {
                    /** save to file using the current date and time-stamp */
                    String date = "";
                    try
                    {
                        date = new SimpleDateFormat("HH:mm:ss_'on'_dd.MM.yyyy").format(new Date());
                    }
                    catch ( Exception e)
                    {
                        e.printStackTrace();
                    }

                    File newfile;
                    if ( false == date.isEmpty() ) {
                        newfile = saveBitmap(imageBitmap, "zeeny_" + date);
                        Toast.makeText(MainActivity.this,
                                "Image saved with filename: " + newfile.getName().toString()
                                , Toast.LENGTH_SHORT).show();
                    }

                } catch (IOException ioe) {
                    Toast.makeText(MainActivity.this,
                            "File I/O Error!", Toast.LENGTH_SHORT).show();
                }

                /** mark the image as ready */
                _isPictureTaken = true;
            }
        }

        /** callback after picking the contact */
        else if (
                ( PICK_CONTACT_REQUEST == requestCode )
                        &&  ( RESULT_OK == resultCode )
                )
        {
            sendImage();
        }
    }
}
