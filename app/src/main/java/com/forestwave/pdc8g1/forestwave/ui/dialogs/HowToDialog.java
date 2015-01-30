package com.forestwave.pdc8g1.forestwave.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.forestwave.pdc8g1.forestwave.R;

public class HowToDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.how_to_dialog_content)
                    .setPositiveButton(R.string.about_dialog_validate, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    })
                    .setTitle(R.string.how_to_dialog_title);

            // Create the AlertDialog object and return it
            return builder.create();

        }
}
