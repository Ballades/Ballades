package com.forestwave.pdc8g1.forestwave.services;
import android.app.Service;
import android.content.Intent;
import com.forestwave.pdc8g1.forestwave.location.LocationProvider;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import com.forestwave.pdc8g1.forestwave.model.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositionEngineService extends Service {

    private static final String TAG = "CompositionEngineService";
    public static final int SPECIES_EQUALITY_FACTOR = 1;
    public static final int SCORE_FACILITY = 1;
    public static final int SOUND_DISTANCE_DEACREASE_SLOWNESS = 1;

    private LocationProvider locationProvider;

    public CompositionEngineService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onCreate() {
    }

    /**
     * Fonction princpale : Calcule l'état désiré
     */
    public Map<Integer, InfosTrees> calculateDesiredState(List<Tree> trees, LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
        Map<Species, InfosTrees> infosBySpecies = new HashMap<>();
        Log.v(TAG, "START");
        // Remplir les infos
        for (Tree tree : trees) {
            Species species = tree.getSpecies();
            InfosTrees infosSpecies = infosBySpecies.get(species);
            // Remplir les scores
            Double treeScore = this.getScore(tree);
            infosSpecies.setScore(infosSpecies.getScore()+treeScore);
// Remplir la location
            Double[] newPosition = this.getNewCenterPosition(tree.getLocation(), 1, infosSpecies.getLocation(), infosSpecies.getCount());
            infosSpecies.setLocation(newPosition);
// Remplir les counts
            infosSpecies.setCount(infosSpecies.getCount()+1);
            infosBySpecies.put(species, infosSpecies);
        }
// Pondérer les scores selon les espèces
        for (Map.Entry<Species, InfosTrees> entry : infosBySpecies.entrySet())
        {
            Species species = entry.getKey();
            InfosTrees infos = entry.getValue();
            double speciesVolumeScoreMultiplier = this.getSpeciesVolumeScoreMultiplier(species);
            infos.setScore(infos.getScore()*speciesVolumeScoreMultiplier);
            infosBySpecies.put(species, infos);
        }
// Calculer l'état désiré
        Map<Integer, InfosTrees> desiredState = this.scoresToVolumes(infosBySpecies);
        return desiredState;
    }
    /**
     * Renvoie le score d'un arbre
     */
    private Double getScore(Tree tree) {
        Location userLocation = this.locationProvider.getLocation();
//double distance = tree.getDistance(userLocation); //TODO : Attendre Léo
        double distance = 10; //TEMP
        double score = SOUND_DISTANCE_DEACREASE_SLOWNESS*SCORE_FACILITY/(distance+SOUND_DISTANCE_DEACREASE_SLOWNESS);
        return score;
    }
    /**
     * Renvoie le nouveau barycentre de deux locations avec leurs poids respectifs
     */
    private Double[] getNewCenterPosition(Double[] position1, Integer weight1, Double[] position2, Integer weight2) {
        Double[] centerPosition = new Double[]{};
        double newLat = (position1[0]*weight1 + position2[0]*weight2)/(weight1+weight2);
        centerPosition[0] = newLat;
        double newLong = (position1[1]*weight1 + position2[1]*weight2)/(weight1+weight2);
        centerPosition[1] = newLong;
        return centerPosition;
    }
    /**
     * Calcule l'etat sonore désiré (tracks avec volume et position)
     */
    private Map<Integer, InfosTrees> scoresToVolumes(Map<Species, InfosTrees> infosBySpecies) {
        Map<Integer, InfosTrees> infosByTrack = new HashMap<>();
// Regrouper les scores par track
        for (Map.Entry<Species, InfosTrees> entry : infosBySpecies.entrySet())
        {
            Species species = entry.getKey();
            InfosTrees infos = entry.getValue();
//int trackId = species.getTrack(); // TODO : connecter avec DAO
            Integer trackId = 1; //TEMP
            InfosTrees infoTrack = infosByTrack.get(trackId);
// Grouper les scores
            infoTrack.setScore(infoTrack.getScore() + infos.getScore());
// Grouper les positions
            Double[] newPosition = this.getNewCenterPosition(infos.getLocation(), infos.getCount(), infoTrack.getLocation(), infoTrack.getCount());
            infoTrack.setLocation(newPosition);
// Mettre à jour les counts
            infoTrack.setCount(infoTrack.getCount() + infos.getCount());
        }
// Calculer les volumes par track
        for (Map.Entry<Integer, InfosTrees> entry : infosByTrack.entrySet())
        {
            int track = entry.getKey();
            InfosTrees infos = entry.getValue();
            infos.setVolume(Math.tanh(infos.getScore()));
        }
        return infosByTrack;
    }
    /**
     * Renvoie le multiplicateur de score pour une espèce donnée
     */
    private double getSpeciesVolumeScoreMultiplier(Species species) {
//int speciesCount = species.getCount(); //TODO : Brancher avec la DAO
        int speciesCount = 100; //TEMP
        double multiplier = SPECIES_EQUALITY_FACTOR /(speciesCount+SPECIES_EQUALITY_FACTOR);
        return multiplier;
    }
}