package me.kudryavka.bluecon_demo;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.SPP.SPPListener;
import me.kudryavka.bluecon.SPP.SPPService;

public class SPPActivity extends AppCompatActivity implements View.OnClickListener, SPPListener {

    private final static String TAG = "SPPActivity";

    private SPPService.SPPBinder svBinder;
    private boolean isSVConned;

    private EditText msg_for_send;
    private Button btn_get_paired_devices;
    private Button btn_connect_to_device;
    private Button btn_send_to_device;
    private TextView con_device_info;
    private ListView lists;
    private Handler handler;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spp);

        msg_for_send = (EditText)findViewById(R.id.msg_for_send);
        btn_get_paired_devices = (Button)findViewById(R.id.btn_get_paired_devices);
        btn_connect_to_device = (Button)findViewById(R.id.btn_connect_to_device);
        btn_send_to_device = (Button)findViewById(R.id.btn_send_to_device);
        con_device_info = (TextView)findViewById(R.id.con_device_info);
        lists = (ListView)findViewById(R.id.lists);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lists.setAdapter(adapter);

        btn_get_paired_devices.setOnClickListener(this);
        btn_connect_to_device.setOnClickListener(this);
        btn_send_to_device.setOnClickListener(this);
        bindService(new Intent(this, SPPService.class), serviceConnection, BIND_AUTO_CREATE);

        handler = new Handler();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void onClick(View view) {
        if(isSVConned) {
            switch (view.getId()) {
                case R.id.btn_get_paired_devices:
                    final ArrayList<BluetoothDevice> devices = svBinder.getPairedDevices();
                    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
                    for(BluetoothDevice device : devices){
                        adapter.add(device.getName() + "/" + device.getAddress());
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.select_device))
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String selected = adapter.getItem(i);
                                    String address = selected.split("/")[1];
                                    for(BluetoothDevice device : devices){
                                        if(device.getAddress().equals(address)){
                                            svBinder.init(device);
                                            con_device_info.setText(selected);
                                            break;
                                        }
                                    }
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();
                    break;
                case R.id.btn_connect_to_device:
                    if(svBinder.isInited()){
                        svBinder.connect();
                    }
                    else {
                        Toast.makeText(this, getString(R.string.need_select_device), Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.btn_send_to_device:
                    if(svBinder.getState() == ENUMS.BLUETOOTH_STATES.CONNECTED){
                        String text = msg_for_send.getText().toString();
                        svBinder.sendPacket(text.getBytes());
                        msg_for_send.setText("");
                    }
                    else {
                        Toast.makeText(this, getString(R.string.need_connect_device), Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
        else {
            Toast.makeText(this, getString(R.string.need_conn_to_service), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBluetoothDeviceConnected(final String name, final String address) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPActivity.this, name + "/" + address + " Connected", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBluetoothDeviceConnecting() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onBluetoothDeviceDisconnected() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onBluetoothDisabled() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPActivity.this, "Bluetooth Disabled", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onBluetoothNotSupported() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPActivity.this, "Can't use bluetooth", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onPacketReceived(final byte[] packet) {
        Log.d(TAG, "PACKET RECV : " + new String(packet));
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.add("RECV: " + new String(packet));
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onPacketSended(final byte[] packet) {
        Log.d(TAG, "PACKET SEND : " + new String(packet));
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.add("SEND: " + new String(packet));
                adapter.notifyDataSetChanged();
            }
        });
    }

    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("SERVICE", "CONNNECTED");
            svBinder = (SPPService.SPPBinder)service;
            isSVConned = true;
            svBinder.addSPPListener(SPPActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("SERVICE", "DISCONNNECTED");
            isSVConned = false;
        }
    };
}
