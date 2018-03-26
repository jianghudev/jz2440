package org.flysnow.app.example131;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hello  h = new hello();
        ((TextView) findViewById(android.R.id.text1)).setText(h.get_hello());
    }

}
