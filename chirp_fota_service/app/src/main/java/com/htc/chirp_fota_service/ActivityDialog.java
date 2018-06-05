package com.htc.chirp_fota_service;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityDialog extends AppCompatActivity {
    private static final String TAG=Const.G_TAG;
    private TextView bt =null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        bt = (TextView) findViewById(R.id.bt);

        Intent r_intent =getIntent();
        String value= r_intent.getStringExtra("status");
        bt.setText(value);

        bt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG,"onClick");
                //ActivityDialog.this.finish();
                //android.os.Process.killProcess(android.os.Process.myPid());
                moveTaskToBack(true);
                ActivityDialog.this.finish();

            }
        });

    }
}
