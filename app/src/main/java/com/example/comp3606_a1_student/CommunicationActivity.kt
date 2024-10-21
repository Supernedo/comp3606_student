package com.example.comp3606_a1_student

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.comp3606_a1_student.chatlist.ChatListAdapter
import com.example.comp3606_a1_student.models.ContentModel
import com.example.comp3606_a1_student.network.Client
import com.example.comp3606_a1_student.network.NetworkMessageInterface
//import com.example.comp3606_a1_student.network.Server
import com.example.comp3606_a1_student.peerlist.PeerListAdapter
import com.example.comp3606_a1_student.peerlist.PeerListAdapterInterface
import com.example.comp3606_a1_student.wifidirect.WifiDirectInterface
import com.example.comp3606_a1_student.wifidirect.WifiDirectManager

class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, PeerListAdapterInterface, NetworkMessageInterface {
    private var wfdManager: WifiDirectManager? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private var peerListAdapter:PeerListAdapter? = null
    private var chatListAdapter:ChatListAdapter? = null

    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var hasDevices = false
    //private var server: Server? = null
    private var client: Client? = null
    private var deviceIp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(this, manager, channel, this)

        peerListAdapter = PeerListAdapter(this)
        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.adapter = peerListAdapter
        rvPeerList.layoutManager = LinearLayoutManager(this)

        chatListAdapter = ChatListAdapter()
        val rvChatList: RecyclerView = findViewById(R.id.rvChat)
        rvChatList.adapter = chatListAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }
    fun createGroup(view: View) {
        wfdManager?.createGroup()
    }

    fun discoverNearbyPeers(view: View) {
        wfdManager?.discoverPeers()
        sendDiscreteMessage(view)
    }

    private fun updateUI(){
        //Turn on adapter
        val wfdAdapterErrorView: ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        //Create group or discover peers
        val wfdNoConnectionView: ConstraintLayout = findViewById(R.id.clNoWifiDirectConnection)
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        //Show eligible peers
        val rvPeerList: RecyclerView = findViewById(R.id.rvPeerListing)
        rvPeerList.visibility = if (wfdAdapterEnabled && !wfdHasConnection && hasDevices) View.VISIBLE else View.GONE

        //Chat interface for established connection
        val wfdConnectedView: ConstraintLayout = findViewById(R.id.clHasConnection)
        wfdConnectedView.visibility = if(wfdHasConnection) View.VISIBLE else View.GONE
    }

    private fun sendDiscreteMessage(view: View) {
        val studentIDText:EditText = findViewById(R.id.studentID)

        if (!isValidID(studentIDText.text.toString()))
        {
            var text = "Invalid Student ID entered!"

            val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
            toast.show()
            studentIDText.text.clear()
        }
        else {
            client?.studentID = studentIDText.text.toString()

            val studentJoinMessage = "I am here"
            val content = ContentModel(studentJoinMessage, deviceIp)

            client?.sendMessage(content)
        }
    }

    fun isValidID(studentID: String): Boolean {
        val idStart = 816
        if (studentID.startsWith(idStart.toString()))
            return true

        return false
    }

    fun sendMessage(view: View) {
        val etMessage:EditText = findViewById(R.id.etMessage)
        val etString = etMessage.text.toString()
        val content = ContentModel(etString, deviceIp)
        etMessage.text.clear()
        client?.sendMessage(content)
        chatListAdapter?.addItemToEnd(content)
    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "Wifi Direct state updated. It is now "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        val toast = Toast.makeText(this, "Refreshed nearby devices!", Toast.LENGTH_SHORT)
        toast.show()
        hasDevices = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        updateUI()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?) {
        val text = if (groupInfo == null){
            "Group was not formed..."
        } else {
            "Group formed!"
        }
        val toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        toast.show()
        wfdHasConnection = groupInfo != null

        if (groupInfo == null){
            //server?.close()
            client?.close()
            //else if (groupInfo.isGroupOwner && server == null)
        } else if (groupInfo.isGroupOwner){
            //server = Server(this)
            deviceIp = "192.168.49.2"
        } else if (!groupInfo.isGroupOwner && client == null) {
            client = Client(this)
            deviceIp = client!!.ip
        }
    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        val toast = Toast.makeText(this, "Wifi Direct status has been updated!" , Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        wfdManager?.connectToPeer(peer)
    }

    override fun onContent(content: ContentModel) {
        runOnUiThread{
            chatListAdapter?.addItemToEnd(content)
        }
    }
}