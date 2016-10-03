package me.kudryavka.bluecon.SPPClient;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

import java.util.ArrayList;

import me.kudryavka.bluecon.Consts.ENUMS;
import me.kudryavka.bluecon.Listeners.SPPListener;

public class SPPClientService extends Service {

    private static final String TAG = "SPPClientService";

    private SPPClientController sppClientController;

    public SPPClientService() {
    }

    @Override
    public void onCreate() {
        sppClientController = SPPClientController.getInstance(getApplicationContext());
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new SPPBinder();
    }

    public class SPPBinder extends android.os.Binder{

        public void connect(BluetoothDevice device){
            sppClientController.connect(device);
        }

        public void connect(String address){
            sppClientController.connect(address);
        }

        public void connect(BluetoothDevice device, int bufferSize){
            sppClientController.connect(device, bufferSize);
        }

        public void connect(String address, int bufferSize){
            sppClientController.connect(address, bufferSize);
        }

        public void disconnect(){
            sppClientController.stop();
        }

        public void sendPacket(byte[] data){
            sppClientController.sendPacket(data);
        }

        public ENUMS.BLUETOOTH_STATES getState(){
            return sppClientController.getBluetooth_states();
        }

        public void addSPPListener(SPPListener sppListener){
            sppClientController.addSPPListener(sppListener);
        }

        public void removeSPPLiestener(SPPListener sppListener){
            sppClientController.removeSPPLiestener(sppListener);
        }

        public ArrayList<BluetoothDevice> getPairedDevices(){
            return sppClientController.getPairedDevices();
        }
    }
}
