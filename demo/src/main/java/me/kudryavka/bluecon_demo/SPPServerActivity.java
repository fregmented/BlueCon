package me.kudryavka.bluecon_demo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;
import me.kudryavka.bluecon.SPPServer.SPPServerService;


public class SPPServerActivity extends AppCompatActivity implements View.OnClickListener, SPPListener {
    private final static String TAG = "SPPServerActivity";
    private SPPServerService.SPPBinder svBinder;
    private boolean isSVConned;

    private EditText msg_for_send;
    private Button btn_start_spp_server_single;
    private Button btn_start_spp_server_multi;
    private Button btn_send_to_device;
    private TextView con_device_info;
    private ListView lists;
    private Handler handler;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sppserver);
        bindService(new Intent(this, SPPServerService.class), serviceConnection, BIND_AUTO_CREATE);
        handler = new Handler();

        msg_for_send = (EditText)findViewById(R.id.msg_for_send);
        btn_start_spp_server_single = (Button)findViewById(R.id.btn_start_spp_server_single);
        btn_start_spp_server_multi = (Button)findViewById(R.id.btn_start_spp_server_multi);
        btn_send_to_device = (Button)findViewById(R.id.btn_send_to_device);
        con_device_info = (TextView)findViewById(R.id.con_device_info);
        lists = (ListView)findViewById(R.id.lists);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lists.setAdapter(adapter);

        btn_start_spp_server_single.setOnClickListener(this);
        btn_start_spp_server_multi.setOnClickListener(this);
        btn_send_to_device.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        svBinder.stop();
        unbindService(serviceConnection);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start_spp_server_single:
                svBinder.startServerSingle("SPP_SERVER_TEST_SINGLE");
                con_device_info.setText("SINGLE");
                break;
            case R.id.btn_start_spp_server_multi:
                svBinder.startServerMultiple("SPP_SERVER_TEST_MULTI");
                con_device_info.setText("MULTI");
                break;
            case R.id.btn_send_to_device:
                if(svBinder.getState() == ENUMS.BLUETOOTH_STATES.CONNECTED){
                    String text = msg_for_send.getText().toString();
                    svBinder.broadcast(text.getBytes());
                    msg_for_send.setText("");
                }
                else {
                    Toast.makeText(this, getString(R.string.need_connect_device), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("SERVICE", "CONNNECTED");
            svBinder = (SPPServerService.SPPBinder)service;
            isSVConned = true;
            svBinder.addSPPListener(SPPServerActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("SERVICE", "DISCONNNECTED");
            isSVConned = false;
        }
    };

    @Override
    public void onBluetoothDeviceConnected(final String name, final String address) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPServerActivity.this, name + "/" + address + " Connected", Toast.LENGTH_LONG).show();
                adapter.add("CON: " + address);
                adapter.notifyDataSetChanged();
                String strings = con_device_info.getText().toString();
                strings += "\n"+address;
                con_device_info.setText(strings);
            }
        });
    }

    @Override
    public void onBluetoothDeviceConnecting(String address) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPServerActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onBluetoothDeviceDisconnected(final String address) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPServerActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                adapter.add("DINCON: " + address);
                adapter.notifyDataSetChanged();
                String strings = con_device_info.getText().toString();
                strings = strings.replace("\n"+address, "");
                con_device_info.setText(strings);
            }
        });
    }

    @Override
    public void onBluetoothDisabled() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPServerActivity.this, "Bluetooth Disabled", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onBluetoothNotSupported() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SPPServerActivity.this, "Can't use bluetooth", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onPacketReceived(String address, final byte[] packet) {
        Log.d(TAG, "PACKET RECV("+address+") : " + new String(packet));
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.add("RECV: " + new String(packet));
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onPacketSended(String address, final byte[] packet) {
        Log.d(TAG, "PACKET SEND("+address+") : " + new String(packet));
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.add("SEND: " + new String(packet));
                adapter.notifyDataSetChanged();
            }
        });
    }
}
