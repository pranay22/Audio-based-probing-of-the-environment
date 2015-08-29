package com.cn2.wifi;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
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
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

//This class is responsible for running client part of the application which searches & sends message to remote service  and plays sound 
public class WifiClient extends Service implements ConnectionInfoListener{	
	 
	public static final String TXTRECORD_SERVER_ANDROIDID = "TXTRECORD_SERVER_ANDROIDID";	//server android_id key published by server
	public static final String TXTRECORD_PROP_AVAILABLE   = "available";					//text published by server service 
    public static final String SERVER_SERVICE_INSTANCE    = "_wifi_ID_A1_8_Server";			//server service
    public static final String CLIENT_SERVICE_INSTANCE    = "_wifi_ID_A1_8_Client";			//client service
    public static final String SERVICE_REG_TYPE           = "_presence._tcp";				//service type
    //variable used for client-server message handler
    public static final String MSG_REQUEST_RECORDSOUND 	  = "MSG_REQUEST_RECORDSOUND";		
    public static final String MSG_CONFIRM_RECORDSOUND    = "MSG_CONFIRM_RECORDSOUND";
    public static final String MSG_REPLY_POSITIVE         = "MSG_REPLY_POSITIVE";
    public static final String MSG_REPLY_NEGATIVE         = "MSG_REPLY_NEGATIVE";
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
	 
	private WifiP2pDnsSdServiceRequest serviceRequest;				//instance variables for remote service listener
    private PlaySoundHandler playSoundHandler;						//Handler to handle local message to play sound 
    private Looper mServiceLooper;									//service looper for playsound handler
    private HandlerThread localMsgHandlerThread=null;									//thread for playsound handler
    PlaySound playsound;											//instance of play sound class
	private final IntentFilter intentFilter = new IntentFilter();	//intent filter
    private Handler incomingMsgHandler = new IncomingMsgHandler();				//handler to handle client-server messaging
    public Thread ConnectorThread=null;
	public MessageManager msgManager=null;    
	private BroadcastReceiver receiver = null;	
	private WifiP2pManager manager;
	private Channel channel;
	private Thread socketHandler=null;			//client socket handler thread

    private boolean ReplyAccepted          = false;
    private boolean playerStopped = false;
    private int startId;
    public boolean sendRequest=false;
    private String deviceBeingConnected;
    String clientAndroid_id;
    private Connector connector; 
    //Map that holds mapping of <peer IP address,p2p service object>
    private Map<String,WifiClientP2pService> deviceList = new HashMap<String,WifiClientP2pService>();
    //Map that holds mapping of <peer IP address,peer device android id>
    private Map<String,String> deviceNameList = new HashMap<String,String>();
    
    private static final String TAG = "WifiLogClient";
    boolean p2pconnected=false;
    private Handler getHandler() {
        return incomingMsgHandler;
    }
    //method to add to <peer IP address,p2p service object> Map -> MAP1
 	public void addDevice(String deviceAddress, WifiClientP2pService p2pservice){
 		deviceList.put(deviceAddress,p2pservice);
 		Log.d(TAG, "added device in Device list");
 	}
 	//method to add to <peer IP address,peer device android id> -> MAP2
 	public void addDeviceName(String deviceAddress, String deviceAndroidName){
 		deviceNameList.put(deviceAddress,deviceAndroidName);
 		Log.d(TAG, "added name in Device name list");
 	}
 	public Map<String,WifiClientP2pService> getDeviceList(){
 		return deviceList;
 	}
 	public Map<String,String> getDeviceNameList(){
 		return deviceNameList;
 	}

