package com.cn2.wifi;
import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
//This is the Client Socket handler. This is used by the WifiClient. it creates client socket
public class ClientSocketHandler extends Thread {
    private static final String TAG = "WifiLogClientSocketHandler";
    private Handler handler;
    private MessageManager msgMgr;
    private InetAddress mAddress;
    private Socket socket;
    static final int SERVER_PORT = 4545;
    public Thread msgThread;
    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
        this.setName("Client_Skt_Hndlr");
    }
    @Override
    public void run() {
        socket = new Socket();
    	if(Thread.currentThread().isInterrupted()){
    		Log.d(TAG, "clientsockethandler interrupted");
    		return;
    	}
        try {
            socket.bind(null);
            //connect to server socket            
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),SERVER_PORT));
            Log.d(TAG, "Launching the I/O handler");
            // create message manager
            msgMgr = new MessageManager(socket, handler);
            //create message thread
            msgThread = new Thread(msgMgr);
            msgThread.setName("Client_Msg_Thread");
            msgThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }
    //method to close client thread
    public void closeSocket(){
    	try {
    		//close message thread
    		Log.d(TAG, "inside closesocket of socket handler");
    		msgThread.interrupt();
    		//close socket
    		socket.close();    		
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
