package com.alitronics.app

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.p2p.*
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val CHAT_PORT = 8988
        private const val FILE_PORT = 8990
        private const val PICK_FILE = 30
    }

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var peersList: ListView
    private lateinit var status: TextView
    private lateinit var chat: TextView
    private lateinit var message: EditText

    private val peers = mutableListOf<WifiP2pDevice>()
    private val executor = Executors.newCachedThreadPool()
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    @Volatile private var peerIp: String? = null
    @Volatile private var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        chat = findViewById(R.id.chat)
        message = findViewById(R.id.message)
        val discoverButton: Button = findViewById(R.id.discoverButton)
        val sendButton: Button = findViewById(R.id.sendButton)
        val fileButton: Button = findViewById(R.id.fileButton)
        val voiceButton: Button = findViewById(R.id.voiceButton)
        val videoButton: Button = findViewById(R.id.videoButton)
        peersList = findViewById(R.id.peersList)

        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> requestConnectionInfo()
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) ==
                                WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        status.text = if (enabled) "Wi-Fi Direct • Ready" else "Turn on Wi-Fi"
                    }
                }
            }
        }

        discoverButton.setOnClickListener { discover() }
        sendButton.setOnClickListener { sendMessage() }
        fileButton.setOnClickListener {
            if (peerIp == null) {
                toast("Connect to a device first")
            } else {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, PICK_FILE)
            }
        }
        voiceButton.setOnClickListener {
            toast("Voice media is reserved for the tested V3 media build.")
        }
        videoButton.setOnClickListener {
            toast("Video media is reserved for the tested V3 media build.")
        }
        peersList.setOnItemClickListener { _, _, position, _ -> connect(peers[position]) }

        requestPermissionsIfNeeded()
        startChatServer()
        startFileServer()
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            needed += Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (needed.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 10)
        }
    }

    private fun hasWifiPermission(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 33)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        else
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()
        val f = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        registerReceiver(receiver, f, RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        unregisterReceiver(receiver)
        super.onPause()
    }

    override fun onDestroy() {
        running = false
        try { socket?.close() } catch (_: Exception) {}
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun discover() {
        if (!hasWifiPermission()) {
            requestPermissionsIfNeeded()
            return
        }
        status.text = "Searching…"
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { }
            override fun onFailure(reason: Int) {
                status.text = "Discovery failed: $reason"
            }
        })
    }

    private fun requestPeers() {
        if (!hasWifiPermission()) return
        manager.requestPeers(channel) { list ->
            peers.clear()
            peers.addAll(list.deviceList)
            peersList.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                peers.map { "${it.deviceName} • ${it.deviceAddress}" }
            )
            if (peers.isNotEmpty()) status.text = "Select the other device"
        }
    }

    private fun connect(device: WifiP2pDevice) {
        if (!hasWifiPermission()) return
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        status.text = "Connecting…"
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {
                status.text = "Connection failed: $reason"
            }
        })
    }

    private fun requestConnectionInfo() {
        if (!hasWifiPermission()) return
        manager.requestConnectionInfo(channel) { info ->
            if (!info.groupFormed) return@requestConnectionInfo
            status.text = "Wi-Fi Direct connected • Offline"
            if (!info.isGroupOwner) {
                info.groupOwnerAddress?.hostAddress?.let { connectChat(it) }
            }
        }
    }

    private fun startChatServer() {
        executor.execute {
            try {
                ServerSocket(CHAT_PORT).use { server ->
                    while (running) {
                        val s = server.accept()
                        attachChat(s)
                    }
                }
            } catch (_: IOException) { }
        }
    }

    private fun connectChat(host: String) {
        if (socket?.isConnected == true && !socket!!.isClosed) return
        executor.execute {
            try {
                attachChat(Socket(host, CHAT_PORT))
            } catch (_: IOException) {
                runOnUiThread { status.text = "Chat connection failed" }
            }
        }
    }

    private fun attachChat(s: Socket) {
        socket = s
        peerIp = s.inetAddress.hostAddress
        writer = PrintWriter(BufferedWriter(OutputStreamWriter(s.getOutputStream())), true)
        runOnUiThread { status.text = "Connected • ${peerIp} • Offline" }

        executor.execute {
            try {
                BufferedReader(InputStreamReader(s.getInputStream())).use { reader ->
                    while (running) {
                        val line = reader.readLine() ?: break
                        runOnUiThread { chat.append("Other: $line\n") }
                    }
                }
            } catch (_: IOException) { }
        }
    }

    private fun sendMessage() {
        val text = message.text.toString().trim()
        if (text.isEmpty()) return
        val w = writer
        if (w == null) {
            toast("Connect to a device first")
            return
        }
        executor.execute { w.println(text) }
        chat.append("You: $text\n")
        message.text.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { sendFile(it) }
        }
    }

    private fun sendFile(uri: Uri) {
        val host = peerIp ?: return toast("Peer address unavailable")
        executor.execute {
            try {
                val name = queryDisplayName(uri) ?: "file"
                val size = contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: -1L
                if (size < 0) throw IOException("Unknown file size")

                Socket(host, FILE_PORT).use { s ->
                    DataOutputStream(BufferedOutputStream(s.getOutputStream())).use { out ->
                        val nameBytes = name.toByteArray(Charsets.UTF_8)
                        out.writeInt(nameBytes.size)
                        out.write(nameBytes)
                        out.writeLong(size)
                        contentResolver.openInputStream(uri).use { input ->
                            if (input == null) throw IOException("Cannot open file")
                            input.copyTo(out, 64 * 1024)
                        }
                        out.flush()
                    }
                }
                runOnUiThread { chat.append("You: sent $name ($size bytes)\n") }
            } catch (e: Exception) {
                runOnUiThread { toast("File send failed: ${e.message}") }
            }
        }
    }

    private fun startFileServer() {
        executor.execute {
            try {
                ServerSocket(FILE_PORT).use { server ->
                    while (running) {
                        val s = server.accept()
                        executor.execute { receiveFile(s) }
                    }
                }
            } catch (_: IOException) { }
        }
    }

    private fun receiveFile(s: Socket) {
        s.use { socket ->
            try {
                DataInputStream(BufferedInputStream(socket.getInputStream())).use { input ->
                    val nameLength = input.readInt()
                    require(nameLength in 1..4096)
                    val nameBytes = ByteArray(nameLength)
                    input.readFully(nameBytes)
                    val rawName = String(nameBytes, Charsets.UTF_8)
                    val safeName = rawName.replace(Regex("[^A-Za-z0-9._ -]"), "_")
                    val size = input.readLong()
                    require(size in 0..10L * 1024 * 1024 * 1024)

                    val dir = File(getExternalFilesDir(null), "received").apply { mkdirs() }
                    val outputFile = uniqueFile(dir, safeName)
                    FileOutputStream(outputFile).use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var remaining = size
                        while (remaining > 0) {
                            val n = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (n < 0) throw EOFException("Transfer ended early")
                            out.write(buffer, 0, n)
                            remaining -= n
                        }
                    }
                    runOnUiThread {
                        chat.append("Other: received $safeName ($size bytes)\n")
                        toast("Received: $safeName")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { toast("File receive failed: ${e.message}") }
            }
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 1
        while (f.exists()) {
            f = File(dir, "$base ($i)$ext")
            i++
        }
        return f
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return uri.lastPathSegment
    }

    private fun toast(text: String) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }
}
