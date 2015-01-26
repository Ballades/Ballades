package com.forestwave.pdc8g1.forestwave.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.forestwave.pdc8g1.forestwave.R;

/**
 * Created by leo on 26/01/15.
 */
public class StartActivity extends Activity {

    private Button mRun_button;
    private Button mAmbiance_button;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the user interface layout for this Activity
        // The layout file is defined in the project res/layout/main_activity.xml file
        setContentView(R.layout.start_activity);

        mRun_button = (Button) findViewById(R.id.run_button);
        mAmbiance_button = (Button) findViewById(R.id.ambiance_button);
    }

    public void launchRunActivity(View view) {

        Intent intent = new Intent(this, RunActivity.class);
        startActivity(intent);
    }

    public void launchAmbianceActivity(View view) {

        Intent intent = new Intent(this, AmbianceActivity.class);
        startActivity(intent);
    }
}
