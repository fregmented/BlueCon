package me.kudryavka.bluecon.Listeners;

import android.support.annotation.Nullable;

/**
 * Created by seyriz on 2016. 10. 3..
 */

public interface SPPListener {
    void onPacketReceived(String address, byte[] packet);

    void onPacketSended(String address, byte[] packet);

    void onBluetoothDeviceConnected(String name, String address);

    void onBluetoothDeviceConnecting(@Nullable String name);

    void onBluetoothDeviceDisconnected(@Nullable String name);

    void onBluetoothDisabled();

    void onBluetoothNotSupported();
}
