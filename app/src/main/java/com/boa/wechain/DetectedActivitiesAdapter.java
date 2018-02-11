package com.boa.wechain;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.boa.utils.Common;
import com.boa.utils.Utils;
import com.google.android.gms.location.DetectedActivity;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class DetectedActivitiesAdapter extends ArrayAdapter<DetectedActivity>{
	DetectedActivitiesAdapter(Context context, ArrayList<DetectedActivity> detectedActivities){
		super(context, 0, detectedActivities);
	}
	
	@NonNull
	@Override
	public View getView(int position, @Nullable View view, @NonNull ViewGroup parent){
		try{
			DetectedActivity detectedActivity = getItem(position);
			
			if(view == null){
				view = LayoutInflater.from(getContext()).inflate(R.layout.detected_activity, parent, false);
			}
			
			TextView activityName = view.findViewById(R.id.detected_activity_name);
			TextView activityConfidenceLevel = view.findViewById(R.id.detected_activity_confidence_level);
			ProgressBar progressBar = view.findViewById(R.id.detected_activity_progress_bar);
			
			if(detectedActivity != null){
				activityName.setText(Utils.getActivityString(getContext(), detectedActivity.getType()));
				activityConfidenceLevel.setText(getContext().getString(R.string.percent, detectedActivity.getConfidence()));
				//Que se llene toda la barra cuando haya actividad o sea apartir del 15%
				progressBar.setProgress(100);//detectedActivity.getConfidence());
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "DetectedActivitiesAdapter:getView - ", e);
		}
		
		return view;
	}
	
	void updateActivities(ArrayList<DetectedActivity> detectedActivities){
		try{
			HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();
			
			for(DetectedActivity activity : detectedActivities){
				System.out.println("Actividad: "+Utils.getActivityString(getContext(), activity.getType())+"("+activity.getType()+") Level: "+activity.getConfidence());
				detectedActivitiesMap.put(activity.getType(), activity.getConfidence());
			}
			
			ArrayList<DetectedActivity> tempList = new ArrayList<>();
			
			if(Common.TRACK_ALL){
				for(int i = 0; i < Common.MONITORED_ALL_ACTIVITIES.length; i++){
					int confidence = detectedActivitiesMap.containsKey(Common.MONITORED_ALL_ACTIVITIES[i]) ? detectedActivitiesMap.get(Common.MONITORED_ALL_ACTIVITIES[i]) : 0;
					tempList.add(new DetectedActivity(Common.MONITORED_ALL_ACTIVITIES[i], confidence));
					System.out.println(Utils.getActivityString(WechainApp.getContext(), Common.MONITORED_ALL_ACTIVITIES[i])+"("+Common.MONITORED_ALL_ACTIVITIES[i]+") "+confidence+"%");
					Utils.writeStringInFile(Utils.getActivityString(WechainApp.getContext(), Common.MONITORED_ALL_ACTIVITIES[i])+
						"("+Common.MONITORED_ALL_ACTIVITIES[i]+") "+confidence+"%", "");
				}
			}else{
				for(int i = 0; i < Common.MONITORED_ACTIVITIES.length; i++){
					int confidence = detectedActivitiesMap.containsKey(Common.MONITORED_ACTIVITIES[i]) ? detectedActivitiesMap.get(Common.MONITORED_ACTIVITIES[i]) : 0;
					tempList.add(new DetectedActivity(Common.MONITORED_ACTIVITIES[i], confidence));
					System.out.println(Utils.getActivityString(WechainApp.getContext(), Common.MONITORED_ACTIVITIES[i])+"("+Common.MONITORED_ACTIVITIES[i]+") "+confidence+"%");
					//BICI es 1
					
					if(confidence > 15){
						PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).edit().putBoolean("showBar", true).apply();
					}else{
						PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).edit().putBoolean("showBar", false).apply();
					}
				}
			}
			
			this.clear();
			
			for(DetectedActivity detectedActivity : tempList){
				this.add(detectedActivity);
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "DetectedActivitiesAdapter:updateActivities - ", e);
		}
	}
}