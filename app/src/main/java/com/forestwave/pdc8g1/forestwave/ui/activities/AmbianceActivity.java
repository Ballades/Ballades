package com.forestwave.pdc8g1.forestwave.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

import com.forestwave.pdc8g1.forestwave.R;

/**
 * Created by leo on 26/01/15.
 */
public class AmbianceActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the user interface layout for this Activity
        // The layout file is defined in the project res/layout/main_activity.xml file
        setContentView(R.layout.ambiance_activity);
    }
}
