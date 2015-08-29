package com.cn2.wifi;
import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
//This is the ServerSocket handler. This is used by the WifiServer. it creates server socket and socket pool for client request
public class ServerSocketHandler extends Thread {
    private ServerSocket socket = null;
    private final int THREAD_COUNT = 10;
    static final int SERVER_PORT = 4545;
    private MessageManager msgMgr=null;
    private Handler handler;
    private static final String TAG = "WifiLogServerSocketHandler";
    
    public ServerSocketHandler(Handler handler) throws IOException {
        try {
        	//create server socket
            socket = new ServerSocket(SERVER_PORT);
            this.handler = handler;
            this.setName("Server_Skt_Hndlr");
            Log.d(TAG, "Server Socket Started");
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }
    }
    //Method to close server socket
    public void closeSocket(){
    	try {
    		pool.shutdownNow();
			socket.close();
    		Log.d(TAG, "after closing socket inside serversoket "+socket.isClosed()+","+pool.isShutdown());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    // A ThreadPool for client sockets.
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    @Override
    public void run() {
        while (true) {
            try {
                //Initiate a MessageManager instance to handle the messages from client
            	if(Thread.currentThread().isInterrupted()){
            		Log.d(TAG, "server socket handler interrupted");
                	try {
            			socket.close();
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
            		break;
            	}

            	msgMgr= new MessageManager(socket.accept(), handler);
                pool.execute(msgMgr);
                
                Log.d(TAG, "Launching the I/O handler");
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) {
                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }
}
