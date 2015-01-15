package com.forestwave.pdc8g1.forestwave.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class CompositionEngineService {
    private static Context context;

    public CompositionEngineService(Context context) {
           CompositionEngineService.context = context;
    }

    public void start() {
        AudioTrackSoundPlayer audioTrackSoundPlayer = new AudioTrackSoundPlayer(context);
        audioTrackSoundPlayer.play("ds");
        audioTrackSoundPlayer.play("clarinet");
        //audioTrackSoundPlayer.stop("ds");
        //audioTrackSoundPlayer.stop("clarinet");
    }
}
