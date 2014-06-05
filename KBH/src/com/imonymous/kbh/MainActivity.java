package com.imonymous.kbh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.echonest.api.v4.EchoNestException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;




@SuppressLint("NewApi")
public class MainActivity extends Activity {

	private int mState;	

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	
	// Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    
    // Enabling Bluetooth intent
	private static final int REQUEST_ENABLE_BT = 3;
	private static final int DISCOVERABLE_TIME = 300;
	
    private AcceptThread mSecureAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	
	BluetoothDevice device;
	
	Thread t;
	
	private BluetoothAdapter btAdapter = null;

	EditText mEdit;
	ToggleButton b1;
	ToggleButton b2;
	ToggleButton b3;
	ToggleButton b4;
	ToggleButton b5;
	ToggleButton b6;
	ToggleButton b7;
	ToggleButton b8;
    
	List<Long> beats;
	
	boolean vibrating = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		
		mState = STATE_NONE;
		
		mEdit   = (EditText)findViewById(R.id.editText1);
		b1 = (ToggleButton) findViewById(R.id.toggleButton1);
		b2 = (ToggleButton) findViewById(R.id.toggleButton2);
		b3 = (ToggleButton) findViewById(R.id.toggleButton3);
		b4 = (ToggleButton) findViewById(R.id.toggleButton4);
		b5 = (ToggleButton) findViewById(R.id.toggleButton5);
		b6 = (ToggleButton) findViewById(R.id.toggleButton6);
		b7 = (ToggleButton) findViewById(R.id.toggleButton7);
		b8 = (ToggleButton) findViewById(R.id.toggleButton8);
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
		    // Device does not support Bluetooth
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy); 
	}
	
	@Override
    public void onStart() {
        super.onStart();
        
        // If BT is not on, request that it be enabled.
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else
        {
        	setUp();
        }
        String log = "OnStart";    	
    	Log.d("Function", log);
    	
    }
	
	protected void onResume() {
        super.onResume();
        
        String log = "OnResume";    	
    	Log.d("Function", log);
	}
	
	@Override
    public synchronized void onPause() {
        super.onPause();
        String log = "OnPause";    	
    	Log.d("Function", log);
    }

    @Override
    public void onStop() {
        super.onStop();
        String log = "OnStop";    	
    	Log.d("Function", log);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();        
        
        stop();
    
        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }        
        String log = "OnDestroy";    	
    	Log.d("Function", log);
    }
    
    // Setting up before the BT transactions begin. Make sure UI is initialized and the device is discoverable
    void setUp()
	{		
        // Both open a socket for listening, until one of them sends a connect initiating the BT transaction
        startListening();
        
        Toast.makeText(this, "Ran SetUp", Toast.LENGTH_LONG).show();
	}
	
	
	// Ensure all time discoverability
	void ensureDiscoverable() {
        if (btAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_TIME);
            startActivity(discoverableIntent);
        }
	}
	
	/**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            write(send);
        }
    }
    
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
    	 // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
		
	TextView.OnEditorActionListener mWriteListener =
	        new TextView.OnEditorActionListener(){
	        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
	            // If the action is a key-up event on the return key, send the message
	            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
	                String message = view.getText().toString();
	                sendMessage(message);
	            }
	            return true;
	        }
	    };
	   
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        
        // Attempt to connect to the device
        connectToDevice(device);
    }
	    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	//super.onActivityResult(requestCode, resultCode, data);
    	
        switch (requestCode) {
    
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setUp();
                Toast.makeText(this, R.string.bt_enabled, Toast.LENGTH_LONG).show();
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }
    
    //Morebeat 1
    public void vibrateTap1(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
			sendMessage("1");
	    	v.vibrate(30);
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
    
    //Morebeat 2
    public void vibrateTap2(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
			sendMessage("2");
			long[] pattern = {0, 31, 63, 31, 63, 31, 31, 125, 250, 125, 125, 125};
  	    	v.vibrate(pattern, -1);
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
    
    //Morebeat 3
    public void vibrateTap3(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
			sendMessage("3");
			long[] pattern = {31, 31, 125, 31, 63, 63, 31, 125, 63, 62, 125, 125, 62, 63};
  	    	v.vibrate(pattern, -1);
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
    
    //Morebeat 4
    public void vibrateTap4(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
			sendMessage("4");
			long[] pattern = {0, 250, 250, 250, 250, 125, 125, 125, 125, 250, 250};
  	    	v.vibrate(pattern, -1);
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}

    public void vibrateSong(View view)
	{
		TrackAnalyser ta = new TrackAnalyser();
			
		try {
			if(mEdit.getText().toString() != null)
			{
				beats = ta.analyse(mEdit.getText().toString());
				if(beats.size() != 0 )
				{
					playSong();
				}
			}
		} catch (EchoNestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
		
	}
    
    public void playSong()
	{ 
		t = new Thread(new Runnable()
		{
			public void run()
			{
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			   	long startTime = System.currentTimeMillis();
				long nextTime = (long) ( startTime + beats.get(0));
				boolean vibrated = false;
				
				vibrating = true;
				
				Iterator<Long> it = beats.iterator();
				
				if(v.hasVibrator())
				{
					while(it.hasNext())
					{
						vibrated = false;
						while( System.currentTimeMillis() < nextTime)
						{
							if ( !vibrated && vibrating )
							{
								v.vibrate(30);
								vibrated = true;
							}
						}
						nextTime = startTime + it.next();
					}
				}
			}
		});
		t.start();
	}

    //Rattlesnake
	public void vibrate1(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
			int tON = 750;
			int tOFF = 250;
			
	    	long[] pattern = {0, tON, tOFF};
	    	
	    	if(b1.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//Rain
	public void vibrate2(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
			Random random = new Random(System.currentTimeMillis());
			
	    	long[] pattern = {0, random.nextInt(200), random.nextInt(200), random.nextInt(200), random.nextInt(200), random.nextInt(200), random.nextInt(200), random.nextInt(200), random.nextInt(200), random.nextInt(200)};
	    	if(b2.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//Timebomb
	public void vibrate3(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
	    	long[] pattern = {125, 125};
	    	if(b3.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//OffbeatWaltz
	public void vibrate4(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
	    	long[] pattern = {250, 125, 125, 125, 125};
	    	if(b4.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//Beat1
	public void vibrate5(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
	    	long[] pattern = { 62, 63, 125, 62, 63, 125, 62, 250, 63, 125 };
	    	if(b5.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//Beat2
	public void vibrate6(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
	    	long[] pattern = {0, 31, 63, 31, 63, 31, 31, 125, 250, 125, 125, 125};
	    	if(b6.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//Beat3
	public void vibrate7(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
	    	long[] pattern = {31, 31, 125, 31, 63, 63, 31, 125, 63, 62, 125, 125, 62, 63};
	    	if(b7.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	//Beat4
	public void vibrate8(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	 
		if(v.hasVibrator())
		{
	    	long[] pattern = {0, 250, 250, 250, 250, 125, 125, 125, 125, 250, 250};
	    	if(b8.isChecked())
	    	{
	    		v.vibrate(pattern, 0);
	    	}
	    	else
	    	{
	    		v.vibrate(pattern, -1);
	    	}
		}
		else
		{
			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void stopVibrate(View view)
	{
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	   	v.cancel();	
	   	
	   	vibrating = false;
	   	
	   	if (t != null)
	   	{
	   		
	   		t.interrupt();
	   		t = null;
	   	}
	}
	
	public void btnExit(View view) {
		
		Toast.makeText(this, "Bye!", Toast.LENGTH_LONG).show();
		
		if (mSecureAcceptThread != null)
		{
			mSecureAcceptThread = null;
		}
		finish();   	   	    	
    }


    // Start Threads
	 /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void startListening() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);
        
        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
        }
    }    
        
    // Listen for Incoming
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
            
        public AcceptThread() {

        	BluetoothServerSocket tmp = null;
        	
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("Iman", MY_UUID_SECURE);
                
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }
     
        public void run() {

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();

                } catch (IOException e) {
                    Log.e("TAG", "Socket accept() failed", e);
                    break;
                };
                
                // If a connection was accepted
                if (socket != null) {
                    synchronized (this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e("TAG", "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }

        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Socket close() of server failed", e);
            }
        }
    }

    
    // Connect to a device
	
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connectToDevice(BluetoothDevice device) {
    	   	
        // Cancel any thread attempting to make a connection
    	if (mState == STATE_CONNECTING) {
    		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
    	}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setState(STATE_CONNECTING);
        Toast.makeText(this, "Came to ConnectToDevice", Toast.LENGTH_LONG).show();
    }

	 /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
            	
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                }
            catch (IOException e) {
                
            }
            mmSocket = tmp;
        }

        public void run() {


            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
                
            } catch (IOException e) {
                // Close the socket
            	try {
            		if(mmSocket != null)
            		{
            			mmSocket.close();
            		}
				} catch (IOException e1) {
					// Auto-generated catch block
					e1.printStackTrace();
				}
            }
         
            // Reset the ConnectThread because we're done            
            mConnectThread = null;

            if(mmSocket != null)
    		{
            	// Start the connected thread
            	connected(mmSocket, mmDevice);
    		}
            return;
        }            

        public void cancel() {
            try {
            	if(mmSocket != null)
        		{
            		mmSocket.close();
        		}
            } catch (IOException e) {
                
            }
        }
    }

	

    // Manage the connection once connected

	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        
    	// Cancel any attempts to accept connection
    	if (mSecureAcceptThread != null) {mSecureAcceptThread.cancel(); mSecureAcceptThread = null;}
    	
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        if(getState() != STATE_CONNECTED)
        {
        	// Cancel any thread currently running a connection
        	if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        
	        // Start the thread to manage the connection and perform transmissions
	        mConnectedThread = new ConnectedThread(socket);
	        mConnectedThread.start();
	
	        setState(STATE_CONNECTED);

        }
    }
	
    
    
	/**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
           
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
	        
            byte[] buffer = new byte[1024];
            int bytes = 0;
            
            
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
	                
                    if(bytes > 0)
                    {
	                    int i = byteArrayToInt(buffer);
	                    
	                    switch(i)
	                    {
	                    case 1:
	                    	vibrate1();
	                    	break;
	                    case 2:
	                    	vibrate2();
	                    	break;
	                    case 3:
	                    	vibrate3();
	                    	break;
	                    case 4:
	                    	vibrate4();
	                    	break;
	                    default:
	                    	break;
	                    }
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
               
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
    
    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            try {
				mConnectedThread.mmSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        
        setState(STATE_NONE);        
        
    }
   
    /**
     * Set the current connection state. */
    private synchronized void setState(int state) {       
        mState = state;
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    public static int byteArrayToInt(byte[] b) 
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

 	
  	//MoreBeat1
  	public void vibrate1()
  	{
  		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  	   	 
  		if(v.hasVibrator())
  		{
  	    	v.vibrate(30);
  		}
  		else
  		{
  			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
  		}
  	}
  	
  	//MoreBeat2
  	public void vibrate2()
  	{
  		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  	   	 
  		if(v.hasVibrator())
  		{
  	    	long[] pattern = {0, 31, 63, 31, 63, 31, 31, 125, 250, 125, 125, 125};
  	    	v.vibrate(pattern, -1);
  	    	
  		}
  		else
  		{
  			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
  		}
  	}
  	
  	//MoreBeat3
  	public void vibrate3()
  	{
  		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  	   	 
  		if(v.hasVibrator())
  		{
  	    	long[] pattern = {31, 31, 125, 31, 63, 63, 31, 125, 63, 62, 125, 125, 62, 63};
  	    	v.vibrate(pattern, -1);
  		}
  		else
  		{
  			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
  		}
  	}
  	
  	//MoreBeat4
  	public void vibrate4()
  	{
  		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  	   	 
  		if(v.hasVibrator())
  		{
  	    	long[] pattern = {0, 250, 250, 250, 250, 125, 125, 125, 125, 250, 250};
  	    	v.vibrate(pattern, -1);
  		}
  		else
  		{
  			Toast.makeText(this, R.string.no_vibrator, Toast.LENGTH_SHORT).show();
  		}
  	}
}