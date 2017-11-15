package com.boa.wechain;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import com.boa.wechain.Common;
import com.boa.wechain.Utils;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.ArrayList;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class DetectedActivitiesIntentService extends IntentService{
	protected static final String TAG = "DetectedActivitiesIS";
	
	/**
	 * This constructor is required, and calls the super IntentService(String)
	 * constructor with the name for a worker thread.
	 */
	public DetectedActivitiesIntentService(){
		// Use the TAG to name the worker thread.
		super(TAG);
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
	}
	
	/**
	 * Handles incoming intents.
	 * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
	 *               is called.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void onHandleIntent(Intent intent){
		ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
		// Get the list of the probable activities associated with the current state of the
		// device. Each activity is associated with a confidence level, which is an int between
		// 0 and 100.
		ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
		PreferenceManager.getDefaultSharedPreferences(this).edit()
			.putString(Common.KEY_DETECTED_ACTIVITIES, Utils.detectedActivitiesToJson(detectedActivities)).apply();
		// Log each activity.
		Log.i(TAG, "activities detected");
		for(DetectedActivity da : detectedActivities){
			Log.i(TAG, Utils.getActivityString(
					getApplicationContext(),
					da.getType()) + " " + da.getConfidence() + "%"
			);
		}
	}
}