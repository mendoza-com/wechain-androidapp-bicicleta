package com.boa.utils;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.boa.services.AppLocationService;
import com.boa.services.SendDataTask;
import com.boa.wechain.Exercise;
import com.boa.wechain.WechainApp;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.ArrayList;
import io.realm.Realm;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class DetectedActivitiesIntentService extends IntentService{
	protected static final String TAG = "DetectedActivitiesIS";
	
	public DetectedActivitiesIntentService(){
		// Use the TAG to name the worker thread.
		super(TAG);
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent){
		try{
			Realm.init(WechainApp.getContext());
			final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
			ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
			preferences.edit().putString(Common.KEY_DETECTED_ACTIVITIES, Utils.detectedActivitiesToJson(detectedActivities)).apply();
			
			if(Common.DEBUG){
				System.out.println(Utils.detectedActivitiesToJson(detectedActivities));
				Utils.writeStringInFile(Utils.detectedActivitiesToJson(detectedActivities), "");
			}
			
			for(DetectedActivity da : detectedActivities){
				System.out.println("Servicio: "+Utils.getActivityString(getApplicationContext(), da.getType()) + " " + da.getConfidence() + "%");
				Utils.writeStringInFile(Utils.getActivityString(getApplicationContext(), da.getType()) + " " + da.getConfidence() + "%", "");
				Toast.makeText(WechainApp.getContext(), Utils.getActivityString(getApplicationContext(), da.getType()) + " " + da.getConfidence() + "%", Toast.LENGTH_SHORT).show();
				
				//Empezar a medir y tomar la posición cuando el nivel es aceptable, filtrar por bici
				if(da.getConfidence() > 15 /*&& da.getType() == 1*/){
					AppLocationService.getCurrentPosition();
					//Comparar si es la primera vez no guardamos nada
					if(	!Utils.isEmpty(preferences.getString(Common.PREF_CURRENT_LAT, "")) && !Utils.isEmpty(preferences.getString(Common.PREF_CURRENT_LON, "")) &&
						!Utils.isEmpty(preferences.getString(Common.PREF_SELECTED_LAT, "")) && !Utils.isEmpty(preferences.getString(Common.PREF_SELECTED_LON, ""))){
						//si sigue quieto (prevenir bici fija)
						if(	!preferences.getString(Common.PREF_CURRENT_LAT, "").equals(preferences.getString(Common.PREF_SELECTED_LAT, "")) ||
							!preferences.getString(Common.PREF_CURRENT_LON, "").equals(preferences.getString(Common.PREF_SELECTED_LON, ""))){
							final double meters = AppLocationService.meterDistanceBetweenPoints(Float.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, "")),
								Float.valueOf(preferences.getString(Common.PREF_CURRENT_LON, "")), Float.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, "")),
								Float.valueOf(preferences.getString(Common.PREF_SELECTED_LON, "")));
							if(meters > 0 && meters <= 2000){
								//Se movió algo entonces guardamos pero filtramos si el movimiento fue superior o igual a 1 km
								Realm realm = Realm.getDefaultInstance();
								realm.executeTransaction(new Realm.Transaction(){
									@Override
									public void execute(Realm realm){
										try{
											long ts = System.currentTimeMillis();
											Utils.writeStringInFile("Exercise.id: "+ts+" Exercise.initLat: "+Double.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, ""))
												+ " Exercise.initLon: "+Double.valueOf(preferences.getString(Common.PREF_SELECTED_LON, ""))+
												" Exercise.endLat: "+Double.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, ""))+
												" Exercise.endLon: "+Double.valueOf(preferences.getString(Common.PREF_CURRENT_LON, ""))+ " Exercise.metters: "+meters+
												" Exercise.email: "+preferences.getString("email", "")+ " Exercise.distance: "+
													AppLocationService.meterDistanceBetweenPoints(Float.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, "")),
													Float.valueOf(preferences.getString(Common.PREF_CURRENT_LON, "")),
													Float.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, "")),
													Float.valueOf(preferences.getString(Common.PREF_SELECTED_LON, "")))
												, "");
											Exercise exercise = new Exercise();
											exercise.setId(ts);
											exercise.setInitLat(Double.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, "")));
											exercise.setInitLon(Double.valueOf(preferences.getString(Common.PREF_SELECTED_LON, "")));
											exercise.setEndLat(Double.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, "")));
											exercise.setEndLon(Double.valueOf(preferences.getString(Common.PREF_CURRENT_LON, "")));
											exercise.setDistance(meters);
											exercise.setStatus(Exercise.STATUS_PENDING);
											exercise.setEmail(preferences.getString("email", ""));
											realm.copyToRealmOrUpdate(exercise);
										}catch(Exception e){
											Utils.logError(WechainApp.getContext(), "DetectedActivitiesIntentService:onHandleIntent:execute - ", e);
										}
									}
								});
								realm.close();
							}
						}
					}
				}
			}
			
			//Calcular si con el resto ya tiene un km
			new SendDataTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "DetectedActivitiesIntentService:onHandleIntent - ", e);
		}
	}
}