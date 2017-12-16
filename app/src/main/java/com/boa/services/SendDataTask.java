package com.boa.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.boa.utils.Common;
import com.boa.utils.Utils;
import com.boa.wechain.Exercise;
import com.boa.wechain.WechainApp;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 16 dic 2017.
 */
public class SendDataTask extends AsyncTask<Void, Void, String>{
	private SharedPreferences preferences;
	
	public SendDataTask(){
	}
	
	@Override
	protected String doInBackground(final Void... voids){
		try{
			Realm.init(WechainApp.getContext());
			preferences	= WechainApp.getContext().getSharedPreferences(Common.KEY_PREF, Context.MODE_PRIVATE);
			Realm realm	= Realm.getDefaultInstance();
			RealmResults<Exercise> exercises = realm.where(Exercise.class).findAllSorted("id", Sort);
			realm.close();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "SendDataTask:doInBackground - Exception: ", e);
		}
		
		return null;
	}
}