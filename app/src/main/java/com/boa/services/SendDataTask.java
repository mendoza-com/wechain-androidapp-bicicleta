package com.boa.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import com.boa.utils.Api;
import com.boa.utils.Common;
import com.boa.utils.TxParam;
import com.boa.utils.Utils;
import com.boa.wechain.Exercise;
import com.boa.wechain.WechainApp;
import java.util.ArrayList;
import java.util.List;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 16 dic 2017.
 */
public class SendDataTask extends AsyncTask<Void, Void, String>{
	private SharedPreferences preferences;
	private boolean isTest = false;
	
	public SendDataTask(){
	}
	
	public SendDataTask(boolean isTest){
		this.isTest = isTest;
	}
	
	@Override
	protected String doInBackground(final Void... voids){
		try{
			Realm.init(WechainApp.getContext());
			preferences	= WechainApp.getContext().getSharedPreferences(Common.KEY_PREF, Context.MODE_PRIVATE);
			final Realm realm	= Realm.getDefaultInstance();
			RealmResults<Exercise> exercises = realm.where(Exercise.class).notEqualTo("status", Exercise.STATUS_SENDED).findAllSorted("id", Sort.ASCENDING);
			System.out.println("Pendientes para enviar: "+exercises.size());
			final List<Long> ids = new ArrayList<>();
			double totalTrip = 0;
			
			if(exercises.size() > 0){
				for(Exercise exercise : exercises){
					if(totalTrip <= 1000){
						totalTrip = exercise.getDistance()+totalTrip;
						ids.add(exercise.getId());
					}else{
						break;
					}
				}
				
				if(totalTrip <= 1000 && ids.size() > 0){
					//Enviar a api
					TxParam param = new TxParam();
					param.setTo(preferences.getString("email", "df@kwan.com.ar"));
					param.setAsset("co2");
					param.setAmount("0.00001");
					param.setPayload("Recompensa Wechain por tu km recorrido en bici");
					param.setCallbackURL("https://www.site.com/callback");
					Api.getIt().reward(param, new Api.ApiCallback(){
						@Override
						public void onLoaded(Object object){
							System.out.println("SendDataTask:doInBackground:onLoaded: - "+object.toString());
							//En callback de api procesamos y marcamos
							realm.executeTransaction(new Realm.Transaction(){
								@Override
								public void execute(Realm realm){
									for(Long id : ids){
										Exercise exercise = realm.where(Exercise.class).equalTo("id", id).findFirst();
										
										if(exercise != null){
											exercise.setStatus(Exercise.STATUS_SENDED);
										}
									}
								}
							});
						}
						
						@Override
						public void onError(Throwable t){
							Utils.logError(WechainApp.getContext(), "SendDataTask:doInBackground:onError - Exception: ", (Exception) t);
						}
						
						@Override
						public void onConnectionError(){
							System.out.println("SendDataTask:doInBackground:onConnectionError - ");
						}
					});
				}
			}
			
			realm.close();
			
			if(isTest){
				TxParam param = new TxParam();
				param.setTo(preferences.getString("email", "df@kwan.com.ar"));
				param.setAsset("co2");
				param.setAmount("0.00001");
				param.setPayload("Recompensa Wechain por tu km recorrido en bici");
				param.setCallbackURL("https://www.site.com/callback");
				Api.getIt().reward(param, new Api.ApiCallback(){
					@Override
					public void onLoaded(Object object){
						System.out.println("SendDataTask:doInBackground:onLoaded: - "+object.toString());
					}
					
					@Override
					public void onError(Throwable t){
						Utils.logError(WechainApp.getContext(), "SendDataTask:doInBackground:onError - Exception: ", (Exception) t);
					}
					
					@Override
					public void onConnectionError(){
						System.out.println("SendDataTask:doInBackground:onConnectionError - ");
					}
				});
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "SendDataTask:doInBackground - Exception: ", e);
		}
		
		return null;
	}
}