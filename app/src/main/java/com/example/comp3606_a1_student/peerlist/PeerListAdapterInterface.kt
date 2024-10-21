package com.example.comp3606_a1_student.peerlist

import android.net.wifi.p2p.WifiP2pDevice

interface PeerListAdapterInterface {
    fun onPeerClicked(peer: WifiP2pDevice)
}