package com.boa.wechain;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.boa.services.SendDataTask;
import com.boa.utils.Api;
import com.boa.utils.Common;
import com.boa.utils.DetectedActivitiesIntentService;
import com.boa.utils.UserParam;
import com.boa.utils.Utils;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener{
	protected static final String TAG = "MainActivity";
	private ActivityRecognitionClient mActivityRecognitionClient;
	private Button mRequestActivityUpdatesButton, mRemoveActivityUpdatesButton;
	private DetectedActivitiesAdapter mAdapter;
	private static final int RC_SIGN_IN = 123;
	private FirebaseAuth mAuth;
	private GoogleSignInClient mGoogleSignInClient;
	private TextView mStatusTextView, mDetailTextView, tvTotal, tvCount, tvCountDays, tvCountFortnight, tvCountWeek, tvCountMonth, tvBalance;
	private double count = 0.00, countDay = 0.00, countFortnight = 0.00, countWeek = 0.00, countMonth = 0.00;
	private RelativeLayout rlSplash, rlHome;
	private CircleImageView ivUser;
	private Uri img;
	private String name;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState){
		try{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.login);
			mRequestActivityUpdatesButton = findViewById(R.id.request_activity_updates_button);
			mRemoveActivityUpdatesButton = findViewById(R.id.remove_activity_updates_button);
			mStatusTextView = findViewById(R.id.status);
			mDetailTextView = findViewById(R.id.detail);
			tvTotal = findViewById(R.id.tvTotal);
			SignInButton signInButton = findViewById(R.id.sign_in_button);
			signInButton.setSize(SignInButton.SIZE_WIDE);
			signInButton.setOnClickListener(this);
			findViewById(R.id.web).setOnClickListener(this);
			findViewById(R.id.sign_out_button).setOnClickListener(this);
			findViewById(R.id.disconnect_button).setOnClickListener(this);
			rlSplash = findViewById(R.id.rlSplash);
			rlHome = findViewById(R.id.rlHome);
			ivUser = findViewById(R.id.ivUser);
			tvCount = findViewById(R.id.tvCount);
			tvCountDays = findViewById(R.id.tvCountDays);
			tvCountFortnight = findViewById(R.id.tvCountFortnight);
			tvCountWeek = findViewById(R.id.tvCountWeek);
			tvCountMonth = findViewById(R.id.tvCountMonth);
			tvBalance = findViewById(R.id.tvBalance);
			GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
			mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
			mAuth = FirebaseAuth.getInstance();
			
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
			Utils.logError(this, getLocalClassName() + ":onCreate - Exception: ", e);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
		try{
			init();
		}catch(Exception e){
			Utils.logError(this, getLocalClassName() + ":onRequestPermissionsResult - Exception: ", e);
		}
	}
	
	public void init(){
		try{
			ListView detectedActivitiesListView = findViewById(R.id.detected_activities_listview);
			setButtonsEnabledState();
			ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
				PreferenceManager.getDefaultSharedPreferences(this).getString(Common.KEY_DETECTED_ACTIVITIES, ""));
			mAdapter = new DetectedActivitiesAdapter(this, detectedActivities);
			detectedActivitiesListView.setAdapter(mAdapter);
			mActivityRecognitionClient = new ActivityRecognitionClient(this);
		}catch(Exception e){
			Utils.logError(this, getLocalClassName() + ":onRequestPermissionsResult - Exception: ", e);
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		try{
			PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
			updateDetectedActivitiesList();
			
			if(mRequestActivityUpdatesButton.isEnabled() && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.KEY_ACTIVITY_UPDATES_REQUESTED, false)){
				mRequestActivityUpdatesButton.performClick();
			}
			
			//Contamos el total de km recorridos para mostrar
			Realm realm = Realm.getDefaultInstance();
			RealmResults<Exercise> exercises = realm.where(Exercise.class).findAll();
			count = 0;
			countDay = 0;
			countFortnight = 0;
			countWeek = 0;
			countMonth = 0;
			
			if(exercises.size() > 0){
				for(Exercise exercise : exercises){
					count = count + exercise.getDistance();
				}
			}
			
			tvTotal.setText(Html.fromHtml("Km recorridos: <b>"+String.format("%.2f", count/1000)+"</b>"));
			tvCount.setText(Html.fromHtml("<b>"+String.format("%.2f", count/1000)+"</b>"));
			tvCountDays.setText(Html.fromHtml("<b>"+String.format("%.2f", countDay/1000)+" KM</b>"));
			tvCountFortnight.setText(Html.fromHtml("<b>"+String.format("%.2f", countFortnight/1000)+" KM</b>"));
			tvCountWeek.setText(Html.fromHtml("<b>"+String.format("%.2f", countWeek/1000)+" KM</b>"));
			tvCountMonth.setText(Html.fromHtml("<b>"+String.format("%.2f", countMonth/1000)+" KM</b>"));
			System.out.println("Count: "+count+" VAL: "+Common.REWARD_VALUE+" DOU: "+Double.valueOf(Common.REWARD_VALUE));
			tvBalance.setText((count*(Double.valueOf(Common.REWARD_VALUE))/1000)+"");
			realm.close();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onResume - ", e);
		}
	}
	
	@Override
	protected void onPause(){
		try{
			PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onPause - ", e);
		}
		
		super.onPause();
	}
	
	public void requestActivityUpdatesButtonHandler(View view){
		try{
			Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(Common.DETECTION_INTERVAL_IN_MILLISECONDS, getActivityDetectionPendingIntent());
			task.addOnSuccessListener(new OnSuccessListener<Void>(){
				@Override
				public void onSuccess(Void result){
					try{
						setUpdatesRequestedState(true);
						updateDetectedActivitiesList();
					}catch(Exception e){
						Utils.logError(WechainApp.getContext(), getLocalClassName()+":requestActivityUpdatesButtonHandler:onSuccess - ", e);
					}
				}
			});
			
			task.addOnFailureListener(new OnFailureListener(){
				@Override
				public void onFailure(@NonNull Exception e){
					setUpdatesRequestedState(false);
					Utils.logError(WechainApp.getContext(), getLocalClassName()+":requestActivityUpdatesButtonHandler:onFailure - ", e);
				}
			});
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":requestActivityUpdatesButtonHandler - ", e);
		}
	}
	
	public void removeActivityUpdatesButtonHandler(View view){
		try{
			Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(getActivityDetectionPendingIntent());
			task.addOnSuccessListener(new OnSuccessListener<Void>(){
				@Override
				public void onSuccess(Void result){
					try{
						setUpdatesRequestedState(false);
						mAdapter.updateActivities(new ArrayList<DetectedActivity>());
					}catch(Exception e){
						Utils.logError(WechainApp.getContext(), getLocalClassName()+":removeActivityUpdatesButtonHandler:onSuccess - ", e);
					}
				}
			});
			
			task.addOnFailureListener(new OnFailureListener(){
				@Override
				public void onFailure(@NonNull Exception e){
					setUpdatesRequestedState(true);
					Utils.logError(WechainApp.getContext(), getLocalClassName()+":removeActivityUpdatesButtonHandler:onFailure - ", e);
				}
			});
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":removeActivityUpdatesButtonHandler - ", e);
		}
	}
	
	private PendingIntent getActivityDetectionPendingIntent(){
		Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	private void setButtonsEnabledState(){
		try{
			if(getUpdatesRequestedState()){
				mRequestActivityUpdatesButton.setEnabled(false);
				mRemoveActivityUpdatesButton.setEnabled(true);
			}else{
				mRequestActivityUpdatesButton.setEnabled(true);
				mRemoveActivityUpdatesButton.setEnabled(false);
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":setButtonsEnabledState - ", e);
		}
	}
	
	private boolean getUpdatesRequestedState(){
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Common.KEY_ACTIVITY_UPDATES_REQUESTED, false);
	}
	
	private void setUpdatesRequestedState(boolean requesting){
		try{
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Common.KEY_ACTIVITY_UPDATES_REQUESTED, requesting).apply();
			setButtonsEnabledState();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":setUpdatesRequestedState - ", e);
		}
	}
	
	protected void updateDetectedActivitiesList(){
		try{
			ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
				PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).getString(Common.KEY_DETECTED_ACTIVITIES, ""));
			mAdapter.updateActivities(detectedActivities);
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":updateDetectedActivitiesList - ", e);
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s){
		try{
			if(s.equals(Common.KEY_DETECTED_ACTIVITIES)){
				updateDetectedActivitiesList();
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onSharedPreferenceChanged - ", e);
		}
	}
	
	@VisibleForTesting
	public ProgressDialog mProgressDialog;
	
	public void showProgressDialog(){
		try{
			if(mProgressDialog == null){
				mProgressDialog = new ProgressDialog(this);
				mProgressDialog.setMessage("Cargando...");
				mProgressDialog.setIndeterminate(true);
			}
			
			mProgressDialog.show();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":showProgressDialog - ", e);
		}
	}
	
	public void hideProgressDialog(){
		try{
			if(mProgressDialog != null && mProgressDialog.isShowing()){
				mProgressDialog.dismiss();
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":hideProgressDialog - ", e);
		}
	}
	
	@Override
	public void onStop(){
		super.onStop();
		try{
			hideProgressDialog();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onStop - ", e);
		}
	}
	
	@Override
	public void onClick(View view){
		try{
			switch(view.getId()){
				case R.id.sign_in_button:
					signIn();
				break;
				
				case R.id.sign_out_button:
					signOut();
				break;
				
				case R.id.disconnect_button:
					revokeAccess();
				break;
				
				case R.id.web:
					//Conseguir el id para redirigir url
					if(Utils.isEmpty(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).getString("id",""))){
						new Thread(new Runnable(){
							@Override
							public void run(){
								UserParam param = new UserParam();
								param.setEmail(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).getString("email", ""));
								Api.getIt().register(param, new Api.ApiCallback(){
									@Override
									public void onLoaded(String object){
										saveId(object);
										runOnUiThread(new Runnable(){
											@Override
											public void run(){
												startActivity(new Intent(MainActivity.this, WebActivity.class));
											}
										});
									}
									
									@Override
									public void onError(Throwable t){
										Utils.logError(WechainApp.getContext(), getLocalClassName()+":onClick:onError - ", (Exception) t);
									}
									
									@Override
									public void onConnectionError(){
										System.out.println(getLocalClassName()+":onClick:onConnectionError - ");
									}
								});
							}
						}).start();
					}else{
						startActivity(new Intent(MainActivity.this, WebActivity.class));
					}
				break;
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onClick - ", e);
		}
	}
	
	public void saveId(String object){
		try{
			if(Utils.isEmpty(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).getString("id",""))){
				JSONObject jsonObject = new JSONObject(object);
				
				if(jsonObject.has("data")){
					if(!jsonObject.isNull("data")){
						if(jsonObject.getJSONObject("data") != null){
							if(jsonObject.getJSONObject("data").has("_id")){
								if(!jsonObject.getJSONObject("data").isNull("_id")){
									if(!Utils.isEmpty(jsonObject.getJSONObject("data").getString("_id"))){
										PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext()).edit().putString("id", jsonObject.getJSONObject("data")
											.getString("_id")).apply();
									}
								}
							}
						}
					}
				}
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":saveId - ", e);
		}
	}
	
	@Override
	public void onPointerCaptureChanged(boolean hasCapture){
	}
	
	@Override
	public void onStart(){
		super.onStart();
		try{
			FirebaseUser currentUser = mAuth.getCurrentUser();
			updateUI(currentUser);
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onStart - ", e);
		}
	}
	
	private void signIn(){
		try{
			List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
			startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(), RC_SIGN_IN);
			showProgressDialog();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":signIn - ", e);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		try{
			System.out.println("requestCode: "+requestCode+" resultCode: "+resultCode+" RC_SIGN_IN: "+RC_SIGN_IN+" OK: "+ResultCodes.OK);
			
			if(requestCode == RC_SIGN_IN){
				IdpResponse response = IdpResponse.fromResultIntent(data);
				
				if(resultCode == ResultCodes.OK){
					FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
					
					if(user != null){
						System.out.println("user: "+user.toString());
						System.out.println("displayname: "+user.getDisplayName());
						System.out.println("email: "+user.getEmail());
					}
					
					updateUI(user);
				}else{
					Log.w(TAG, "Google sign in failed");
					updateUI(null);
				}
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onActivityResult - ", e);
		}
	}
	
	private void signOut(){
		try{
			mAuth.signOut();
			mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>(){
				@Override
				public void onComplete(@NonNull Task<Void> task){
					updateUI(null);
				}
			});
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":signOut - ", e);
		}
	}
	
	private void revokeAccess(){
		try{
			mAuth.signOut();
			mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, new OnCompleteListener<Void>(){
				@Override
				public void onComplete(@NonNull Task<Void> task){
					updateUI(null);
				}
			});
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":revokeAccess - ", e);
		}
	}
	
	private void updateUI(FirebaseUser user){
		try{
			hideProgressDialog();
			findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
			mDetailTextView.setVisibility(View.VISIBLE);
			
			if(user != null){
				rlHome.setVisibility(RelativeLayout.VISIBLE);
				rlSplash.setVisibility(RelativeLayout.GONE);
				mStatusTextView.setText(getString(R.string.google_status_fmt, user.getEmail()));
				findViewById(R.id.sign_in_button).setVisibility(View.GONE);
				GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
				
				if(acct != null){
					String personName = acct.getDisplayName();
					String personGivenName = acct.getGivenName();
					String personFamilyName = acct.getFamilyName();
					final String personEmail = acct.getEmail();
					String personId = acct.getId();
					Uri personPhoto = acct.getPhotoUrl();
					img = personPhoto;
					name = personName;
					Picasso.with(WechainApp.getContext()).load(personPhoto).placeholder(R.drawable.ic_shortcut_person).into(ivUser);
					mStatusTextView.setText(getString(R.string.google_status_fmt, personEmail));
					mStatusTextView.setVisibility(View.VISIBLE);
					
					if(Common.DEBUG){
						System.out.println("personName: "+personName+"\npersonGivenName: "+personGivenName+"\npersonFamilyName: "+personFamilyName+"\npersonEmail: "+personEmail
							+"\npersonId: "+personId+"\npersonPhoto: "+personPhoto);
					}
					
					if(Utils.isEmpty(PreferenceManager.getDefaultSharedPreferences(this).getString("email", ""))){
						PreferenceManager.getDefaultSharedPreferences(this).edit().putString("email", personEmail).apply();
						new Thread(new Runnable(){
							@Override
							public void run(){
								UserParam param = new UserParam();
								param.setEmail(personEmail);
								Api.getIt().register(param, new Api.ApiCallback(){
									@Override
									public void onLoaded(String object){
										saveId(object);
										Realm realm = Realm.getDefaultInstance();
										realm.executeTransaction(new Realm.Transaction(){
											@Override
											public void execute(Realm realm){
												long ts = System.currentTimeMillis();
												Exercise exercise = new Exercise();
												exercise.setId(ts);
												exercise.setInitLat(Double.valueOf(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext())
													.getString(Common.PREF_SELECTED_LAT, "")));
												exercise.setInitLon(Double.valueOf(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext())
													.getString(Common.PREF_SELECTED_LON, "")));
												exercise.setEndLat(Double.valueOf(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext())
													.getString(Common.PREF_CURRENT_LAT, "")));
												exercise.setEndLon(Double.valueOf(PreferenceManager.getDefaultSharedPreferences(WechainApp.getContext())
													.getString(Common.PREF_CURRENT_LON, "")));
												exercise.setDistance(0.001);
												exercise.setStatus(Exercise.STATUS_PENDING);
												exercise.setEmail(personEmail);
												realm.copyToRealmOrUpdate(exercise);
											}
										});
										realm.close();
									}
									
									@Override
									public void onError(Throwable t){
										Utils.logError(WechainApp.getContext(), getLocalClassName()+":updateUI:onError - ", (Exception) t);
									}
									
									@Override
									public void onConnectionError(){
										System.out.println(getLocalClassName()+":updateUI:onConnectionError - ");
									}
								});
							}
						}).start();
					}
				}
			}else{
				rlHome.setVisibility(RelativeLayout.GONE);
				rlSplash.setVisibility(RelativeLayout.VISIBLE);
				mStatusTextView.setText("Salir");
				mDetailTextView.setText(null);
				findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
				mStatusTextView.setVisibility(View.GONE);
			}
			
			if(Common.DEBUG){
				mStatusTextView.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View view){
						count = count+0.1;
						tvTotal.setText("Km recorridos: "+count/1000);
						tvCount.setText(""+count/1000);
						new SendDataTask(true).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
					}
				});
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":updateUI - ", e);
		}
	}
}