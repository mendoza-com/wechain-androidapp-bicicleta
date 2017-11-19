package com.boa.wechain;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

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
			findViewById(R.id.sign_in_button).setOnClickListener(this);
			findViewById(R.id.sign_out_button).setOnClickListener(this);
			findViewById(R.id.disconnect_button).setOnClickListener(this);
			GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(Common.ID_GOOGLE).requestEmail().build();
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
	
	@VisibleForTesting
	public ProgressDialog mProgressDialog;
	
	public void showProgressDialog(){
		if(mProgressDialog == null){
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setMessage("Cargando...");
			mProgressDialog.setIndeterminate(true);
		}
		
		mProgressDialog.show();
	}
	
	public void hideProgressDialog(){
		if(mProgressDialog != null && mProgressDialog.isShowing()){
			mProgressDialog.dismiss();
		}
	}
	
	@Override
	public void onStop(){
		super.onStop();
		hideProgressDialog();
	}
	
	@Override
	public void onClick(View view){
		int i = view.getId();
		if(i == R.id.sign_in_button){
			signIn();
		}else if(i == R.id.sign_out_button){
			signOut();
		}else if(i == R.id.disconnect_button){
			revokeAccess();
		}
	}
	
	@Override
	public void onPointerCaptureChanged(boolean hasCapture){
	}
	
	@Override
	public void onStart(){
		super.onStart();
		// Check if user is signed in (non-null) and update UI accordingly.
		FirebaseUser currentUser = mAuth.getCurrentUser();
		updateUI(currentUser);
	}
	
	private void signIn(){
		//Intent signInIntent = mGoogleSignInClient.getSignInIntent();
		//startActivityForResult(signInIntent, RC_SIGN_IN);
		List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
		startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build(), RC_SIGN_IN);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SIGN_IN) {
			IdpResponse response = IdpResponse.fromResultIntent(data);
			
			if(resultCode == ResultCodes.OK){
				// Successfully signed in
				FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
				if(user != null){
					System.out.println("user: "+user.toString());
					System.out.println("displayname: "+user.getDisplayName());
					System.out.println("email: "+user.getEmail());
				}
			}else{
				Log.w(TAG, "Google sign in failed");
				updateUI(null);
			}
		}
		/*
		// Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
		if(requestCode == RC_SIGN_IN){
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			try{
				// Google Sign In was successful, authenticate with Firebase
				GoogleSignInAccount account = task.getResult(ApiException.class);
				firebaseAuthWithGoogle(account);
			}catch(ApiException e){
				// Google Sign In failed, update UI appropriately
				Log.w(TAG, "Google sign in failed", e);
				// [START_EXCLUDE]
				updateUI(null);
				// [END_EXCLUDE]
			}
		}*/
	}
	
	private void firebaseAuthWithGoogle(GoogleSignInAccount acct){
		Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
		// [START_EXCLUDE silent]
		showProgressDialog();
		// [END_EXCLUDE]
		
		AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
		mAuth.signInWithCredential(credential)
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>(){
					@Override
					public void onComplete(@NonNull Task<AuthResult> task){
						if(task.isSuccessful()){
							// Sign in success, update UI with the signed-in user's information
							Log.d(TAG, "signInWithCredential:success");
							FirebaseUser user = mAuth.getCurrentUser();
							updateUI(user);
						}else{
							// If sign in fails, display a message to the user.
							Log.w(TAG, "signInWithCredential:failure", task.getException());
							Toast.makeText(MainActivity.this, "Authentication failed.",
									Toast.LENGTH_SHORT).show();
							updateUI(null);
						}
						
						// [START_EXCLUDE]
						hideProgressDialog();
						// [END_EXCLUDE]
					}
				});
	}
	
	private void signOut(){
		// Firebase sign out
		mAuth.signOut();
		
		// Google sign out
		mGoogleSignInClient.signOut().addOnCompleteListener(this,
				new OnCompleteListener<Void>(){
					@Override
					public void onComplete(@NonNull Task<Void> task){
						updateUI(null);
					}
				});
	}
	
	private void revokeAccess(){
		// Firebase sign out
		mAuth.signOut();
		
		// Google revoke access
		mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
				new OnCompleteListener<Void>(){
					@Override
					public void onComplete(@NonNull Task<Void> task){
						updateUI(null);
					}
				});
	}
	
	private void updateUI(FirebaseUser user){
		hideProgressDialog();
		if(user != null){
			mStatusTextView.setText(getString(R.string.google_status_fmt, user.getEmail()));
			mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));
			
			findViewById(R.id.sign_in_button).setVisibility(View.GONE);
			findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
		}else{
			mStatusTextView.setText("Sign Out");
			mDetailTextView.setText(null);
			
			findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
			findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
		}
	}
}