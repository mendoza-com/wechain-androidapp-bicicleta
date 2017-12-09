package com.boa.utils;

import android.Manifest;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public final class Common{
	//En true desbloquea la impresión de trackers y variables para debug
	public static final boolean		DEBUG					= true;
	public static final String		WEB						= "https://www.wechain.org";
	//Referencia al diccionario de preferencias para la app actual
	public static final String		KEY_PREF				= "WechainPref";
	public static final String[]	PERMISSIONS				= {	Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE,
		Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
	public static final String		PREF_CURRENT_LAT		= "prefCurrentLat";
	public static final String		PREF_CURRENT_LON		= "prefCurrentLon";
	public static final String		PREF_SELECTED_LAT		= "prefSelectedLat";
	public static final String		PREF_SELECTED_LON		= "prefSelectedLon";
	public static final double		DEFAULT_LAT				= -32.8832979;//Mendoza capital
	public static final double		DEFAULT_LON				= -68.8760287;
	//Referencia para saber si hubo o no sesión iniciada
	public static final String		PREF_SESSION_STARTED	= "prefSessionStarted";
	private static final String PACKAGE_NAME = "com.google.android.gms.location.activityrecognition";
	public static final String KEY_ACTIVITY_UPDATES_REQUESTED = PACKAGE_NAME + ".ACTIVITY_UPDATES_REQUESTED";
	public static final String KEY_DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";
	public static final String ID_GOOGLE = "669546340867-3pd5jcvsfgdd23ijl6scc0nsk30e5qog.apps.googleusercontent.com";//Cert
	/**
	 * The desired time between activity detections. Larger values result in fewer activity
	 * detections while improving battery life. A value of 0 results in activity detections at the
	 * fastest possible rate.
	 */
	public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 30 * 1000; // 30 seconds
	/**
	 * List of DetectedActivity types that we monitor in this sample.
	 */
	public static final int[] MONITORED_ACTIVITIES ={
			/*DetectedActivity.STILL,
			DetectedActivity.ON_FOOT,*/
			DetectedActivity.WALKING,/*
			DetectedActivity.RUNNING,*/
			DetectedActivity.ON_BICYCLE/*,
			DetectedActivity.IN_VEHICLE,
			DetectedActivity.TILTING,
			DetectedActivity.UNKNOWN*/
	};
}