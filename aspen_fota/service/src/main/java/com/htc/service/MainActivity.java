package com.htc.service;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.util.Log;

import com.htc.aspen_fota.service.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG=Const.G_TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        try {
            Class c = Class.forName("com.htc.service.FotaService");
            Intent intent = new Intent(this, c);
            Log.d(TAG, "__jh__ startService: " + c.getName() );
            startService(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //finish();
    }

}
