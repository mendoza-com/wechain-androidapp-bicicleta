package com.boa.wechain;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 8 dic 2017.
 */
public class Exercise extends RealmObject{
	@PrimaryKey
	private long	id;
	private double	initLat, initLon, endLat, endLon, distance;
	private int		status;
	private String	email;
	
	@Ignore
	public static final int STATUS_PENDING = 0;
	@Ignore
	public static final int STATUS_SENDED = 1;
	
	public long getId(){
		return id;
	}
	
	public void setId(long id){
		this.id = id;
	}
	
	public double getInitLat(){
		return initLat;
	}
	
	public void setInitLat(double initLat){
		this.initLat = initLat;
	}
	
	public double getInitLon(){
		return initLon;
	}
	
	public void setInitLon(double initLon){
		this.initLon = initLon;
	}
	
	public double getEndLat(){
		return endLat;
	}
	
	public void setEndLat(double endLat){
		this.endLat = endLat;
	}
	
	public double getEndLon(){
		return endLon;
	}
	
	public void setEndLon(double endLon){
		this.endLon = endLon;
	}
	
	public double getDistance(){
		return distance;
	}
	
	public void setDistance(double distance){
		this.distance = distance;
	}
	
	public int getStatus(){
		return status;
	}
	
	public void setStatus(int status){
		this.status = status;
	}
	
	public String getEmail(){
		return email;
	}
	
	public void setEmail(String email){
		this.email = email;
	}
}