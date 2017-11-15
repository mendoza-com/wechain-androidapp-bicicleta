package com.boa.wechain;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
	protected static final String TAG = "MainActivity";
	private Context mContext;
	/**
	 * The entry point for interacting with activity recognition.
	 */
	private ActivityRecognitionClient mActivityRecognitionClient;
	// UI elements.
	private Button mRequestActivityUpdatesButton;
	private Button mRemoveActivityUpdatesButton;
	
	/**
	 * Adapter backed by a list of DetectedActivity objects.
	 */
	private DetectedActivitiesAdapter mAdapter;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState){
		try{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			mContext = this;
			// Get the UI widgets.
			mRequestActivityUpdatesButton = findViewById(R.id.request_activity_updates_button);
			mRemoveActivityUpdatesButton = findViewById(R.id.remove_activity_updates_button);
			
			if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
				List<String> permissionsList = new ArrayList<>();
				
				for(String permission : Common.PERMISSIONS){
					if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
						if(!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
							permissionsList.add(permission);
						}
					}
				}
				
				String[] permissions = new String[permissionsList.size()];
				permissionsList.toArray(permissions);
				
				if(permissions.length > 0){
					int callBack = 0;
					ActivityCompat.requestPermissions(this, permissions, callBack);
				}else{
					init();
				}
			}else{
				init();
			}
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":onCreate - Exception: ", e);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
		try{
			init();
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":onRequestPermissionsResult - Exception: ", e);
		}
	}
	
	public void init(){
		try{
			ListView detectedActivitiesListView = findViewById(R.id.detected_activities_listview);
			// Enable either the Request Updates button or the Remove Updates button depending on
			// whether activity updates have been requested.
			setButtonsEnabledState();
			ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
					PreferenceManager.getDefaultSharedPreferences(this).getString(Common.KEY_DETECTED_ACTIVITIES, ""));
			// Bind the adapter to the ListView responsible for display data for detected activities.
			mAdapter = new DetectedActivitiesAdapter(this, detectedActivities);
			detectedActivitiesListView.setAdapter(mAdapter);
			mActivityRecognitionClient = new ActivityRecognitionClient(this);
		}catch(Exception e){
			Utils.logError(this, getLocalClassName()+":onRequestPermissionsResult - Exception: ", e);
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		updateDetectedActivitiesList();
	}
	
	@Override
	protected void onPause(){
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	
	/**
	 * Registers for activity recognition updates using
	 * {@link ActivityRecognitionClient#requestActivityUpdates(long, PendingIntent)}.
	 * Registers success and failure callbacks.
	 */
	public void requestActivityUpdatesButtonHandler(View view){
		Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(Common.DETECTION_INTERVAL_IN_MILLISECONDS, getActivityDetectionPendingIntent());
		task.addOnSuccessListener(new OnSuccessListener<Void>(){
			@Override
			public void onSuccess(Void result){
				Toast.makeText(mContext, getString(R.string.activity_updates_enabled), Toast.LENGTH_SHORT).show();
				setUpdatesRequestedState(true);
				updateDetectedActivitiesList();
			}
		});
		
		task.addOnFailureListener(new OnFailureListener(){
			@Override
			public void onFailure(@NonNull Exception e){
				Log.w(TAG, getString(R.string.activity_updates_not_enabled));
				Toast.makeText(mContext, getString(R.string.activity_updates_not_enabled), Toast.LENGTH_SHORT).show();
				setUpdatesRequestedState(false);
			}
		});
	}
	
	
	/**
	 * Removes activity recognition updates using
	 * {@link ActivityRecognitionClient#removeActivityUpdates(PendingIntent)}. Registers success and
	 * failure callbacks.
	 */
	public void removeActivityUpdatesButtonHandler(View view){
		Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(getActivityDetectionPendingIntent());
		task.addOnSuccessListener(new OnSuccessListener<Void>(){
			@Override
			public void onSuccess(Void result){
				Toast.makeText(mContext,
						getString(R.string.activity_updates_removed),
						Toast.LENGTH_SHORT)
						.show();
				setUpdatesRequestedState(false);
				// Reset the display.
				mAdapter.updateActivities(new ArrayList<DetectedActivity>());
			}
		});
		
		task.addOnFailureListener(new OnFailureListener(){
			@Override
			public void onFailure(@NonNull Exception e){
				Log.w(TAG, "Failed to enable activity recognition.");
				Toast.makeText(mContext, getString(R.string.activity_updates_not_removed), Toast.LENGTH_SHORT).show();
				setUpdatesRequestedState(true);
			}
		});
	}
	
	/**
	 * Gets a PendingIntent to be sent for each activity detection.
	 */
	private PendingIntent getActivityDetectionPendingIntent(){
		Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
		// We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
		// requestActivityUpdates() and removeActivityUpdates().
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	/**
	 * Ensures that only one button is enabled at any time. The Request Activity Updates button is
	 * enabled if the user hasn't yet requested activity updates. The Remove Activity Updates button
	 * is enabled if the user has requested activity updates.
	 */
	private void setButtonsEnabledState(){
		if(getUpdatesRequestedState()){
			mRequestActivityUpdatesButton.setEnabled(false);
			mRemoveActivityUpdatesButton.setEnabled(true);
		}else{
			mRequestActivityUpdatesButton.setEnabled(true);
			mRemoveActivityUpdatesButton.setEnabled(false);
		}
	}
	
	/**
	 * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity
	 * updates.
	 */
	private boolean getUpdatesRequestedState(){
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.KEY_ACTIVITY_UPDATES_REQUESTED, false);
	}
	
	/**
	 * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
	 * updates.
	 */
	private void setUpdatesRequestedState(boolean requesting){
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Common.KEY_ACTIVITY_UPDATES_REQUESTED, requesting).apply();
		setButtonsEnabledState();
	}
	
	/**
	 * Processes the list of freshly detected activities. Asks the adapter to update its list of
	 * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
	 * activities.
	 */
	protected void updateDetectedActivitiesList(){
		ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
			PreferenceManager.getDefaultSharedPreferences(mContext).getString(Common.KEY_DETECTED_ACTIVITIES, ""));
		mAdapter.updateActivities(detectedActivities);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s){
		if(s.equals(Common.KEY_DETECTED_ACTIVITIES)){
			updateDetectedActivitiesList();
		}
	}
}