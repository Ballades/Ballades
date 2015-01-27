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

import org.puredata.core.PdBase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.query.Query;

/**
 * Created by leo on 17/01/15.
 */
public class TreeFinder implements Runnable {
    private final static String TAG="TreeFinder";
    public static final int SPECIES_EQUALITY_FACTOR = 1;
    public static final int SCORE_FACILITY = 1000;
    public static final int SOUND_DISTANCE_DEACREASE_SLOWNESS = 1;

    private TreeDao treeDao =  null;
    private WeakReference<SoundService> serviceWeakReference;

    public TreeFinder(SoundService service) {
        serviceWeakReference =new WeakReference<>(service);
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(service.getApplicationContext(), "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        treeDao = daoSession.getTreeDao();

    }

    @Override
    public void run() {

        if(serviceWeakReference.get().provider.getLocation() != null && serviceWeakReference.get().isRunning()) {
            Double latitude = serviceWeakReference.get().provider.getLocation().getLatitude();
            Double longitude = serviceWeakReference.get().provider.getLocation().getLongitude();
            Log.d(TAG, "Latitude : " + latitude + ", Longitude : " + longitude);
            Query query = treeDao.queryBuilder().where(TreeDao.Properties.Latitude.between(latitude - 0.05, latitude + 0.05), TreeDao.Properties.Longitude.between(longitude - 0.01/76, longitude + 0.01/76)).build();
            List<Tree> trees = query.list();
            //List<Tree> trees = getTestTrees(); //TEMP

            Log.d(TAG, "Nombre d'arbres pris en compte : " + Integer.toString(trees.size()));
            Map<Integer, InfosTrees> desiredState = calculateDesiredState(trees, serviceWeakReference.get().provider);
            this.applyState(desiredState);
        }
        serviceWeakReference.get().handler.postDelayed(this, 1000);
    }

    private List<Tree> getTestTrees() { //TEMP
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
     * Applique l'état désiré sur la sortie sonore (mode ambient)
     */
    private void applyState(Map<Integer, InfosTrees> desiredState) {
        Log.d(TAG, "Nombre de tracks simultanées : " + desiredState.size());

        // Jouer les tracks désirées
        for (Map.Entry<Integer, InfosTrees> entry : desiredState.entrySet())
        {
            int track = entry.getKey();
            InfosTrees infos = entry.getValue();
            Log.d(TAG, "track : " + track + ", volume : " + infos.getVolume());
            Log.d(TAG, "latitude : " + infos.getLocation()[0] + ", longitude : " + infos.getLocation()[1]);

            // Jouer la piste si non démarrée
            if (!serviceWeakReference.get().playingTracks.contains(track)) {
                PdBase.sendBang("play_" + track);
                serviceWeakReference.get().playingTracks.add(track);
                Log.d(TAG, "NOW PLAYING : "+ track);
            }

            // Calculer les valeurs des canaux
            double[] inputsValue = this.getInputsValue(serviceWeakReference.get().provider, infos.getLocation());

            // Appliquer les valeurs
            PdBase.sendFloat("volume_left_" + track, (float)(inputsValue[0]*infos.getVolume()));
            PdBase.sendFloat("volume_right_" + track, (float)(inputsValue[1]*infos.getVolume()));
        }

        // Retirer les tracks non-présentes
        for (Integer playingTrack : serviceWeakReference.get().playingTracks) {
            if (!desiredState.containsKey(playingTrack)) {
                //TODO : Appeler le stop chez PD
                PdBase.sendBang("stop_" + playingTrack);
                serviceWeakReference.get().playingTracks.remove(playingTrack);
            }
        }
    }

    /**
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

        double angle = soundToNorthAngle - Math.toRadians(provider.getLocation().getBearing());
        Log.v(TAG, "NtoSound : " + Math.toDegrees(Math.atan(deltaX/deltaY)));
        Log.v(TAG, "bearing : " + provider.getLocation().getBearing());
        Log.v(TAG, "angle : " + Math.toDegrees(angle));

        inputsValue[1] = Math.sin(angle)/2+0.5;
        inputsValue[0] = 1-inputsValue[1];
        Log.d(TAG, "Left : " + inputsValue[0]);
        Log.d(TAG, "Right : " + inputsValue[1]);

        return inputsValue;
    }
    /**
     * Fonction princpale : Calcule l'état désiré
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
        Location userLocation = serviceWeakReference.get().provider.getLocation();
        //double distance = tree.getDistance(userLocation); //TODO : Attendre Léo
        double distance = 10; //TEMP
        double score = SOUND_DISTANCE_DEACREASE_SLOWNESS*SCORE_FACILITY/(distance+SOUND_DISTANCE_DEACREASE_SLOWNESS);

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

            infos.setVolume(Math.tanh(infos.getScore()));
        }

        // TODO : Limiter le nombre de tracks en fonction du volume

        return infosByTrack;
    }

    /**
     * Renvoie le multiplicateur de score pour une espèce donnée
     */
    private double getSpeciesVolumeScoreMultiplier(Species species) {
        double speciesCount = species.getCount();
        Log.v(TAG, "FFF"+(SPECIES_EQUALITY_FACTOR /(speciesCount+SPECIES_EQUALITY_FACTOR)));
        return SPECIES_EQUALITY_FACTOR /(speciesCount+SPECIES_EQUALITY_FACTOR);
    }
}
