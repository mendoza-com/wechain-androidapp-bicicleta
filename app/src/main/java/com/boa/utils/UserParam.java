package com.boa.utils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Boa (David Figueroa dgfigueroa29@gmail.com) on 26 nov 2017.
 */
public class UserParam{
	@SerializedName("email")
	@Expose
	private String email;
	
	public String getEmail(){
		return email;
	}
	
	public void setEmail(String email){
		this.email = email;
	}
}