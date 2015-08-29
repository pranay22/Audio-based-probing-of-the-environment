package com.cn2.wifi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import android.provider.Settings.Secure;
//This class is responsible for registering a service other remote client can bind to, receiving client message and start recorder 
public class WifiServer extends Service implements ConnectionInfoListener{	

	public static final String TXTRECORD_SERVER_ANDROIDID 	= "TXTRECORD_SERVER_ANDROIDID";	//server android_id key published by server
	public static final String TXTRECORD_PROP_AVAILABLE 	= "available";					//text published by server service
    public static final String SERVER_SERVICE_INSTANCE      = "_wifi_ID_A1_8_Server";		//server service
    public static final String CLIENT_SERVICE_INSTANCE      = "_wifi_ID_A1_8_Client";		//client service
    public static final String SERVICE_REG_TYPE 			= "_presence._tcp";				//service type
	private WifiP2pManager manager;
	private Channel channel;
	private WifiP2pDnsSdServiceRequest serviceRequest;
	//variable used for client-server message handler
    static final String MSG_REQUEST_RECORDSOUND = "MSG_REQUEST_RECORDSOUND";
    static final String MSG_CONFIRM_RECORDSOUND ="MSG_CONFIRM_RECORDSOUND";
    static final String MSG_REPLY_POSITIVE   = "MSG_REPLY_POSITIVE";
    static final String MSG_REPLY_NEGATIVE    ="MSG_REPLY_NEGATIVE";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;

	private PlayRecorderHandler playrecorderHandler;				//Handler to handle local message to play recorder
	private Looper mServiceLooper;									//service looper for play recorder handler
	private HandlerThread localMsgHandlerThread=null;									//thread for play recorder handler
	private PlayRecorder playrecorder;								//instance of play recorder class
	private BroadcastReceiver receiver = null;
	private final IntentFilter intentFilter = new IntentFilter();	//intent filter
    private Handler incomingMsgHandler = new IncomingMsgHandler();				//handler to handle client-server messaging
    private Thread socketHandler=null;								//server socket handler thread
    public MessageManager chatManager = null;
	private boolean RequestAccepted=false;
	private boolean recorderStopped = false;
	private int startId;
    String serverAndroid_id;
    
	public static final String TAG = "WifiLogServer";
	
