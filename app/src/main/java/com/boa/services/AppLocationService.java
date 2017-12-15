package com.boa.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import com.boa.utils.Common;
import com.boa.utils.Utils;
import com.boa.wechain.WechainApp;
import java.util.List;

/**
 * Servicio para re-localizar la app según movimiento y tiempo
 * Created by Boa (David Figueroa davo.figueroa14@gmail.com) on 15/04/2017.
 */
public class AppLocationService extends Service implements LocationListener{
	protected LocationManager	locationManager;
	public Location				location;
	public static final long	MIN_DISTANCE_FOR_UPDATE	= 10000;
	private static final long	MIN_TIME_FOR_UPDATE		= 10000 * 60 * 2;
	
	public AppLocationService(Context context){
		locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
	}
	
	public Location getLocation(String provider){
		try{
			if(locationManager.isProviderEnabled(provider)){
				if(	ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
					ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
					locationManager.requestLocationUpdates(provider, MIN_TIME_FOR_UPDATE, MIN_DISTANCE_FOR_UPDATE, this);
					
					if(locationManager != null){
						location = locationManager.getLastKnownLocation(provider);
						return location;
					}
				}
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "AppLocationService:getLocation - Exception: ", e);
		}
		
		return null;
	}
	
	@Override
	public void onLocationChanged(Location location){
		try{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext());
			double lat = Common.DEFAULT_LAT, lng = Common.DEFAULT_LON;
			
			if(location != null){
				lat								= location.getLatitude();
				lng								= location.getLongitude();
				SharedPreferences.Editor editor	= preferences.edit();
				editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
				editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
				editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
				editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
				editor.apply();
			}
			
			if(Common.DEBUG){
				System.out.println("Auto Default Location Latitude: "+Common.DEFAULT_LAT+"  Longitude: "+Common.DEFAULT_LON);
				System.out.println("Auto Current Location Latitude: "+lat+" Longitude: "+lng);
				System.out.println("Auto Last Location Latitude: "+preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))+
					" Longitude: "+preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON)));
				//TODO Quitar al finalizar pruebas
				Utils.writeStringInFile("Auto Default Location Latitude: "+Common.DEFAULT_LAT+"  Longitude: "+Common.DEFAULT_LON+"\n"+
					"Auto Current Location Latitude: "+lat+" Longitude: "+lng+"\n"+
					"Auto Last Location Latitude: "+preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))+
					" Longitude: "+preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON))+"\nAuto Metros: "+
					meterDistanceBetweenPoints(Float.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))),
						Float.valueOf(preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON))),
						Float.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT))),
						Float.valueOf(preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON))))
					+"\nAuto Distancia: "+distanceBetween(Float.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))),
						Float.valueOf(preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON))),
						Float.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT))),
						Float.valueOf(preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON))))
				, "");
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "AppLocationService:onLocationChanged - Exception: ", e);
		}
	}
	
	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras){
	}
	
	@Override
	public void onProviderDisabled(String provider){
	}
	
	@Override
	public void onProviderEnabled(String provider){
	}
	
	@Nullable
	@Override
	public IBinder onBind(final Intent intent){
		return null;
	}
	
	public static void getCurrentPosition(){
		double lat = Common.DEFAULT_LAT, lng = Common.DEFAULT_LON;
		
		try{
			SharedPreferences preferences	= PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext());
			Criteria criteria				= new Criteria();
			LocationManager locationManager	= (LocationManager) WechainApp.getContext().getSystemService(Context.LOCATION_SERVICE);
			
			if(locationManager != null){
				String provider = locationManager.getBestProvider(criteria, false);
				
				if(provider != null){
					if(	ContextCompat.checkSelfPermission(WechainApp.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
						ContextCompat.checkSelfPermission(WechainApp.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
						Location location = locationManager.getLastKnownLocation(provider);
						
						if(location != null){
							lat = location.getLatitude();
							lng = location.getLongitude();
							SharedPreferences.Editor editor = preferences.edit();
							editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
							editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
							editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
							editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
							editor.apply();
						}
					}
				}else{
					//Buscamos con otro service
					AppLocationService appLocationService	= new AppLocationService(WechainApp.getContext());
					Location newLocation					= appLocationService.getLocation(LocationManager.GPS_PROVIDER);
					
					if(newLocation != null){
						lat	= newLocation.getLatitude();
						lng	= newLocation.getLongitude();
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
						editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
						editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
						editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
						editor.apply();
						
						if(Common.DEBUG){
							System.out.println("Location (GPS): Latitude: " + lat+ " Longitude: " + lng);
						}
					}else{
						newLocation = appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);
						
						if(newLocation != null){
							lat	= newLocation.getLatitude();
							lng	= newLocation.getLongitude();
							SharedPreferences.Editor editor = preferences.edit();
							editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
							editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
							editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
							editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
							editor.apply();
							
							if(Common.DEBUG){
								System.out.println("Location (NW): Latitude: " + lat+ " Longitude: " + lng);
							}
						}
					}
				}
			}else{
				//Buscamos con otro service
				AppLocationService appLocationService	= new AppLocationService(WechainApp.getContext());
				Location newLocation					= appLocationService.getLocation(LocationManager.GPS_PROVIDER);
				
				if(newLocation != null){
					lat	= newLocation.getLatitude();
					lng	= newLocation.getLongitude();
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
					editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
					editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
					editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
					editor.apply();
					
					if(Common.DEBUG){
						System.out.println("Location (GPS): Latitude: " + lat+ " Longitude: " + lng);
					}
				}else{
					newLocation = appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);
					
					if(newLocation != null){
						lat	= newLocation.getLatitude();
						lng	= newLocation.getLongitude();
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
						editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
						editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
						editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
						editor.apply();
						
						if(Common.DEBUG){
							System.out.println("Location (NW): Latitude: " + lat+ " Longitude: " + lng);
						}
					}
				}
			}
			
			if(lat == Common.DEFAULT_LAT && lng == Common.DEFAULT_LON){
				if(locationManager != null){
					String bestProvider	= locationManager.getBestProvider(criteria, false);
					locationManager.getLastKnownLocation(bestProvider);
					Location location	= locationManager.getLastKnownLocation(bestProvider);
					
					if(location != null){
						lat = location.getLatitude();
						lng = location.getLongitude();
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
						editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
						editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
						editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
						editor.apply();
					}else{
						List<String> providers = locationManager.getAllProviders();
						
						if(providers.size() > 0){
							for(String provider : providers){
								location = locationManager.getLastKnownLocation(provider);
								
								if(location != null){
									if(Common.DEBUG){
										System.out.println("Provider: "+provider+" Current Location Latitude: "+location.getLatitude()+" Longitude: "+location.getLongitude());
									}
									
									lat = location.getLatitude();
									lng = location.getLongitude();
									SharedPreferences.Editor editor = preferences.edit();
									editor.putString(Common.PREF_SELECTED_LAT, preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT)));
									editor.putString(Common.PREF_SELECTED_LON, preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)));
									editor.putString(Common.PREF_CURRENT_LAT, String.valueOf(lat));
									editor.putString(Common.PREF_CURRENT_LON, String.valueOf(lng));
									editor.apply();
								}
							}
						}
					}
				}
			}
			
			if(Common.DEBUG){
				System.out.println("Force Default Location Latitude: "+Common.DEFAULT_LAT+"  Longitude: "+Common.DEFAULT_LON);
				System.out.println("Force Current Location Latitude: "+lat+" Longitude: "+lng);
				System.out.println("Force Last Location Latitude: "+preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))+
					" Longitude: "+preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON)));
				//TODO Quitar al finalizar pruebas
				Utils.writeStringInFile("Force Default Location Latitude: "+Common.DEFAULT_LAT+"  Longitude: "+Common.DEFAULT_LON+"\n"+
					"Force Current Location Latitude: "+lat+" Longitude: "+lng+"\n"+
					"Force Last Location Latitude: "+preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))+
					" Longitude: "+preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON))+"\nForce Metros: "+
						meterDistanceBetweenPoints(Float.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))),
							Float.valueOf(preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON))),
							Float.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT))),
							Float.valueOf(preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON))))
					+"\nForce Distancia: "+distanceBetween(Float.valueOf(preferences.getString(Common.PREF_SELECTED_LAT, String.valueOf(Common.DEFAULT_LAT))),
						Float.valueOf(preferences.getString(Common.PREF_SELECTED_LON, String.valueOf(Common.DEFAULT_LON))),
						Float.valueOf(preferences.getString(Common.PREF_CURRENT_LAT, String.valueOf(Common.DEFAULT_LAT))),
						Float.valueOf(preferences.getString(Common.PREF_CURRENT_LON, String.valueOf(Common.DEFAULT_LON)))), "");
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "AppLocationService:getCurrentPosition - Exception: ", e);
		}
	}
	
	public static double meterDistanceBetweenPoints(float initLat, float initLon, float endLat, float endLon){
		try{
			float pk = (float) (180.f/Math.PI);
			float a1 = initLat / pk;
			float a2 = initLon / pk;
			float b1 = endLat / pk;
			float b2 = endLon / pk;
			double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
			double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
			double t3 = Math.sin(a1) * Math.sin(b1);
			double tt = Math.acos(t1 + t2 + t3);
			return 6366000 * tt;
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "AppLocationService:meterDistanceBetweenPoints - Exception: ", e);
		}
		
		return 0;
	}
	
	public static float distanceBetween(float initLat, float initLon, float endLat, float endLon){
		try{
			Location locationA = new Location("start");
			locationA.setLatitude(initLat);
			locationA.setLongitude(initLon);
			Location locationB = new Location("end");
			locationB.setLatitude(endLat);
			locationB.setLongitude(endLon);
			return locationA.distanceTo(locationB);
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "AppLocationService:distanceBetween - Exception: ", e);
		}
		
		return 0;
	}
}