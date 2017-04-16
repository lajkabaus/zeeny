package com.mici.apps.zeeny;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
    ImageView _image;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PICK_CONTACT  = 2;

    static final int REQUEST_WRITE_STORAGE = 3;
    static final int REQUEST_USE_CAMERA    = 4;

    boolean _isPictureTaken   = false;
    boolean _isFlashAvailable = false;
    boolean _isFlashOn        = false;

    boolean _hasPermission_IO_write = false;
    boolean _hasPermission_camera   = false;


    private static CameraManager _cameraManager = null;
    private static String _cameraId = "";

    private String _lastImageName = "";
    private Uri _lastImageUri;
    private static final String appName = "Zeeny";

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

        _image = (ImageView)findViewById(R.id.cameraView);
        _image.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkCameraPermissions();
                createImage();
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
        startActivityForResult(pickContactIntent, REQUEST_PICK_CONTACT);
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

        if ( true == _lastImageName.isEmpty() )
        {
            Toast.makeText(this, "no new image", Toast.LENGTH_SHORT).show();
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
        emailIntent.putExtra(Intent.EXTRA_STREAM, _lastImageUri);

        try
        {
            startActivity(Intent.createChooser(emailIntent, "Send e-mail with"));
            finish();

        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There is no e-mail client installed.", Toast.LENGTH_SHORT).show();
        }

        _lastImageName = "";
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals(state) )
        {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        if  (   Environment.MEDIA_MOUNTED.equals(state)
            ||  Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
            )
        {
            return true;
        }
        return false;
    }

    public File getImagesDir()
    {
        // Get the directory for the user's public pictures directory.
        File folder = new File( Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ), "/" + appName);
        if ( false == folder.exists() )
        {
            if (false == folder.mkdir())
            {
                Log.e("", "Directory not created");
                folder = null;
            }
        }
        return folder;
    }

    /**
     * load a file from app's directory
     * @param fileName
     * @return
     */
    public File loadFile(String fileName)
    {
        if ( false == isExternalStorageReadable() )
        {
            return null;
        }

        if ( true == fileName.isEmpty() )
        {
            return null;
        }

        File file = new File( _lastImageUri.toString() );
        return file;
    }

    /**
     * save the bitmap with given a file-name
     * @return
     * @throws IOException
     */
    public File createImageFile() throws IOException
    {
        if ( false == _lastImageName.isEmpty() )
        {
            _lastImageName = "";
            return null;
        }

        if ( false == isExternalStorageWritable() )
        {
            _lastImageName = "";
            return null;
        }

        checkWritePermissions();
        if ( false == _hasPermission_IO_write )
        {
            _lastImageName = "";
            return null;
        }

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

        if ( true == date.isEmpty() )
        {
            return null;
        }

        File imagesDir = getImagesDir();
        if ( ( null == imagesDir ) || ( false == imagesDir.isDirectory() ) )
        {
            return null;
        }

        File image = new File(imagesDir, date.toString() + ".jpg");
        if ( true == image.exists() )
        {
            image.delete();
        }

        _lastImageName = image.toString();
        _lastImageUri = Uri.fromFile(image);

        return image;
    }

    private void createImage()
    {
        if ( false == _hasPermission_camera )
        {
            Toast.makeText(this, "Camera usage not allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if ( null != takePictureIntent.resolveActivity( getPackageManager() ) )
        {
            // Create the File where the photo should go
            File photoFile = null;
            try
            {
                photoFile = createImageFile();
            }
            catch (IOException ioe)
            {
                Toast.makeText(this, "File I/O Error!", Toast.LENGTH_SHORT).show();
                ioe.printStackTrace();
            }

            // Continue only if a file was successfully created
            if ( null != photoFile )
            {
                takePictureIntent.putExtra( MediaStore.EXTRA_OUTPUT, _lastImageUri );
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public  void initFlash()
    {
        if ( false == getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) )
        {
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

    public void checkWritePermissions()
    {
        _hasPermission_IO_write =
            ( PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) );

        if ( false == _hasPermission_IO_write )
        {
            ActivityCompat.requestPermissions(  this,
                                                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                                REQUEST_WRITE_STORAGE   );
        }
    }

    public void checkCameraPermissions()
    {
        _hasPermission_camera =
                ( PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) );

        if ( false == _hasPermission_camera )
        {
            ActivityCompat.requestPermissions(  this,
                                                new String[]{ Manifest.permission.CAMERA },
                                                REQUEST_USE_CAMERA   );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE:
            {
                if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                {
                    _hasPermission_IO_write = true;
                }
                else
                {
                    Toast.makeText(this, "permission to write to files is required", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case REQUEST_USE_CAMERA:
            {
                if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED )
                {
                    _hasPermission_camera = true;
                }
                else
                {
                    Toast.makeText(this, "permission to use camera is required", Toast.LENGTH_LONG).show();
                }
                break;
            }
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
        switch (requestCode)
        {
            case REQUEST_IMAGE_CAPTURE:
            {
                if ( RESULT_OK == resultCode )
                {
                    /** set thumbnail */
                    Bundle extras = data.getExtras();

                    Bitmap imageBitmap = null;
                    if ( null != extras )
                    {
                        imageBitmap = (Bitmap) extras.get("data");
                    }
                    else
                    {
                        if ( false == _lastImageName.isEmpty() )
                        {
                            imageBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(_lastImageName), _image.getWidth(), _image.getHeight());
                        }
                    }

                    if ( null != imageBitmap )
                    {
                        _image.setImageBitmap(imageBitmap);
                    }

                    Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
                    _isPictureTaken = true;
                }
                break;
            }

            case REQUEST_PICK_CONTACT:
            {
                if ( RESULT_OK == resultCode )
                {
                    /** send the image */
                    sendImage();
                }
                break;
            }

            default: {}
        }
    }
}
