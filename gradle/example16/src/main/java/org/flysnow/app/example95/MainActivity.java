package org.flysnow.app.example95;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] sharedLibraryNames = getPackageManager().getSystemSharedLibraryNames();
        StringBuilder resultBuild = new StringBuilder();
        for(String name:sharedLibraryNames){
            resultBuild.append(name);
            resultBuild.append("\n");
        }
        resultBuild.append(BuildConfig.jh_release1);
        resultBuild.append("\n");
        String tmp_s = getResources().getString(R.string.res_value);
        resultBuild.append(tmp_s);
        ((TextView)findViewById(android.R.id.text1)).setText(resultBuild);

    }

}
