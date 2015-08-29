package com.cn2.wifi;
import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
// This class Handles reading & writing of messages with socket buffers. Uses a Handler to pass the message to handler
public class MessageManager implements Runnable {
    private Socket socket = null;
    private Handler handler;
    public static final int MY_HANDLE = 0x400 + 2;
    public static final int MESSAGE_READ = 0x400 + 1;
    private InputStream iStream;
    private OutputStream oStream;

    public MessageManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
        
    }
    private static final String TAG = "WifiLogChatHandler";
    @Override
    public void run() {
    	Log.d(TAG, "inside run method of MessageManager");
        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(MY_HANDLE, this).sendToTarget();
            while (true) {
            	//if message manager thread is interrupted close the socket
            	if(Thread.currentThread().isInterrupted()){
            		Log.d(TAG, "message manager interrupted");
                	try {
            			socket.close();
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
            		break;
            	}
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                    	Log.d(TAG, "I am heres");
                        break;
                    }
                    // Send the obtained bytes to the handler
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    handler.obtainMessage(MESSAGE_READ,bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            	Log.d(TAG, "finally");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //method to write on message manager 
    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
    //method to close message manager thread
    public void closeSocket(){
    	Log.e(TAG, "closing socket in message Manager");
    		Thread.currentThread().interrupt();
    }

}
