package com.boa.wechain;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.boa.wechain.activities.WebActivity;
import com.boa.wechain.utils.Api;
import com.boa.wechain.utils.Common;
import com.boa.wechain.utils.DetectedActivitiesIntentService;
import com.boa.wechain.utils.UserParam;
import com.boa.wechain.utils.Utils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 15 nov 2017.
 */
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener{
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
	private static final int RC_SIGN_IN = 123;
	private FirebaseAuth mAuth;
	private GoogleSignInClient mGoogleSignInClient;
	private TextView mStatusTextView;
	private TextView mDetailTextView;
	
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
			mStatusTextView = findViewById(R.id.status);
			mDetailTextView = findViewById(R.id.detail);
			SignInButton signInButton = findViewById(R.id.sign_in_button);
			signInButton.setSize(SignInButton.SIZE_STANDARD);
			signInButton.setOnClickListener(this);
			findViewById(R.id.web).setOnClickListener(this);
			findViewById(R.id.sign_out_button).setOnClickListener(this);
			findViewById(R.id.disconnect_button).setOnClickListener(this);
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
			Utils.logError(this, getLocalClassName() + ":onRequestPermissionsResult - Exception: ", e);
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		try{
			PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
			updateDetectedActivitiesList();
			
			if(mRequestActivityUpdatesButton.isEnabled()){
				mRequestActivityUpdatesButton.performClick();
			}
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
	
	/**
	 * Registers for activity recognition updates using
	 * {@link ActivityRecognitionClient#requestActivityUpdates(long, PendingIntent)}.
	 * Registers success and failure callbacks.
	 */
	public void requestActivityUpdatesButtonHandler(View view){
		try{
			Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(Common.DETECTION_INTERVAL_IN_MILLISECONDS, getActivityDetectionPendingIntent());
			task.addOnSuccessListener(new OnSuccessListener<Void>(){
				@Override
				public void onSuccess(Void result){
					try{
						//Toast.makeText(mContext, getString(R.string.activity_updates_enabled), Toast.LENGTH_SHORT).show();
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
					Log.w(TAG, getString(R.string.activity_updates_not_enabled));
					//Toast.makeText(mContext, getString(R.string.activity_updates_not_enabled), Toast.LENGTH_SHORT).show();
					setUpdatesRequestedState(false);
					Utils.logError(WechainApp.getContext(), getLocalClassName()+":requestActivityUpdatesButtonHandler:onFailure - ", e);
				}
			});
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":requestActivityUpdatesButtonHandler - ", e);
		}
	}
	
	/**
	 * Removes activity recognition updates using
	 * {@link ActivityRecognitionClient#removeActivityUpdates(PendingIntent)}. Registers success and
	 * failure callbacks.
	 */
	public void removeActivityUpdatesButtonHandler(View view){
		try{
			Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(getActivityDetectionPendingIntent());
			task.addOnSuccessListener(new OnSuccessListener<Void>(){
				@Override
				public void onSuccess(Void result){
					try{
						Toast.makeText(mContext, getString(R.string.activity_updates_removed), Toast.LENGTH_SHORT).show();
						setUpdatesRequestedState(false);
						// Reset the display.
						mAdapter.updateActivities(new ArrayList<DetectedActivity>());
					}catch(Exception e){
						Utils.logError(WechainApp.getContext(), getLocalClassName()+":removeActivityUpdatesButtonHandler:onSuccess - ", e);
					}
				}
			});
			
			task.addOnFailureListener(new OnFailureListener(){
				@Override
				public void onFailure(@NonNull Exception e){
					Log.w(TAG, "Failed to enable activity recognition.");
					Toast.makeText(mContext, getString(R.string.activity_updates_not_removed), Toast.LENGTH_SHORT).show();
					setUpdatesRequestedState(true);
					Utils.logError(WechainApp.getContext(), getLocalClassName()+":removeActivityUpdatesButtonHandler:onFailure - ", e);
				}
			});
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":removeActivityUpdatesButtonHandler - ", e);
		}
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
		try{
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(Common.KEY_ACTIVITY_UPDATES_REQUESTED, requesting).apply();
			setButtonsEnabledState();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":setUpdatesRequestedState - ", e);
		}
	}
	
	/**
	 * Processes the list of freshly detected activities. Asks the adapter to update its list of
	 * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
	 * activities.
	 */
	protected void updateDetectedActivitiesList(){
		try{
			ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
					PreferenceManager.getDefaultSharedPreferences(mContext).getString(Common.KEY_DETECTED_ACTIVITIES, ""));
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
					System.out.println("WEB!");
					startActivity(new Intent(MainActivity.this, WebActivity.class));
				break;
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":onClick - ", e);
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
					// Successfully signed in
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
			mGoogleSignInClient.signOut().addOnCompleteListener(this,
					new OnCompleteListener<Void>(){
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
			mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
					new OnCompleteListener<Void>(){
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
					mStatusTextView.setText(getString(R.string.google_status_fmt, personEmail));
					mStatusTextView.setVisibility(View.VISIBLE);
					
					if(Utils.isEmpty(PreferenceManager.getDefaultSharedPreferences(this).getString("email", ""))){
						PreferenceManager.getDefaultSharedPreferences(this).edit().putString("email", personEmail).apply();
						new Thread(new Runnable(){
							@Override
							public void run(){
								UserParam param = new UserParam();
								param.setEmail(personEmail);
								Api.getIt().register(param, new Api.ApiCallback(){
									@Override
									public void onLoaded(Object object){
										System.out.println(getLocalClassName()+":updateUI:onLoaded - "+object);
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
				mStatusTextView.setText("Salir");
				mDetailTextView.setText(null);
				findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
				mStatusTextView.setVisibility(View.GONE);
			}
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), getLocalClassName()+":updateUI - ", e);
		}
	}
}