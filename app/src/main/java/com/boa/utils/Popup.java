package com.boa.utils;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import com.boa.wechain.MainActivity;
import com.boa.wechain.R;
import com.boa.wechain.WechainApp;
import com.squareup.picasso.Picasso;
import java.util.List;

/**
 * Created by Boa (David Figueroa dgfigueroa29@gmail.com) on 10/02/2018.
 */
public class Popup extends Dialog implements android.view.View.OnClickListener{
	private String user, email;
	private Uri image;
	private MainActivity activity;
	
	public Popup(@NonNull Context context){
		super(context);
	}
	
	public Popup(@NonNull Context context, int themeResId){
		super(context, themeResId);
	}
	
	protected Popup(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener){
		super(context, cancelable, cancelListener);
	}
	
	public Popup(MainActivity activity, String user, String email, Uri image){
		super(activity);
		this.activity = activity;
		this.user = user;
		this.email = email;
		this.image = image;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		try{
			super.onCreate(savedInstanceState);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.popup);
			TextView tvHello = findViewById(R.id.tvHello);
			TextView tvEmail = findViewById(R.id.tvEmail);
			ImageView ivUser = findViewById(R.id.ivUser);
			Picasso.with(WechainApp.getContext()).load(image).placeholder(R.drawable.ic_shortcut_person).into(ivUser);
			tvHello.setText("¡HOLA "+user+"!");
			tvEmail.setText(email);
			findViewById(R.id.btnLogout).setOnClickListener(this);
			findViewById(R.id.btnWeb).setOnClickListener(this);
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "Popup:onCreate - ", e);
		}
	}
	
	@Override
	public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data, @Nullable Menu menu, int deviceId){
	}
	
	@Override
	public void onPointerCaptureChanged(boolean hasCapture){
	}
	
	@Override
	public void onClick(View v){
		try{
			switch(v.getId()){
				case R.id.btnLogout:
					activity.signOut();
				break;
				
				case R.id.btnWeb:
					activity.go2Wechain();
				break;
			}
			
			dismiss();
		}catch(Exception e){
			Utils.logError(WechainApp.getContext(), "Popup:onClick - ", e);
		}
	}
}