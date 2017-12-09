package com.boa.wechain;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import com.boa.utils.Common;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class WechainApp extends MultiDexApplication{
	private static WechainApp instance;
	
	public static Context getContext(){
		return instance;
	}
	
	@Override
	public void onCreate(){
		instance = this;
		super.onCreate();
		Realm.init(this);
		Realm realm = Realm.getDefaultInstance();
		realm.setAutoRefresh(true);
		
		if(!Common.DEBUG){
			Fabric.with(this, new Crashlytics());
			Fabric.with(this, new Answers());
		}
	}
	
	@Override
	protected void attachBaseContext(Context base){
		super.attachBaseContext(base);
		MultiDex.install(this);
	}
}