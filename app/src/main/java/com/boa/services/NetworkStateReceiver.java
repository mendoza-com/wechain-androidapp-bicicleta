package com.boa.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.boa.utils.Common;
import io.realm.Realm;

/**
 * Receiver de cambio en la conexión para intentar reportar a api
 * Created by Boa (David Figueroa davo.figueroa14@gmail.com) on 23/12/2016.
 */
public class NetworkStateReceiver extends BroadcastReceiver{
	private static boolean proccess			= false;
	private static boolean firstConnection	= false;
	
	@Override
	@SuppressWarnings("deprecation")
	public void onReceive(final Context context, final Intent intent){
		intent.getAction();
		
		if(proccess || firstConnection && !isConnected(context)){
			return;
		}
		
		if(isConnected(context)){
			proccess		= true;
			firstConnection	= true;
			Realm.init(context);
			/*Realm realm		= Realm.getDefaultInstance();
			long total2Send	= realm.where(Infringement.class).lessThan(Common.KEY_STATUS, Infringement.STATUS_SENDED).count();//Reemplazar por days a enviar
			realm.close();
			
			if(Common.DEBUG){
				System.out.println("Total a enviar en background: "+total2Send);
			}
			
			if(total2Send > 0){
				new SendFormTask(context, 0, "", null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}*/
			
			proccess		= false;
			firstConnection	= false;
		}
	}
	
	public static boolean isConnected(Context context){
		ConnectivityManager cm		= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork	= cm.getActiveNetworkInfo();
		
		if(activeNetwork != null){
			if(Common.DEBUG){
				System.out.println(	"isConnected: "+activeNetwork.isConnected()+ " isFailover: "+activeNetwork.isFailover()+
						" isConnectedOrConnecting: "+activeNetwork.isConnectedOrConnecting()+ " isRoaming: "+activeNetwork.isRoaming());
			}
			
			return activeNetwork.isConnected();
		}else{
			if(Common.DEBUG){
				System.out.println("No hay red activa!");
			}
			
			return false;
		}
	}
}