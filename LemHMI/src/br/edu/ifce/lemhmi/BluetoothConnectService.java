package br.edu.ifce.lemhmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BluetoothConnectService {

//	private static final UUID MY_UUID = new UUID(0x0000110100001000L, 0x800000805F9B34FBL);
//	private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
//	private static final UUID MY_UUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb");
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");



	public static final int EXIT_CMD = -1;

	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device

	private static final String NAME = "LemHMI";

	private final Handler mHandler;
	private BluetoothAdapter mBluetoothAdapter;
//	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	public BluetoothConnectService(Handler handler) {
		mHandler = handler;
		mState = STATE_NONE;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	public synchronized void start() {
		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

//		if (mAcceptThread == null) {
//			mAcceptThread = new AcceptThread();
//			mAcceptThread.start();
//		}
		setState(STATE_LISTEN);
	}

	private synchronized void setState(int state) {
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	public synchronized int getState() {
		return mState;
	}

	public synchronized void connect(BluetoothDevice device) {
		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
//		if (mAcceptThread != null) {
//			mAcceptThread.cancel();
//			mAcceptThread = null;
//		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
		write("Yay!".getBytes());
	}

	public synchronized void stop() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// if (mAcceptThread != null) {
		// mAcceptThread.cancel();
		// mAcceptThread = null;
		// }

		setState(STATE_NONE);
	}

	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	public void write(int out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		setState(STATE_LISTEN);
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

//	private class AcceptThread extends Thread {
//		// The local server socket
//		private final BluetoothServerSocket mmServerSocket;
//
//		public AcceptThread() {
//			super("AcceptThread");
//			BluetoothServerSocket tmp = null;
//
//			// Create a new listening server socket
//			try {
//				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			mmServerSocket = tmp;
//		}
//
//		public void run() {
//			BluetoothSocket socket = null;
//
//			// Listen to the server socket if we're not connected
//			while (mState != STATE_CONNECTED) {
//				try {
//					// This is a blocking call and will only return on a
//					// successful connection or an exception
//					socket = mmServerSocket.accept();
//				} catch (IOException e) {
//					e.printStackTrace();
//					break;
//				}
//
//				// If a connection was accepted
//				if (socket != null) {
//					synchronized (BluetoothConnectService.this) {
//						switch (mState) {
//						case STATE_LISTEN:
//						case STATE_CONNECTING:
//							// Situation normal. Start the connected thread.
//							connected(socket, socket.getRemoteDevice());
//							break;
//						case STATE_NONE:
//						case STATE_CONNECTED:
//							// Either not ready or already connected. Terminate
//							// new socket.
//							try {
//								socket.close();
//							} catch (IOException e) {
//								e.printStackTrace();
//							}
//							break;
//						}
//					}
//				}
//			}
//		}
//
//		public void cancel() {
//			try {
//				mmServerSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			super("ConnectThread");
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				tmp = (BluetoothSocket) m.invoke(device, 1);
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
//				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				connectionFailed();
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) {
					closeException.printStackTrace();
				}
				BluetoothConnectService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothConnectService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			super("ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			System.out.println("Yay!");
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// Send the obtained bytes to the UI activity
					mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					connectionLost();
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void write(int out) {
			try {
				mmOutStream.write(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmOutStream.write(EXIT_CMD);
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}