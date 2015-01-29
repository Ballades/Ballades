package com.forestwave.pdc8g1.forestwave.utils;

import android.util.Log;

import com.forestwave.pdc8g1.forestwave.location.LocationProvider;
import com.forestwave.pdc8g1.forestwave.model.InfosTrees;
import com.forestwave.pdc8g1.forestwave.service.SoundService;

import org.puredata.core.PdBase;

import java.util.Map;

public class SoundPlayer implements Runnable {

    private final static String TAG = "SoundPlayer";
    private SoundService soundService;

    public SoundPlayer(SoundService service) {
        this.soundService = service;
    }

    @Override
    /**
     * Boucle qui applique le desiredState très fréquemment (pour l'azimuth notamment)
     */
    public void run() {
        if(soundService.provider.getLocation() != null && soundService.isRunning()) {
            Log.d(TAG, "Latitude : " + soundService.provider.getLocation().getLatitude() + ", Longitude : " + soundService.provider.getLocation().getLongitude());

            Map<Integer, InfosTrees> desiredState = soundService.getDesiredState();
            Map<Integer, InfosTrees> actualState = soundService.getActualState();
            Log.d(TAG, "Nombre de tracks simultanées : " + desiredState.size());

            // Jouer les tracks désirées
            for (Map.Entry<Integer, InfosTrees> entry : desiredState.entrySet())
            {
                int track = entry.getKey();
                InfosTrees infos = entry.getValue();
                Log.d(TAG, "track : " + track + ", volume : " + Math.round(infos.getVolume()*100));
                Log.d(TAG, "latitude : " + infos.getLocation()[0] + ", longitude : " + infos.getLocation()[1]);

                // Jouer la piste si non démarrée

                if (!actualState.containsKey(track)) {
                    PdBase.sendBang("play_" + track);
                    Log.d(TAG, "NOW PLAYING : " + track);
                }

                // Calculer les valeurs des canaux
                double[] inputsValue = this.getInputsValue(soundService.provider, infos.getLocation());

                // Appliquer les valeurs
                PdBase.sendFloat("volume_left_" + track, (float)(inputsValue[0]*infos.getVolume()));
                PdBase.sendFloat("volume_right_" + track, (float)(inputsValue[1]*infos.getVolume()));
                Log.d(TAG, "VOL IN Left : " + Math.round(inputsValue[0]*infos.getVolume()*100));
                Log.d(TAG, "VOL IN Right : " + Math.round(inputsValue[1]*infos.getVolume()*100));
            }

            // Retirer les tracks non-présentes
            for (Map.Entry<Integer, InfosTrees> entry : actualState.entrySet())
            {
                int playingTrack = entry.getKey();
                if (!desiredState.containsKey(playingTrack)) {
                    PdBase.sendBang("stop_" + playingTrack);
                    Log.d(TAG, "STOPPING : " + playingTrack);
                }
            }

            // Mettre à jour le actualState
            soundService.setActualState(desiredState);
        }
        soundService.handler.postDelayed(this, 500);
    }

    /**
<<<<<<< Updated upstream
=======
     * Applique l'état désiré sur la sortie sonore (mode ambient)
     */
    private void applyDesiredState() {

    }

    /**
>>>>>>> Stashed changes
     * Calcule les valeurs à mettre dans les sorties droites et gauche pour simuler l'angle voulu,
     * à partir d'une position
     */
    private double[] getInputsValue(LocationProvider provider, Double[] locationSound) {
        double[] inputsValue = {0.0, 0.0};
        double deltaX = locationSound[1] - provider.getLocation().getLongitude();
        double deltaY = locationSound[0] - provider.getLocation().getLatitude();
        Log.v(TAG, "deltaX : " + deltaX*10000);
        Log.v(TAG, "deltaY : " + deltaY*10000);
        double a = Math.atan(deltaX / deltaY);
        double soundToNorthAngle = ((Math.signum(deltaX) == Math.signum(deltaX)) ? a : -a) + ((deltaY < 0) ? Math.toRadians(180) : 0);

        double angle = soundToNorthAngle  - Math.toRadians(soundService.getmCurrentDegree());
        Log.v(TAG, "NtoSound : " + Math.toDegrees(Math.atan(deltaX/deltaY)));
        Log.v(TAG, "bearing : " + soundService.getmCurrentDegree());
        Log.v(TAG, "angle : " + Math.toDegrees(angle));

        inputsValue[1] = Math.sin(angle)/2+0.5;
        inputsValue[0] = 1-inputsValue[1];

        return inputsValue;
    }

}
