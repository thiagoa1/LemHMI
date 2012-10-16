package br.edu.ifce.lemhmi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Message types sent from the BluetoothConnectService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothConnectService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private String mConnectedDeviceName = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothConnectService mConnectService = null;
	
	private TextView stateTextView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		stateTextView = (TextView) findViewById(R.id.stateTextView);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "O dispositivo não suporta acesso bluetooth", Toast.LENGTH_LONG).show();
		}
	}

	// OnClick event
	public void connectBluetooth(View view) {
		// If BT is not on, request that it be enabled.
		// setupCommand() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (mConnectService == null) {
				setupCommand();
			}
		}

		// Ensure this device is discoverable by others
		ensureDiscoverable();

		// Launch the DeviceListActivity to see devices and do scan
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	private void setupCommand() {
		// Initialize the BluetoothChatService to perform bluetooth connections
		mConnectService = new BluetoothConnectService(mHandler);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mConnectService != null) {
			mConnectService.stop();
		}
	}

	private void ensureDiscoverable() {
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				// Attempt to connect to the device
				mConnectService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupCommand();
			} else {
				// User did not enable Bluetooth or an error occured
				Toast.makeText(this, "Não foi possível conectar ao dispositivo bluetooth", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	// The Handler that gets information back from the BluetoothChatService
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothConnectService.STATE_CONNECTED:
					stateTextView.setText("Conectado");
					Toast.makeText(MainActivity.this, "Bluetooth conectado", Toast.LENGTH_SHORT).show();
					break;
				case BluetoothConnectService.STATE_CONNECTING:
					stateTextView.setText("Conectando");
					Toast.makeText(MainActivity.this, "Conectando bluetooth", Toast.LENGTH_SHORT).show();
					break;
				case BluetoothConnectService.STATE_LISTEN:
					stateTextView.setText("Aguardando");
					Toast.makeText(MainActivity.this, "Aguardando bluetooth", Toast.LENGTH_SHORT).show();
					break;
				case BluetoothConnectService.STATE_NONE:
					stateTextView.setText("Desconectado");
					Toast.makeText(MainActivity.this, "Bluetooth desconectado", Toast.LENGTH_SHORT).show();
					break;
				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Conectado a " + mConnectedDeviceName, Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
}