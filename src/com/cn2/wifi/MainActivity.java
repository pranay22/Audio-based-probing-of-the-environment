package com.cn2.wifi;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
//This is the main activity
public class MainActivity extends ActionBarActivity {
	public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
	private boolean serverActive=false; 
	private boolean clientActive=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //method to start client service, if client and server service not running then start client
    public void sendMessageStartWifiClient(View view) {
    	if(clientActive==true){
    		Toast.makeText(this, "Client already Active", Toast.LENGTH_SHORT).show();
    	}
    	else if(serverActive==true){
    		Toast.makeText(this, "Server Active,stop Server to start Client", Toast.LENGTH_SHORT).show();
    	}
    	else{
    	    Intent intent = new Intent(this, WifiClient.class);
    		startService(intent);
    		clientActive=true;
    		Toast.makeText(this, "Client Started", Toast.LENGTH_SHORT).show();
    	}
  	}
  //method to stop client service
    public void sendMessageStopWifiClient(View view) {
    	if(clientActive==true){
    		Intent intent = new Intent(this, WifiClient.class);
    		stopService(intent);
    		clientActive=false;
    		Toast.makeText(this, "Client Stopped", Toast.LENGTH_SHORT).show();
    	}
    	else{
    		Toast.makeText(this, "Client not Active", Toast.LENGTH_SHORT).show();
    	}
    }
  //method to start server service, if client and server service not running then start server
    public void sendMessageStartWifiServer(View view) {
    	if(serverActive==true){
    		Toast.makeText(this, "Server already Active", Toast.LENGTH_SHORT).show();
    	}
    	else if(clientActive==true){
    		Toast.makeText(this, "Client Active,stop Client to start Server", Toast.LENGTH_SHORT).show();
    	}
    	else{
    		Intent intent = new Intent(this, WifiServer.class);
    		startService(intent);
    		serverActive=true;
    		Toast.makeText(this, "Server Started", Toast.LENGTH_SHORT).show();
    	}
  	}
  //method to stop server service
    public void sendMessageStopWifiServer(View view) {
    	if(serverActive==true){
    		Intent intent = new Intent(this, WifiServer.class);
    		stopService(intent);
    		serverActive=false;
    		Toast.makeText(this, "Server Stopped", Toast.LENGTH_SHORT).show();
    	}
    	else{
    		Toast.makeText(this, "Server not Active", Toast.LENGTH_SHORT).show();
    	}

    }
 
}
