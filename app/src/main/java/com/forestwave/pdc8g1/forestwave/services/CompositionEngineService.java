package com.forestwave.pdc8g1.forestwave.services;

import android.app.Service;
import android.content.Intent;
import com.forestwave.pdc8g1.forestwave.location.LocationProvider;

import android.location.Location;
import android.os.IBinder;

import com.forestwave.pdc8g1.forestwave.model.*;
import com.forestwave.pdc8g1.forestwave.model.Integer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositionEngineService extends Service {

    public static final int SPECIES_EQUALITY_FACTOR = 1;
    public static final int SCORE_FACILITY = 1;
    public static final int SOUND_DISTANCE_DEACREASE_SLOWNESS = 1;

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
     * Fonction princpale : appelle le nécessaire pour jouer la composition "ambient"
     */
    public void play(List<Tree> trees, LocationProvider locationProvider) {

        Location userLocation = locationProvider.getLocation();
        //Double azimuth = locationProvider.getAzimuth(); //TODO : Attendre Léo
        double azimuth = 0; //TEMP

        Map<Species, InfosTrees> infosBySpecies = new HashMap<>();

        // Remplir les infos
        for (Tree tree : trees) {
            Species species = tree.getSpecies();
            InfosTrees infosSpecies = infosBySpecies.get(species);

            // Remplir les scores
            Double treeScore = this.getScore(tree, userLocation);
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
        Map<java.lang.Integer, Double> desiredState = this.calculateDesiredState(infosBySpecies, userLocation, azimuth);

        // Appliquer cet état


    }

    /**
     * Renvoie le score d'un arbre
     */
    private Double getScore(Tree tree, Location userLocation) {
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
    private Map<java.lang.Integer, Double> calculateDesiredState(Map<Species, InfosTrees> infosBySpecies, Location userLocation, Double azimuth) {
        Map<java.lang.Integer, Double[]> scoreByTrack = new HashMap<>(); // 0 : score, 1 : count, 2 : centerPositionLat, 3 : centerPositionLong
        Map<java.lang.Integer, Double[]> volumeByTrack = new HashMap<>();


        // Regrouper les scores par track
        for (Map.Entry<Integer, Double[]> entry : infosBySpecies.entrySet())
        {
            Integer species = entry.getKey();
            Double[] infos = entry.getValue();
            Double[] trackInfos = (scoreByTrack.get(track) != null) ? scoreByTrack.get(track) : new Double[]{0.0, 0.0, 0.0};

            // Grouper les scores
            double speciesScore = infos[0];
            //int track = species.getTrack(); // TODO : connecter avec DAO
            java.lang.Integer track = 1; //TEMP
            infos[0] = trackInfos[0]+speciesScore;

            //Grouper les positions
            double speciesCenterPositionLat = infos[2];
            double speciesCenterPositionLong = infos[3];
            ArrayList<Double> newPosition = this.getNewCenterPosition(tree, speciesCenterPositionLat, speciesCenterPositionLong, countForSpecies);
            infos[2] = newPosition.get(0);
            infos[3] = newPosition.get(1);

            scoreByTrack.put(track, infos);
        }

        // Calculer les volumes par track
        for (Map.Entry<java.lang.Integer, Double> entry : scoreByTrack.entrySet())
        {
            int track = entry.getKey();
            double score = entry.getValue();
            double volume = Math.tanh(score);

            scoreByTrack.put(track, volume);
        }

        return volumeByTrack;
    }

    /**
     * Renvoie le multiplicateur de score pour une espèce donnée
     */
    private double getSpeciesVolumeScoreMultiplier(Species species) {
        //int speciesCount = species.getCount(); //TODO : Brancher avec la DAO
        int speciesCount = 100; //TEMP
        double multiplier = SPECIES_EQUALITY_FACTOR /(speciesCount+ SPECIES_EQUALITY_FACTOR);
        return multiplier;
    }
}
