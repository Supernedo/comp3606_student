package com.example.comp3606_a1_student.wifidirect

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup

interface WifiDirectInterface {
    fun onWiFiDirectStateChanged(isEnabled:Boolean)
    fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>)
    fun onGroupStatusChanged(groupInfo: WifiP2pGroup?)
    fun onDeviceStatusChanged(thisDevice: WifiP2pDevice)
}