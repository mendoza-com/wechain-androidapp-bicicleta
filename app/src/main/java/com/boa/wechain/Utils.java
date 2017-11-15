package com.boa.wechain;

import android.content.Context;
import android.content.res.Resources;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class Utils{
	/**
	 * Registra forzadamente una Excepción en Crashlytics
	 * @param context Contexto de app
	 * @param referenceName Etiqueta para ubicar el error detectado desde el panel de Fabric
	 * @param e Excepción capturada
	 */
	public static void logError(Context context, final String referenceName, final Exception e){
		try{
			if(context == null){
				context = WechainApp.getContext();
			}
			
			if(Common.DEBUG && e != null){
				System.out.println(referenceName+" "+e);
				e.printStackTrace();
			}else{
				if(context != null){
					Fabric.with(context, new Crashlytics());
					Crashlytics.getInstance();
					
					if(e != null){
						Crashlytics.logException(e);
					}else{
						Crashlytics.log(referenceName);
					}
				}
			}
		}catch(Exception ex){
			System.out.println("Utils:logError - Exception: "+ex);
			
			if(Common.DEBUG){
				ex.printStackTrace();
			}
		}
	}
	
	private Utils(){}
	
	/**
	 * Returns a human readable String corresponding to a detected activity type.
	 */
	public static String getActivityString(Context context, int detectedActivityType){
		Resources resources = context.getResources();
		switch(detectedActivityType) {
			case DetectedActivity.IN_VEHICLE:
				return resources.getString(R.string.in_vehicle);
			case DetectedActivity.ON_BICYCLE:
				return resources.getString(R.string.on_bicycle);
			case DetectedActivity.ON_FOOT:
				return resources.getString(R.string.on_foot);
			case DetectedActivity.RUNNING:
				return resources.getString(R.string.running);
			case DetectedActivity.STILL:
				return resources.getString(R.string.still);
			case DetectedActivity.TILTING:
				return resources.getString(R.string.tilting);
			case DetectedActivity.UNKNOWN:
				return resources.getString(R.string.unknown);
			case DetectedActivity.WALKING:
				return resources.getString(R.string.walking);
			default:
				return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
		}
	}
	
	public static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList){
		Type type = new TypeToken<ArrayList<DetectedActivity>>() {}.getType();
		return new Gson().toJson(detectedActivitiesList, type);
	}
	
	public static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray){
		Type listType = new TypeToken<ArrayList<DetectedActivity>>(){}.getType();
		ArrayList<DetectedActivity> detectedActivities = new Gson().fromJson(jsonArray, listType);
		
		if(detectedActivities == null){
			detectedActivities = new ArrayList<>();
		}
		
		return detectedActivities;
	}
}