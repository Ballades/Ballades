package com.forestwave.pdc8g1.forestwave.utils;

import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.forestwave.pdc8g1.forestwave.location.LocationProvider;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.model.DaoSession;
import com.forestwave.pdc8g1.forestwave.model.InfosTrees;
import com.forestwave.pdc8g1.forestwave.model.Species;
import com.forestwave.pdc8g1.forestwave.model.Tree;
import com.forestwave.pdc8g1.forestwave.model.TreeDao;
import com.forestwave.pdc8g1.forestwave.service.SoundService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.query.Query;

public class TreeFinder implements Runnable {

    private final static String TAG = "TreeFinder";
    public final static double MIN_VOLUME = 0.01;
    public final static double DISTANCE_DETECTION_MAX = 0.0005;
    public final static Integer REFRESH_TIME_TREES = 750;
    public final static Integer PRIORITY_TREE_FINDER = 4;

    private TreeDao treeDao =  null;
    private SoundService soundService;

    public TreeFinder(SoundService service) {
        this.soundService = service;
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(service.getApplicationContext(), "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        treeDao = daoSession.getTreeDao();
        Thread.currentThread().setPriority(PRIORITY_TREE_FINDER);
    }

    @Override
    /**
     * Boucle qui rafraîchit le desiredState (tracks à jouer avec leur position) assez fréquemment
     */
    public void run() {

        if(soundService.provider.getLocation() != null && soundService.isRunning()) {
            Double latitude = soundService.provider.getLocation().getLatitude();
            Double longitude = soundService.provider.getLocation().getLongitude();
            Log.d(TAG, "Latitude : " + latitude + ", Longitude : " + longitude);
            Query query = treeDao.queryBuilder().where(TreeDao.Properties.Latitude.between(latitude - DISTANCE_DETECTION_MAX, latitude + DISTANCE_DETECTION_MAX), TreeDao.Properties.Longitude.between(longitude - DISTANCE_DETECTION_MAX, longitude + DISTANCE_DETECTION_MAX)).build();
            List<Tree> trees = query.list();
            //List<Tree> trees = getTestTrees(); //TEST

            Log.d(TAG, "Nombre d'arbres pris en compte : " + Integer.toString(trees.size()));
            Map<Integer, InfosTrees> desiredState = calculateDesiredState(trees, soundService.provider);

            soundService.setDesiredState(desiredState);
        }
        soundService.handler.postDelayed(this, REFRESH_TIME_TREES);
    }

    /**
     * Renvoie deux arbres avec leur species pour les données de test
     */
    private List<Tree> getTestTrees() { //TEST
        ArrayList<Tree> testTrees = new ArrayList<>();
        Species species1 = new Species((long)1212121, "sequoya de la petite ile", 1, 100);
        Species species2 = new Species((long)1212122, "sequoya de la grande ile", 2, 100);

        Tree tree1 = new Tree((long)4224242, species1, 1, 45.77924188, 4.85142946);
        Tree tree2 = new Tree((long)4224243, species2, 1, 45.78042411, 4.85162258);
        testTrees.add(tree1);
        testTrees.add(tree2);

        return testTrees;
    }

    /**
     * Calcule le desiredState
     */
    public Map<Integer, InfosTrees> calculateDesiredState(List<Tree> trees, LocationProvider locationProvider) {
        Map<Species, InfosTrees> infosBySpecies = new HashMap<>();
        Log.v(TAG, "START");

        // Remplir les infos
        for (Tree tree : trees) {
            Species species = tree.getSpecies();

            InfosTrees infosSpecies = (infosBySpecies.containsKey(species)) ? infosBySpecies.get(species) : new InfosTrees();

            // Remplir les scores
            Double treeScore = this.getScore(tree);
            infosSpecies.setScore(infosSpecies.getScore()+treeScore);

            // Remplir la location
            Double[] newPosition = this.getNewCenterPosition(tree.getLocation(), 1, infosSpecies.getLocation(), infosSpecies.getCount());
            infosSpecies.setLocation(newPosition);
            Log.v(TAG, "AAlatitude : " + newPosition[0] + ", AAlongitude : " + newPosition[1]);


            // Remplir les counts
            infosSpecies.setCount(infosSpecies.getCount()+1);

            infosBySpecies.put(species, infosSpecies);
            Log.v(TAG, "ARBRE : species : " + species.getName() + ", getCount : " + infosSpecies.getCount() + ", getScore : " + infosSpecies.getScore());
        }

        // Pondérer les scores selon les espèces
        for (Map.Entry<Species, InfosTrees> entry : infosBySpecies.entrySet())
        {
            Species species = entry.getKey();
            InfosTrees infos = entry.getValue();
            double speciesVolumeScoreMultiplier = this.getSpeciesVolumeScoreMultiplier(species);

            infos.setScore(infos.getScore()*speciesVolumeScoreMultiplier);
            infosBySpecies.put(species, infos);
            Log.v(TAG, "ESPECE : species : " + species.getName() + ", getCount : " + infos.getCount() + ", getScore : " + infos.getScore());
        }

        // retourner l'état désiré
        return this.scoresToVolumes(infosBySpecies);
    }

    /**
     * Renvoie le score d'un arbre
     */
    private Double getScore(Tree tree) {
        Location userLocation = soundService.provider.getLocation();
        double distance = tree.getDistance(userLocation.getLatitude(), userLocation.getLongitude());
        Log.d(TAG, "SDDS distance : " + distance);
        double score = SoundService.SOUND_DISTANCE_DEACREASE_SLOWNESS*SoundService.SCORE_FACILITY/(distance*distance*distance+SoundService.SOUND_DISTANCE_DEACREASE_SLOWNESS);
        Log.d(TAG, "SDDS score : " + score);
        return score;
    }

    /**
     * Renvoie le nouveau barycentre de deux locations avec leurs poids respectifs
     */
    private Double[] getNewCenterPosition(Double[] position1, Integer weight1, Double[] position2, Integer weight2) {
        Double[] centerPosition = new Double[]{0.0, 0.0};

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
        ArrayList<Integer> tracksTooQuiet = new ArrayList<>();
        Log.v(TAG, "IN scoresToVolumes");
        // Regrouper les scores par track
        for (Map.Entry<Species, InfosTrees> entry : infosBySpecies.entrySet())
        {
            Species species = entry.getKey();
            InfosTrees infos = entry.getValue();
            int trackId = species.getTrack();
            InfosTrees infoTrack = (infosByTrack.containsKey(trackId)) ? infosByTrack.get(trackId) : new InfosTrees();

            // Grouper les scores
            infoTrack.setScore(infoTrack.getScore() + infos.getScore());

            // Grouper les positions
            Double[] newPosition = this.getNewCenterPosition(infos.getLocation(), infos.getCount(), infoTrack.getLocation(), infoTrack.getCount());
            infoTrack.setLocation(newPosition);

            // Mettre à jour les counts
            infoTrack.setCount(infoTrack.getCount() + infos.getCount());

            infosByTrack.put(trackId, infoTrack);
            Log.v(TAG, "trackId : " + trackId + ", getCount : " + infoTrack.getCount() + ", getScore : " + infoTrack.getScore());
        }

        // Calculer les volumes par track
        for (Map.Entry<Integer, InfosTrees> entry : infosByTrack.entrySet())
        {
            int track = entry.getKey();
            InfosTrees infos = entry.getValue();
            double volume = Math.tanh(infos.getScore());

            infos.setVolume(volume);
            if (volume < MIN_VOLUME) {
                tracksTooQuiet.add(track);
            }
        }

        // Supprimer les tracks trop silencieuses
        for (Integer trackTooQuietInteger: tracksTooQuiet) {
            infosByTrack.remove(trackTooQuietInteger);
        }

        return infosByTrack;
    }

    /**
     * Renvoie le multiplicateur de score pour une espèce donnée
     */
    private double getSpeciesVolumeScoreMultiplier(Species species) {
        double speciesCount = species.getCount();
        Log.v(TAG, "FFF"+(SoundService.SPECIES_EQUALITY_FACTOR /(speciesCount+SoundService.SPECIES_EQUALITY_FACTOR)));
        return SoundService.SPECIES_EQUALITY_FACTOR /(speciesCount+SoundService.SPECIES_EQUALITY_FACTOR);
    }
}
