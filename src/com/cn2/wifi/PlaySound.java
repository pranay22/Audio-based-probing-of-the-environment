package com.cn2.wifi;

import java.io.IOException;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
//This class is responsible for playing sound
public class PlaySound {
	private static final String LOG_TAG = "WifiPlayer";
	private MediaPlayer   mPlayer = null;
	private Context context;
	private static Uri uri;
	
	PlaySound(Context context){
		this.context=context;
		uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.gaussian_noise);		//get the file to play from raw folder
		mPlayer = new MediaPlayer();																		//create media player instance
	}
	public void play(boolean start) {																		//method for start/stop player
        if (start) {            startPlayer();        } 
        else {		            stopPlayer();         }
    }
	public boolean isPlaying() {																			//method to check if player playing
		boolean state=false;
		try{          state= mPlayer.isPlaying();
		}catch(Exception e){			 e.printStackTrace();		}
		return state;
    }
	private void startPlayer() {																			//method to start player
        try {
            mPlayer.setDataSource(context,uri);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {      	e.printStackTrace();        }
    }
    private void stopPlayer() {																				//method to stop player
        try {
        	mPlayer.stop();
        	mPlayer.release();
        	mPlayer = null;
        } catch (Exception e) {			e.printStackTrace();        }
    }
}