    public Handler getHandler() {
	        return incomingMsgHandler;
    }
 	public void onCreate() {
		//set intent filters
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        //create instance of wifi pep manager and channel
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        //get the server android id
        serverAndroid_id = Secure.getString(WifiServer.this.getContentResolver(),Secure.ANDROID_ID);
    }
	public int onStartCommand(Intent intent, int flags, final int startId) {
       	Log.d(TAG, "registering service");
       	 RegisterBroadcastReceiver();					//register Broadcast receiver
         RegisterServerService();						//register service and discovering service
       	 discoverRemoteService();
       	//starting local message handler responsible for playing recorder
       	this.startId=startId;
       	for (Thread tRef : Thread.getAllStackTraces().keySet()){ 
			if (tRef.getName().equals("Thrd_Lcl_Msg_HndlrS")){
				localMsgHandlerThread = (HandlerThread) tRef;
			}
		}
		if(localMsgHandlerThread==null){
			localMsgHandlerThread = new HandlerThread("Thrd_Lcl_Msg_HndlrS",Process.THREAD_PRIORITY_BACKGROUND);
			localMsgHandlerThread.start();
		}
       	
 		mServiceLooper = localMsgHandlerThread.getLooper();		// Get the HandlerThread's Looper and use it for our Handler
 		playrecorderHandler = new PlayRecorderHandler(mServiceLooper);
 		
 		createServerSocket();						//create server socket 
 		return START_STICKY;						// If we get killed, after returning from here, restart
    }
	public IBinder onBind(Intent intent) {		return null;	}
	public void onDestroy() {
		Log.d(TAG, "inside onDestroy()");
		localMsgHandlerThread.interrupt();
		closeServerSocket();
		//clear local service registered for clients
		Log.d(TAG,"Wifiserver is Thrd_Lcl_Msg_Hndlr alive? "+localMsgHandlerThread.isAlive());
		if(localMsgHandlerThread!= null){
			 Thread moribund = localMsgHandlerThread;
			 localMsgHandlerThread = null;
			 moribund.interrupt();
		}
		unRegisterServerService();
		unregisterReceiver(receiver);
	}
	//method to register broadcast receiver
	private void RegisterBroadcastReceiver(){
       	Log.d(TAG, "Registering BroadcustReceiver");
        receiver = new WiFiBroadcastReceiver(manager, channel, this);
        Log.d(TAG,"registering BroadcastReceiver in onStartCommand()");
        registerReceiver(receiver, intentFilter);
	}
	//method to register server service
    private void RegisterServerService() {
    	//Registers a local service for clients to be able to search remote service 
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        record.put(TXTRECORD_SERVER_ANDROIDID, serverAndroid_id);		//include server android id with the published service 
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(SERVER_SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {
            public void onSuccess() {         	Log.d(TAG,"Added Local Service");            }
            public void onFailure(int error) {  Log.d(TAG,"Failed to add a service");        }
        });
    }
    //method to unregister server service
    private void unRegisterServerService(){
    	Log.d(TAG,"unRegistering ServerService");
		//clear local service registered for clients
		manager.clearLocalServices(channel, new ActionListener() {
            public void onSuccess() {         	Log.d(TAG,"Cleared Local Service");            }
            public void onFailure(int error) {  Log.d(TAG,"Failed to clear a service");        }
        });
    }
    //method to discover remote service
    public void discoverRemoteService() {
    	manager.setDnsSdResponseListeners(channel,
    			new DnsSdServiceResponseListener() {
    			@Override
    			public void onDnsSdServiceAvailable(String instanceName, String registrationType,WifiP2pDevice srcDevice) {
    				// A service has been discovered. check if this is the service we are searching for
    		        if (instanceName.equalsIgnoreCase(CLIENT_SERVICE_INSTANCE)) {
    		                Log.d(TAG, "ServiceAvailable " + instanceName);
    		                Log.d(TAG,"deviceAddress: "+srcDevice.deviceAddress);
    		                Log.d(TAG,"deviceName: "+srcDevice.deviceName);
    		        }
    			}
    		    }, new DnsSdTxtRecordListener() {
                    @Override
                	public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                        Log.d(TAG, "DnsSdTxtRecord available -"+device.deviceName + record.toString());
                            
                    }
    		    });   
    	//create a service request
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,new ActionListener() {
                    public void onSuccess() {        	Log.d(TAG,"added service discovery request");       		}
                    public void onFailure(int arg0) {   Log.d(TAG,"Failed adding service discovery request");    }
        });
        
        //initiate discovery
        manager.discoverServices(channel, new ActionListener() {
            public void onSuccess() {                	Log.d(TAG,"Service discovery initiated");				}
            public void onFailure(int arg0) {        	Log.d(TAG,"Service discovery failed");               	}
        });
    }
    //when connection is established
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
		//createServerSocket();
	}
	//method to create server socket
	public void createServerSocket(){
        Log.d(TAG, "inside createServerSocket");
        try{
        	//handler = new GroupOwnerSocketHandler(((MessageTarget) this).getHandler());
        	socketHandler = new ServerSocketHandler(this.getHandler());
        	socketHandler.start();
        } catch (IOException e) {
            Log.d(TAG,"Failed to create a server thread - " + e.getMessage());
        }
	}
	//method to close server socket
	private void closeServerSocket(){
		Log.d(TAG,"inside close ServerSocket");
		Log.d(TAG,"before is server socket handler Alive?: "+socketHandler.isAlive());
		if(socketHandler.isAlive()){
			((ServerSocketHandler)socketHandler).closeSocket();
			socketHandler.interrupt(); 
		}
		Log.d(TAG,"afer is server socket handler Alive?: "+socketHandler.isAlive());

	}
    /**This class is the communicate message with other device running client
     *  	If this service gets MSG_REQUEST_RECORDSOUND request to record sound  
     *  		send MSG_REPLY_POSITIVE message if no other +ve reply is sent for which communication is still active (RequestAccepted flag is not set) 
     *  		send MSG_REPLY_NEGATIVE if RequestAccepted flag is set to stop simultaneously recording multiple sound
     *  	If in reply MSG_CONFIRM_RECORDSOUND is received start recorder 
     */
	private final class IncomingMsgHandler extends Handler {
    public void handleMessage(Message msg) {
		Log.d(TAG, "handlemessage:");
        switch (msg.what) {
        case MESSAGE_READ:
            byte[] readBuf = (byte[]) msg.obj;
            // construct a string from the valid bytes in the buffer
            String readMessage = new String(readBuf, 0, msg.arg1);
            Log.d(TAG, "Got message: "+readMessage);
        	String[] reply = readMessage.split(":");
            switch (reply[1]) {
            case MSG_REQUEST_RECORDSOUND:
            	Log.d(TAG, "Client requested recordsound: "+RequestAccepted);
            	if(RequestAccepted==false){
            		RequestAccepted=true;
                    try {
                    	//reply +ve to client with own device id
                    	chatManager.write((serverAndroid_id+":"+MSG_REPLY_POSITIVE).getBytes());
                    	Log.d(TAG, "Server replied +ve to: "+reply[0]);
                    	Toast.makeText(getApplicationContext(), "Server replied +ve to: "+reply[0], Toast.LENGTH_SHORT).show();
                    } 
                    catch (Exception e) {	e.printStackTrace();                        }
            	}
            	else{
                    try {
                 		String serverAndroid_id = Secure.getString(WifiServer.this.getContentResolver(),Secure.ANDROID_ID);
                    	chatManager.write((serverAndroid_id+":"+MSG_REPLY_NEGATIVE).getBytes());
                    	Log.d(TAG, "Server replied -ve to: "+reply[0]);
                    	Toast.makeText(getApplicationContext(), "Server replied -ve to: "+reply[0], Toast.LENGTH_SHORT).show();
                    } 
                    catch (Exception e) {	e.printStackTrace();                        }
            	}
                break;
            case MSG_CONFIRM_RECORDSOUND:
            	Log.d(TAG, "Client confirmed recordsound: "+RequestAccepted);
            	if(RequestAccepted=true){
            		//get the client ID and send it to recorder handler as well as own device id using a bundle
            		Toast.makeText(getApplicationContext(), "Starting recorder for: "+reply[0], Toast.LENGTH_SHORT).show();
            		Message playmsg = playrecorderHandler.obtainMessage();
            		playmsg.arg1 = startId;
            		Log.d(TAG, "########### Server has startID: "+playmsg.arg1);
            		Bundle recorderInfo = new Bundle();
            		recorderInfo.putString("serverID", serverAndroid_id);
            		recorderInfo.putString("clientID", reply[0]);
            		playmsg.setData(recorderInfo);
            		playrecorderHandler.sendMessage(playmsg); 
            	}
            	break;
            default:
            	Log.d(TAG, "Server received something else: "+ reply[1]);
                super.handleMessage(msg);
            }//switch (reply[1]) END
            break;
        case MY_HANDLE:
            Object obj = msg.obj;
            chatManager = (MessageManager)obj;
            Log.d(TAG, "My message: "+msg.arg1+":"+msg.obj);
        }//switch (msg.what) END
        return;
	}}
	//This class is a local message handler used to run recorder, it gets message from the WifiServer service to record sound	
 	private final class PlayRecorderHandler extends Handler {	//Handler to receive message from thread to start playing
 		public PlayRecorderHandler(Looper looper) {	          super(looper);	      }
 		public void handleMessage(Message msg) {
 			Log.d(TAG, "starting recorder,"+msg.getData().getString("serverID")+","+msg.getData().getString("clientID"));
 			//Initiate recorder
 			playrecorder = new PlayRecorder(msg.getData().getString("serverID"),msg.getData().getString("clientID"),5*1000);
 			//start recording
 			playrecorder.record(true);
 			//wait 6 second let recording finish
 			long endTime = System.currentTimeMillis() + 6*1000;
 			while (System.currentTimeMillis() < endTime) {;;			}
 			playrecorder.record(false);
 			RequestAccepted = false;		//re set the flag so that other request can be accepted 
 			Log.d(TAG, "after recorder stopped: ");
 			Log.d(TAG,"is server socket handler Alive?: "+socketHandler.isAlive());
 			Toast.makeText(getApplicationContext(), "Finished recording ", Toast.LENGTH_SHORT).show();
 			Log.d(TAG,"closing server...");
 			 			
 			Log.d(TAG, "@@@@@@@@ Server has startID: "+msg.arg1);
 			stopSelf(msg.arg1);
 		}
 	}
	
}
