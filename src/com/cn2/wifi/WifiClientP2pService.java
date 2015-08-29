package com.cn2.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

// A structure to hold service information.
public class WifiClientP2pService {
	private WifiP2pDevice device;
    private String instanceName = null;
    private String serviceRegistrationType = null;
    private String isCommunicated="NO";
    
    public void setDevice(WifiP2pDevice device){
    	this.device=device;
    	
    }
    public void setInstanceName(String instanceName){
    	this.instanceName=instanceName;
    }
    public void setServiceRegistrationType(String serviceRegistrationType){
    	this.serviceRegistrationType=serviceRegistrationType;
    }
    public void setIsCommunicated(String isCommunicated){
    	this.isCommunicated=isCommunicated;
    }
    public WifiP2pDevice getDevice(){
    	return this.device;
    }
    public String getInstanceName(){
    	return this.instanceName;
    }
    public String getServiceRegistrationType(){
    	return this.serviceRegistrationType;
    }
    public String getIsCommunicated(){
    	return this.isCommunicated;
    }

}
