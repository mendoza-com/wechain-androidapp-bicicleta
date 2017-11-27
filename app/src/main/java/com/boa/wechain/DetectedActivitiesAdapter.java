package com.boa.wechain;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.boa.wechain.utils.Common;
import com.boa.wechain.utils.Utils;
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
			
			// Find the UI widgets.
			TextView activityName = view.findViewById(R.id.detected_activity_name);
			TextView activityConfidenceLevel = view.findViewById(R.id.detected_activity_confidence_level);
			ProgressBar progressBar = view.findViewById(R.id.detected_activity_progress_bar);
			
			// Populate widgets with values.
			if(detectedActivity != null){
				activityName.setText(Utils.getActivityString(getContext(), detectedActivity.getType()));
				activityConfidenceLevel.setText(getContext().getString(R.string.percent, detectedActivity.getConfidence()));
				progressBar.setProgress(detectedActivity.getConfidence());
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "DetectedActivitiesAdapter:getView - ", e);
		}
		
		return view;
	}
	
	/**
	 * Process list of recently detected activities and updates the list of {@code DetectedActivity}
	 * objects backing this adapter.
	 *
	 * @param detectedActivities the freshly detected activities
	 */
	void updateActivities(ArrayList<DetectedActivity> detectedActivities){
		try{
			HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();
			
			for(DetectedActivity activity : detectedActivities){
				detectedActivitiesMap.put(activity.getType(), activity.getConfidence());
			}
			
			// Every time we detect new activities, we want to reset the confidence level of ALL
			// activities that we monitor. Since we cannot directly change the confidence
			// of a DetectedActivity, we use a temporary list of DetectedActivity objects. If an
			// activity was freshly detected, we use its confidence level. Otherwise, we set the
			// confidence level to zero.
			ArrayList<DetectedActivity> tempList = new ArrayList<>();
			
			for(int i = 0; i < Common.MONITORED_ACTIVITIES.length; i++){
				int confidence = detectedActivitiesMap.containsKey(Common.MONITORED_ACTIVITIES[i]) ? detectedActivitiesMap.get(Common.MONITORED_ACTIVITIES[i]) : 0;
				tempList.add(new DetectedActivity(Common.MONITORED_ACTIVITIES[i], confidence));
			}
			
			// Remove all items.
			this.clear();
			
			// Adding the new list items notifies attached observers that the underlying data has
			// changed and views reflecting the data should refresh.
			for(DetectedActivity detectedActivity : tempList){
				this.add(detectedActivity);
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "DetectedActivitiesAdapter:updateActivities - ", e);
		}
	}
}