	public void onCreate() {
		//intent filters
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);		//get instance of service manager
        channel = manager.initialize(this, getMainLooper(), null);					//get instance of channel
        clientAndroid_id = Secure.getString(WifiClient.this.getContentResolver(),Secure.ANDROID_ID);	//get client android id
    }
	public int onStartCommand(Intent intent, int flags, final int startId) {
		//Log.d(TAG,"WifiClient service started");
		RegisterBroadcastReceiver();				//register Broadcast receiver
       	RegisterClientService();					//start registration and discovering service
       	launchServiceListner();
       	discoverRemoteService();
        //create thread and looper to start local message handler for PlaySound
		this.startId = startId;
		for (Thread tRef : Thread.getAllStackTraces().keySet()){ 
			if (tRef.getName().equals("Thrd_Lcl_Msg_HndlrC")){
				localMsgHandlerThread = (HandlerThread) tRef;
			}
		}
		if(localMsgHandlerThread==null){
			localMsgHandlerThread = new HandlerThread("Thrd_Lcl_Msg_HndlrC",Process.THREAD_PRIORITY_BACKGROUND);
			localMsgHandlerThread.start();
		}
	 	mServiceLooper = localMsgHandlerThread.getLooper();		// Get the HandlerThread's Looper and use it for our Handler
	 	
	 	connector = new Connector();
       	ConnectorThread = new Thread(connector);
       	ConnectorThread.setName("Connector_Thread");
       	ConnectorThread.start();

		return START_STICKY;						// If we get killed, after returning from here, restart
    }
	public IBinder onBind(Intent intent) {  		        return null;	}
	
	public void onDestroy() {
		ConnectorThread.interrupt();
		Log.d(TAG,"Wifiserver is Connector_Thread alive? "+ConnectorThread.isAlive());
		if(ConnectorThread!= null){
			 Thread moribund = ConnectorThread;
			 ConnectorThread = null;
			 moribund.interrupt();
		}
				
		localMsgHandlerThread.interrupt();
		Log.d(TAG,"Wifiserver is Thrd_Lcl_Msg_Hndlr alive? "+localMsgHandlerThread.isAlive());
		if(localMsgHandlerThread!= null){
			 Thread moribund = localMsgHandlerThread;
			 localMsgHandlerThread = null;
			 moribund.interrupt();
		}
		unRegisterClientService();
		disconnectP2p();
        unregisterReceiver(receiver);
        //Log.d(TAG,"WifiClient service stopped");
	}
	//@@@@@@@@@@@@@@@@@@@@@@@@@@ method to register broadcast receiver
	private void RegisterBroadcastReceiver(){
       	Log.d(TAG, "Registering BroadcustReceiver");
        receiver = new WiFiBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
	}
	//@@@@@@@@@@@@@@@@@@@@@@@@@@ method to register client service
    private void RegisterClientService() {
       	Log.d(TAG, "Registering ClientService");
    	//Registers a local service for clients to be able to search remote service 
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(CLIENT_SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {
            public void onSuccess() {         	Log.d(TAG,"Added Local Service");            }
            public void onFailure(int error) {  Log.d(TAG,"Failed to add a service");        }
        });
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to un-register client service
    private void unRegisterClientService(){
    	Log.d(TAG,"unRegister ClientService");
		//clear local service registered for clients
		manager.clearLocalServices(channel, new ActionListener() {
            public void onSuccess() {         	/*Log.d(TAG,"Cleared Local Service");*/            }
            public void onFailure(int error) {  /*Log.d(TAG,"Failed to clear a service");*/        }
        });
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to launch service listener
    private void launchServiceListner(){
    	Log.d(TAG, "Launching ServiceListner");
    	manager.setDnsSdResponseListeners(channel,
    			new DnsSdServiceResponseListener() {
    			@Override
    			public void onDnsSdServiceAvailable(String instanceName, String registrationType,WifiP2pDevice srcDevice) {
    				Log.d(TAG, "ServiceAvailable " + instanceName);
    				// A service has been discovered. check if this is the service we are searching for
    		        if (instanceName.equalsIgnoreCase(SERVER_SERVICE_INSTANCE)) {
    		        		//store the data in a service instance
    		        		WifiClientP2pService p2pservice = new WifiClientP2pService();	//class to store service object
    		                p2pservice.setDevice(srcDevice);
    		                p2pservice.setInstanceName(instanceName);
    		                p2pservice.setServiceRegistrationType(registrationType);
    		                Log.d("WifisrvListner", "WifiService: "+ p2pservice.getInstanceName()+" from: "+p2pservice.getDevice());
    		                Log.d("WifisrvListner","deviceAddress: "+p2pservice.getDevice().deviceAddress);
    		                Log.d("WifisrvListner","deviceName: "+p2pservice.getDevice().deviceName);
    		                /***auto device list***/
    		                Log.d("WifisrvListner","adding device in list ");
    		                addDevice(p2pservice.getDevice().deviceAddress, p2pservice);		//add device to the MAP1
    		                /***auto device list***/
    		                //connectP2p();								//connect to the device running server service
    		        }
    			}
    		    }, new DnsSdTxtRecordListener() {
                    @Override
                	public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                        Log.d("WifiTxtListner", "DnsSdTxtRecord available -"+device.deviceAddress);
                        Log.d("WifiTxtListner", "DnsSdTxtRecord String -"+record.get(TXTRECORD_SERVER_ANDROIDID));    
    	                /***auto device list***/
                        if(record.get(TXTRECORD_SERVER_ANDROIDID) != null){
                        		addDeviceName(device.deviceAddress, record.get(TXTRECORD_SERVER_ANDROIDID)); //add device to the MAP2
                        }
    	                /***auto device list***/

                    }
    		    });   
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to initiates a service discovery
    public void discoverRemoteService() {
    	Log.d(TAG, "Discovering RemoteService");
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
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to connect to the server device 
    public synchronized void connectP2p(WifiClientP2pService peer) {
    	Log.d(TAG,"inside connectp2p ");
    	/***auto device list***/
    	/***auto device list***/
    			Log.d(TAG,"device address: "+peer.getDevice().deviceAddress);
    			WifiP2pConfig config = new WifiP2pConfig();
    			config.deviceAddress = peer.getDevice().deviceAddress;
    			config.wps.setup = WpsInfo.PBC;
    	    	//Toast.makeText(getApplicationContext(), "Trying to connect with "+config.deviceAddress, Toast.LENGTH_SHORT).show();
    			if (serviceRequest != null)
    				manager.removeServiceRequest(channel, serviceRequest,new ActionListener() {
    					public void onSuccess() {           }
    					public void onFailure(int arg0) {   }
    				});
    				manager.connect(channel, config, new ActionListener() {
    					public void onSuccess() {        	Log.d(TAG,"Connecting to device");            }
    					public void onFailure(int errorCode) {    	Log.d(TAG,"failed Connecting to device");         }
    			});
    	/***auto device list***/		
    	/***auto device list***/
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to disconnect from the p2p device running server
    private void disconnectP2p(){
    	Log.d(TAG,"inside disconnectP2p ");
		if (manager != null && channel != null) {
            manager.removeGroup(channel, new ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }
                @Override
                public void onSuccess() {
                	Log.d(TAG, "Disconnect sucessful.");
                }
            });
        }
		p2pconnected=false;
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ call create client socket when the p2p connection is established
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
    	p2pconnected=true;
    	Log.d(TAG, "onConnectionInfoAvailable client Connected as peer with: "+p2pInfo.groupOwnerAddress);
    	createClientSockcet(p2pInfo.groupOwnerAddress);
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to create and start client socket
    private void createClientSockcet(InetAddress groupOwnerAddress){
        Log.d(TAG, "createClientSockcet trying to create sockethandler ");
        socketHandler = new ClientSocketHandler(this.getHandler(),groupOwnerAddress);
        socketHandler.start();
        Log.d(TAG, "createClientSockcet sockethandler created");
    }
    //@@@@@@@@@@@@@@@@@@@@@@@@@@ method to close client socket
    private void closeClientSocket(){
    	Log.d(TAG,"inside close ClientSocket");
		
			((ClientSocketHandler)socketHandler).closeSocket();
			socketHandler.interrupt();
		
		Log.d(TAG,"@$@#$IS clientSocket messageHandler Thread alive?: "+((ClientSocketHandler)socketHandler).msgThread.isAlive());
		
    }
	/**This class is the communicate message with other device running server
	 * All the messages sent and received are in <unique android_id : message> format for server and client
	 * When client socket handler starts(MY_HANDLE) send message MSG_REQUEST_RECORDSOUND to client 
	 * when server replies
	 *	If reply is +ve with MSG_REPLY_POSITIVE 
	 *		If there no other +ve reply from other server is accepted which is under process (i.e. ReplyAccepted flag not set)
	 *				send message to local handler to start playing sound and set ReplyAccepted flag 
	 *				reply back the confirmation to server with MSG_CONFIRM_RECORDSOUND
	 *		If ReplyAccepted flag is set any other +ve reply from server is under process(avoid playing multiple sound simultaneously)
	 *		 		Ignore the +ve reply and set the connect status of the device picked from MAP1 back from ONGOING to NO
	 *	If reply is -ve with MSG_REPLY_NEGATIVE
	 *		set the connect status of the device picked from MAP1 back from ONGOING to NO 		
	/***auto device list***/
	
	private final class IncomingMsgHandler extends Handler  {	
		public void handleMessage(Message msg) {
			Log.d(TAG, "handlemessage:");
	        switch (msg.what) {
	        case MESSAGE_READ:
	            byte[] readBuf = (byte[]) msg.obj;
	            // construct a string from the valid bytes in the buffer
	            String readMessage = new String(readBuf, 0, msg.arg1);
	            Log.d(TAG, "Got message: "+readMessage);
	            if(sendRequest==true){
	            	String[] reply = readMessage.split(":");
	            	/***auto device list***/
	    	 		String deviceID=null;
	    	 		String serverAndroidId = reply[0];	//get the android id of the server 
	    	 		//get the corresponding device address of android id of the device from MAP2
	    	 		Log.d(TAG, "Client has sever android name "+serverAndroidId);
	    	 		Log.d(TAG, "Client has deviceNameList size "+deviceNameList.size());
	    	    	for(Map.Entry<String, String> entryID : deviceNameList.entrySet()){
	    	    		Log.d(TAG, "Client has sever android name entry "+entryID.getValue());
	    	    		if(entryID.getValue().equals(serverAndroidId)){
	    	    			deviceID = entryID.getKey();
	    	    			Log.d(TAG, "Client has sever android address "+deviceID);
	    	    		}
	    	    	}
	    	    	/***auto device list***/
	    	    	switch (reply[1]) {
	  	       		case MSG_REPLY_POSITIVE:
	  	       			Log.d(TAG, "client received reply +ve from: "+reply[0]);
	  	       			if(ReplyAccepted==false){
	  	       				Log.d(TAG, "client considering +ve from: "+reply[0]);
	  	       				ReplyAccepted=true;
	  	       				//send confirmation back
	  	       				msgManager.write((clientAndroid_id+":"+MSG_CONFIRM_RECORDSOUND).getBytes());
	  	       				//ask to play sound
	  	       				playSoundHandler = new PlaySoundHandler(mServiceLooper);
	  	       				Message playmsg = playSoundHandler.obtainMessage();
	  	       				playmsg.arg1 = startId;
	                 		Bundle playerInfo = new Bundle();
		                 	playerInfo.putString("serverID", reply[0]);
	                 		playmsg.setData(playerInfo);
	  	       				playSoundHandler.sendMessage(playmsg);				//send message to local message handler to start player 
	  	       			Toast.makeText(getApplicationContext(), "Playing sound for "+reply[0]+","+deviceID, Toast.LENGTH_SHORT).show();
	  	       			}
	  	       			else{
	  	       			Log.d(TAG, "client considering -ve from: "+"androidID "+reply[0]+",deviceID"+deviceID);
	  	       			/***auto device list***/
	  	       				 //set connect status no in MAP1 corresponding to the device address in MAP2
		  	       			 for(Map.Entry<String, WifiClientP2pService> entry : deviceList.entrySet()){
		  	       				if(!deviceID.isEmpty()){
		  	       					if(deviceID.equals(entry.getKey())){
		  	       						entry.getValue().setIsCommunicated("NO");
		  	       						Log.d(TAG,"updated: " +entry.getKey()+ " as no");
		  	       					}
		  	       				}
		  	       			} 
	  	       			/***auto device list***/
	  	       			}
	  	       			break;
	  	       		case MSG_REPLY_NEGATIVE:
	  	       		Log.d(TAG, "client received reply -ve from: "+"androidID "+reply[0]+",deviceID"+deviceID);
	  	       			/***auto device list***/
	  	       			  //set connect status no in MAP1 corresponding to the device address in MAP2
	  	       			  for(Map.Entry<String, WifiClientP2pService> entry : deviceList.entrySet()){
	  	       				if(!deviceID.isEmpty()){
	  	       					if(deviceID.equals(entry.getKey())){
	  	       						entry.getValue().setIsCommunicated("NO");
	  	       						Log.d(TAG,"updated: " +entry.getKey()+ " as no");
	  	       					}
	  	       				}
	  	       			}
	  	       			/***auto device list***/  
	  	       			break;
	  	       		default:
	  	       			Log.d(TAG, "Client received something else from server: "+reply[1]);
	  	       			super.handleMessage(msg);
	    	   	    }
	            }	
	    	    break;
	        case MY_HANDLE:
	            Object obj = msg.obj;
	            msgManager = (MessageManager)obj;
	            Log.d(TAG, "My message: "+msg.arg1+":"+msg.obj);
	        	//send request message 
	        	if(sendRequest==false){
	        		msgManager.write((clientAndroid_id+":"+MSG_REQUEST_RECORDSOUND).getBytes());
	        		sendRequest=true;
	        	}
	        }
	        return;
		}//handleMessage END
	}//IncomingMsgHandler END
//This class is a local message handler used to run sound player, it gets message from the WifiCLient service to play sound
	private final class PlaySoundHandler extends Handler {	//Handler to receive message from thread to start playing
		public PlaySoundHandler(Looper looper) {	          super(looper);	      }
	 	public void handleMessage(Message msg) {			//get message from service to play sound
	 		Log.d(TAG, "client starting playing sound");
	 		boolean onGoingFoundFlag=false;
	 		boolean noFoundFlag=false;
	 		playsound = new PlaySound(WifiClient.this);
	 		playsound.play(true);
 			//wait 5.5 second let recording finish
 			long endTime = System.currentTimeMillis() + 5500;
 			while (System.currentTimeMillis() < endTime) {;;			}
	 		while(true){									//wait while player is playing sound
	 			if(!playsound.isPlaying() | playerStopped==true){playsound.play(false);break;}
	 		}
	 		playerStopped = true;		
	 		ReplyAccepted=false;		//reset ReplyAccepted flag so that other +ve reply from server can be processed
	 		sendRequest=false;
	 		Log.d(TAG, "after player stopped"+deviceNameList.size()+","+deviceNameList.size());
	 		
	 		/***auto device list***/
	 		String deviceID=null;
	 		String serverAndroidId = msg.getData().getString("serverID");
	 		//get the corresponding device address of android id of the server device from MAP2
	    	for(Map.Entry<String, String> entryID : deviceNameList.entrySet()){
	    		if(entryID.getValue().equals(serverAndroidId)){
	    			deviceID = entryID.getKey();
	    			Log.d(TAG, "deviceID:"+deviceID+",serverAndroidId: "+serverAndroidId);
	    		}
	    	}
	    	//set the connect status of the device picked from MAP1 back from ONGOING to YES
	    	for(Map.Entry<String, WifiClientP2pService> entry : deviceList.entrySet()){
	    		if(!deviceID.isEmpty()){
	    			if(deviceID.equals(entry.getKey())){
	    				entry.getValue().setIsCommunicated("YES");
	    				Log.d(TAG, "deviceID: "+deviceID+",IsCommunicated: "+entry.getValue().getIsCommunicated());
	    				
	    			}
	    		}
	    	}
	    	Toast.makeText(getApplicationContext(), "Communication ended sucessfully with "+serverAndroidId+","+deviceID, 
	    			Toast.LENGTH_SHORT).show();
	 		
	 	}
	} 
    private class Connector implements Runnable {
		@Override
		public void run() {
			boolean connectedFlag=false;
			int tryingConnectCounter=0;
			WifiClientP2pService p2pservice=null;
			while(true){
		    	if(Thread.currentThread().isInterrupted()){
		    		Log.d(TAG, "Connector interrupted");
		    		return;
		    		//break;
		    	}
		    	//Take each device from MAP1 and issue a connect if the connect status is NO and change the status to ONGOING
		    	Log.d(TAG, "before for loop: "+deviceList.size());
		    	for(Map.Entry<String, WifiClientP2pService> entry : deviceList.entrySet()){
		    		p2pservice = entry.getValue();
		    		if(p2pservice.getIsCommunicated().equals("NO")){
		    			p2pservice.setIsCommunicated("ONGOING");
		    			Log.d(TAG,"updated: " +p2pservice.getDevice().deviceName+ " as "+p2pservice.getIsCommunicated());
		    			connectP2p(p2pservice);
		    			//createClientSockcet(null);
		    			connectedFlag=true;
		    			break;
			    	}
		    	}
	 			if(connectedFlag==true){
	 				while(true){
	 					Log.d(TAG,"Waiting 4 Sec");
	 					//wait 3 second let recording finish
	 					long endTime1 = System.currentTimeMillis() + 4000;
	 					while (System.currentTimeMillis() < endTime1) {;;			}
	 					if(!p2pservice.getIsCommunicated().equals("ONGOING")){	
	 						//close client socket connection and p2p connection
	 						Log.d(TAG,"updated: " +p2pservice.getDevice().deviceName+ " as "+p2pservice.getIsCommunicated());
	 						connectedFlag=false;
	 						Log.d(TAG,"restarting client...");
	 				 		//close client socket connection and p2p connection
	 						if(p2pconnected==true){
	 							closeClientSocket();
	 						}
	 						
	 						disconnectP2p();
	 				        unregisterReceiver(receiver);	 				 		
	 				 		RegisterBroadcastReceiver();	 				 		
	 				 		launchServiceListner();
	 				 		discoverRemoteService();

	 						break;
	 					}
	 					
	 				}
	 			}
	 			else{
 					Log.d(TAG,"Waiting 2 Sec");
		 			//wait 1 second let recording finish
		 			long endTime = System.currentTimeMillis() + 2000;
		 			while (System.currentTimeMillis() < endTime) {;;			}
	 			}
			}
    	
		}
    }
}
