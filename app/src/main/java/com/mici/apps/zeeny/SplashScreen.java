package com.mici.apps.zeeny;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

public class SplashScreen extends AppCompatActivity {

    private static int SPLASH_TIMEOUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed( new Runnable() {
            @Override
            public void run() {
                Intent homeIntent = new Intent( SplashScreen.this, MainActivity.class );
                startActivity(homeIntent);
                finish();
            }
        }, SPLASH_TIMEOUT );
    }
}
