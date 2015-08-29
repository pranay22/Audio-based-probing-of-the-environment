package com.cn2.wifi;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
//This is the class responsible for recording 
public class PlayRecorder {
	private static final String TAG = "WifiRecorder";
	private static String mFileName = null;
	private MediaRecorder mRecorder = null;
	private boolean isRecording=true;
	private int maxduration=0;
	
	PlayRecorder(String serverID,String clientID,int maxduration){
		this.maxduration=maxduration;
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();	//set the file path
        mFileName += "/"+serverID+"_"+clientID+".flac";								//set the file name
        mRecorder = new MediaRecorder();											//create recorder instance
	}
	public void record(boolean start) {												//method used to start/stop recorder
        if (start) {    isRecording = true;   startRecorder();        } 
        else {        	stopRecorder();        						  }
    }
	public boolean isRecording() {return isRecording;		}				//method used to check if recorder recording	
	private void startRecorder() {
        try {																		//method for starting recorder
        	//Log.d(TAG,"inside startRecording()");
        	mRecorder.setOutputFile(mFileName);
        	mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setMaxDuration(maxduration);
            mRecorder.setAudioSamplingRate(44100);
            
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {       Log.d(TAG,""+e);e.printStackTrace();       }
    }
    private void stopRecorder() {													//method for stopping recorder
    	//Log.d(TAG,"inside stopRecording()");
        try {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            isRecording = false;
        } catch (Exception e) {       e.printStackTrace();			}
    }
}
