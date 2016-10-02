package me.kudryavka.bluecon.SPP;

/**
 * Created by seyriz on 2016. 10. 3..
 */

public interface SPPListener {
    void onPacketReceived(byte[] packet);

    void onPacketSended(byte[] packet);

    void onBluetoothDeviceConnected(String name, String address);

    void onBluetoothDeviceDisconnected();

    void onBluetoothDisabled();

    void onBluetoothNotSupported();

    void onBluetoothDeviceConnecting();
}
