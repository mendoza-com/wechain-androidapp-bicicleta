package com.boa.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import com.boa.wechain.R;
import com.boa.wechain.WechainApp;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
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
	
	/**
	 * Escribe la variable convertida a String en un archivo con posibilidad de renombrarlo
	 * @param string
	 */
	public static void writeStringInFile(String string, String fileName){
		try{
			if(isEmpty(fileName)){
				fileName = WechainApp.getContext().getString(R.string.app_name)+"Debug.txt";
			}
			
			File root			= new File(Environment.getExternalStorageDirectory(), WechainApp.getContext().getString(R.string.app_name)+"Debug");
			root.mkdirs();
			File gpxfile		= new File(root, fileName);
			FileWriter writer	= new FileWriter(gpxfile, true);
			writer.append(System.getProperty("line.separator")).append(getDateTimePhone()).append(": ").append(string);
			writer.flush();
			writer.close();
		}catch(Exception e){
			logError(WechainApp.getContext(), "Utils:writeStringInFile - Exception:", e);
		}
	}
	
	/**
	 * Devuelve la fecha y hora actual del teléfono
	 *
	 * @return Fecha y hora en formato "dd/MM/yyyy HH:mm:ss"
	 */
	public static String getDateTimePhone(){
		try{
			Calendar cal		= new GregorianCalendar();
			Date date			= cal.getTime();
			SimpleDateFormat df	= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
			return df.format(date);
		}catch(Exception e){
			logError(WechainApp.getContext(), "Utils:getDateTimePhone - Exception:", e);
		}
		
		return "";
	}
	
	/**
	 * Verifica si el string recibido es nulo o vacío
	 * @param text Texto a validar
	 * @return boolean
	 */
	public static boolean isEmpty(final String text){
		return !(text != null && text.trim().length() > 0 && !text.trim().toLowerCase().equals("null") && !text.trim().equals(""));
	}
	
	private Utils(){}
	
	/**
	 * Returns a human readable String corresponding to a detected activity type.
	 */
	public static String getActivityString(Context context, int detectedActivityType){
		Resources resources = context.getResources();
		
		try{
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
			}
		}catch(Exception e){
			logError(WechainApp.getContext(), "Utils:getActivityString - ", e);
		}
		
		return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
	}
	
	public static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList){
		Type type = new TypeToken<ArrayList<DetectedActivity>>() {}.getType();
		return new Gson().toJson(detectedActivitiesList, type);
	}
	
	public static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray){
		ArrayList<DetectedActivity> detectedActivities = new ArrayList<>();
		
		try{
			Type listType = new TypeToken<ArrayList<DetectedActivity>>(){}.getType();
			detectedActivities = new Gson().fromJson(jsonArray, listType);
			
			if(detectedActivities == null){
				detectedActivities = new ArrayList<>();
			}
		}catch(Exception e){
			logError(WechainApp.getContext(), "Utils:detectedActivitiesFromJson - ", e);
		}
		
		return detectedActivities;
	}
}