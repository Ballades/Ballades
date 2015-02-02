package com.forestwave.pdc8g1.forestwave.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.forestwave.pdc8g1.forestwave.service.MyDownloadService;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;

/**
 * Created by Sylvain on 29/01/15.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            DownloaderClientMarshaller.startDownloadServiceIfRequired(context,
                    intent, MyDownloadService.class);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}

