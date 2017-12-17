package com.boa.utils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Boa (David Figueroa dgfigueroa29@gmail.com) on 17 dic 2017.
 */
public class TxParam{
	@SerializedName("to")
	@Expose
	private String to;
	@SerializedName("asset")
	@Expose
	private String asset;//carbon
	@SerializedName("amount")
	@Expose
	private String amount;//0.00001
	@SerializedName("payload")
	@Expose
	private String payload;
	@SerializedName("callbackURL")
	@Expose
	private String callbackURL;
	
	public String getTo(){
		return to;
	}
	
	public void setTo(String to){
		this.to = to;
	}
	
	public String getAsset(){
		return asset;
	}
	
	public void setAsset(String asset){
		this.asset = asset;
	}
	
	public String getAmount(){
		return amount;
	}
	
	public void setAmount(String amount){
		this.amount = amount;
	}
	
	public String getPayload(){
		return payload;
	}
	
	public void setPayload(String payload){
		this.payload = payload;
	}
	
	public String getCallbackURL(){
		return callbackURL;
	}
	
	public void setCallbackURL(String callbackURL){
		this.callbackURL = callbackURL;
	}
}