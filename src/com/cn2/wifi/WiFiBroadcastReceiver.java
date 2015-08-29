package com.cn2.wifi;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.util.Log;
//This class is the broadcast receiver for client and server service
public class WiFiBroadcastReceiver extends BroadcastReceiver {
    
	private WifiP2pManager manager;    
	private Channel channel;    
	private WifiClient service1=null; 
	private WifiServer service2=null;
    public static final String TAG="WifiLogBRDCStRcvr";
    
    public WiFiBroadcastReceiver(WifiP2pManager manager, Channel channel,	WifiClient service) {
        super();        this.manager = manager;        this.channel = channel;        this.service1 = service;
    }
    public WiFiBroadcastReceiver(WifiP2pManager manager, Channel channel,	WifiServer service) {
        super();        this.manager = manager;        this.channel = channel;        this.service2 = service;
    }
    @Override
    public void onReceive(Context context, Intent intent) {       
        String action = intent.getAction();
         if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	Log.d(TAG, "inside WIFI_P2P_STATE_CHANGED_ACTION in onReceive()");
             int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            } else {
            } 
         } 
         else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
         }
         //If the connection is established/ disconnected with other peer
         else  if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
        	Log.d(TAG, "inside WIFI_P2P_CONNECTION_CHANGED_ACTION");
             if (manager == null) {                return;            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d(TAG,"getState():"+networkInfo.getState().toString()+",getTypeName(): "+networkInfo.getTypeName());
            if (networkInfo.isConnected()) {
                // if we are connected with the other device, request connection info
            	Log.d(TAG, " requesting requestConnectionInfo()");
            	if(this.service1!=null){
            		manager.requestConnectionInfo(channel, (ConnectionInfoListener) service1);
            	}
            	//if(this.service2!=null){
            	//	manager.requestConnectionInfo(channel, (ConnectionInfoListener) service2);
            	//}
            } else {
            	
            } 
         } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        	Log.d(TAG, "Device status -" +getDeviceStatus(device.status));
        }
    }
    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:             return "Available";
            case WifiP2pDevice.INVITED:               return "Invited";
            case WifiP2pDevice.CONNECTED:             return "Connected";
            case WifiP2pDevice.FAILED:                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:           return "Unavailable";
            default:				                  return "Unknown";
        }
    }

}
