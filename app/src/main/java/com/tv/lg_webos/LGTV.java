package com.tv.lg_webos;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.UiThread;

//import com.google.android.gms.security.ProviderInstaller;
import com.stealthcopter.networktools.ARPInfo;
import com.stealthcopter.networktools.WakeOnLan;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

// Inspired by the LG SDK:
// https://github.com/ConnectSDK/Connect-SDK-Android-Core/tree/master/src/com/connectsdk/service/webos

// Tips: We extends ContextWrapper, to have a context available in all the class
public class LGTV extends ContextWrapper {

    private final static String CST_WS =                "ws://";

    // Key to save parameter:
    private final static String KEY_TV_IP =             "key_tv_ip";
    private final static String KEY_TV_PORT =           "key_tv_port";
    private final static String KEY_CLIENT_KEY =        "key_client_key";

    private final static String LGTV =                   "LGTV";

    // todo: Find the right alias, try to PING the TV with: lgwebostv or lgsmarttv.lan or lgwebostv
    // Default IP (this is an alias)
    public final static String DEFAULT_LGTV_IP =         "192.168.1.10"; //lgwebostv";

    // Default Port used to communicate with LG Smart TV
    private final static String DEFAULT_LGTV_PORT =      "3000";
    // Time out in second before raise an exception if Smart TV does not response
    private final static int WEBSOCKET_TIMEOUT =         5;

    // Private values used by LG (Copied from LG SDK)
    // Keys used in Payload sent in a web socket
    private final static String SSAP_OPEN_CHANNEL =      "ssap://tv/openChannel";
    private final static String CHANNEL_NUMBER =         "channelNumber";
    private final static String MUTE =                   "mute";
    private final static String APP_ID =                 "id";
    private final static String APP_YOUTUBE =            "youtube.leanback.v4";
    private final static String APP_NETFLIX =            "netflix";
    private final static String APP_AMAZON =             "amazon";
    private final static String APP_LIVE_TV =            "com.webos.app.livetv";
    private final static String APP_HDMI1 =              "com.webos.app.hdmi1";
    private final static String APP_HDMI2 =              "com.webos.app.hdmi2";
    private final static String APP_HDMI3 =              "com.webos.app.hdmi3";
    private final static String APP_COMPONENT =          "com.webos.app.externalinput.component";
    private final static String APP_AV1 =                "com.webos.app.externalinput.av1";
    private final static String APP_USER_GUIDE =         "com.webos.app.tvuserguide";
    private final static String APP_SMART_SHARE =        "com.webos.app.smartshare";
    private final static String PAIRING =                "pairing";
    private final static String PAIRING_FILE =           "pairing.json";
    private final static String SSAP_ON =                "ssap://system/turnOn";
    private final static String SSAP_OFF =               "ssap://system/turnOff";           // OK
    private final static String SSAP_MUTE =              "ssap://audio/setMute";            // OK
    private final static String SSAP_VOLUME_UP =         "ssap://audio/volumeUp";           // OK
    private final static String SSAP_VOLUME_DOWN =       "ssap://audio/volumeDown";         // OK
    private final static String SSAP_CHANNEL_UP =        "ssap://tv/channelUp";
    private final static String SSAP_CHANNEL_DOWN =      "ssap://tv/channelDown";
    private final static String SSAP_PLAY =              "ssap://media.controls/play";
    private final static String SSAP_PAUSE =             "ssap://media.controls/pause";
    private final static String SSAP_STOP =              "ssap://media.controls/stop";
    private final static String SSAP_REWIND =            "ssap://media.controls/rewind";
    private final static String SSAP_FORWARD =           "ssap://media.controls/fastForward";
    private final static String SSAP_EPG =               "ssap://tv/getChannelProgramInfo";
    private final static String SSAP_UPDATE_INPUT =      "ssap://tv/switchInput";
    private final static String SSAP_APP_LAUNCH =        "ssap://system.launcher/launch";
    private final static String SSAP_APP_BROWSER =       "ssap://system.launcher/open";
    private final static String SSAP_MOUSE_SOCKET =      "ssap://com.webos.service.networkinput/getPointerInputSocket";
    private final static String WS_REQUEST =             "request";
    private final static String WS_REGISTER =            "register";
    private final static String JS_TYPE =                "type";
    private final static String JS_ID =                  "id";
    private final static String JS_URI =                 "uri";
    private final static String JS_PAYLOAD =             "payload";
    private final static String JS_RESPONSE =            "response";
    private final static String JS_CLIENT_KEY =          "client-key";
    private final static String JS_ERROR =               "error";
    private final static String JS_REGISTERED =          "registered";
    private final static String JS_SOCKET_PATH =         "socketPath";
    private final static String BTN_HOME =               "HOME";            // Not working
    private final static String BTN_BACK =               "BACK";
    private final static String BTN_UP =                 "UP";
    private final static String BTN_DOWN =               "DOWN";
    private final static String BTN_LEFT =               "LEFT";
    private final static String BTN_RIGHT =              "RIGHT";
    private final static String BTN_DASH =               "DASH";
    private final static String BTN_ENTER =              "ENTER";
    private final static String BTN_EXIT =               "EXIT";
    private final static String BTN_3D_MODE =            "3D_MODE";         // Not working
    private final static String BTN_RED =                "RED";
    private final static String BTN_GREEN =              "GREEN";
    private final static String BTN_YELLOW =             "YELLOW";
    private final static String BTN_BLUE =               "BLUE";
    private final static String BTN_GOTONEXT =           "GOTONEXT";
    private final static String BTN_GOTOPREV =           "GOTOPREV";

    // WebSocket global
    private static WebSocketClient mWebSocketClient;
    private static WebSocketClient mInputSocket;

    // To save if we have to mute or unmute the TV sound
    private static boolean m_isMute=false;

    // RFU
    private static boolean mConnected;

    // Global int incremented on each TV request (optional can be set to 0)
    private static int nextRequestId=1;

    // Global to save current BTN_ hit
    // Not very clean I know
    private static String m_keyName=null;

    // Globals variables to save IP and Port of the Smart TV
    private String myIP, myPort;
    // Global variable to save client key during TV pairing
    private static String client_key;

    // During instantiation, set the context from MainActivity
    // So we can access to the context every where in the class
    public LGTV(Context base) {
        super(base);
    }

    // Accessor for the IP global variable
    public String getMyIP() {return myIP;}

    // Accessor for the Port global variable
    public String getMyPort() {
        return myPort;
    }

    // In case IP is alias, convert it in format x.x.x.x
    public void resolveIP() {
        String[] ip_split = myIP.split("\\.");
        if( ip_split.length < 4 ) {
            try {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InetAddress inetAddr = InetAddress.getByName(myIP);
                            myIP = inetAddr.getHostAddress();
                            Log.d("IP", myIP);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (e.getMessage() != null) {
                                Log.e("MESSAGE_ERROR", e.getMessage());
                                postToastMessage("Exception: " + e.getMessage(), Toast.LENGTH_LONG);
                            }
                        }
                    }
                });
                thread.start();
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
                if (e.getMessage() != null) {
                    Log.e("MESSAGE_ERROR", e.getMessage());
                    postToastMessage("Exception: " + e.getMessage(), Toast.LENGTH_LONG);
                }
            }
        }
    }

    // Accessor for the IP global variable
    public void setMyIP(String lmyIP) {
        myIP = lmyIP;
    }

    // Save updated IP from UI
    public void saveIPPreference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TV_IP, getMyIP());
        editor.apply();
    }

    // Load saved IP, port and client key (pairing) from previous use
    public void loadMainPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        myIP = prefs.getString(KEY_TV_IP, DEFAULT_LGTV_IP);
        myPort = prefs.getString(KEY_TV_PORT, DEFAULT_LGTV_PORT);

        client_key = prefs.getString(KEY_CLIENT_KEY, "");
    }

    // To know it the application is already pair with the TV
    private static boolean is_registered() {
        return !client_key.isEmpty();
    }

    public String getDecodeurURL(String key)
    {
        return getDecodeurURL(getMyIP(), key);
    }

    public String getDecodeurURL(String IP, String key)
    {
        return getDecodeurURL(IP, key, "");
    }

    // Web socket URL start always with ws:// IP : Port
    public String getDecodeurURL(String IP, String key, String mode)
    {
        return CST_WS + IP + ":" + getMyPort();
        //return "ws://echo.websocket.org"; // Test
    }

    // Open the hardcoded json file from assets (use it as payload for pairing)
    public String getDecodeurBasicURL(String IP) {
        String tContents = "";

        try {
            InputStream stream = getAssets().open(PAIRING_FILE);
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (Exception e) {
            // Handle exceptions here
            e.printStackTrace();
        }

        return getRegisterURL(tContents);

        //Get version
        //return "{\"Params\":{\"Token\":\"LAN\",\"DeviceSoftVersion\":\"11.2.2\",\"Action\":\"GetVersions\",\"DeviceModel\":\"iPhone\",\"DeviceId\":\"375CC21F-2E8D-4C31-B728-7790E6D24BD0\"}}";
    }

    // Return payload for simple url
    private static String getSimpleURL(String prefix, String url) {
        JSONObject headers = new JSONObject();
        try {
            headers.put(JS_TYPE, WS_REQUEST);
            if(prefix==null)
                headers.put(JS_ID, 0);
            else
                headers.put(JS_ID, prefix + nextRequestId++);
            headers.put(JS_URI, url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers.toString();
    }

    // Return payload for keys with no parameter
    private static String getSimpleURL(String url) {
        JSONObject headers = new JSONObject();
        try {
            headers.put(JS_TYPE, WS_REQUEST);
            headers.put(JS_ID, String.valueOf(nextRequestId++));
            headers.put(JS_URI, url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers.toString();
    }

    // Return payload for SSAP_ keys with boolean parameter
    private static String getPayloadURL(String url, String name, boolean value) {
        JSONObject headers = new JSONObject();
        try {
            JSONObject payload = new JSONObject();
            payload.put(name, value);

            headers.put(JS_TYPE, WS_REQUEST);
            headers.put(JS_ID, String.valueOf(nextRequestId++));
            headers.put(JS_URI, url);
            headers.put(JS_PAYLOAD, payload);                   // Ok for Mute
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers.toString();
    }

    /*
    // Return payload for SSAP_ keys with int parameter
    private static String getPayloadURL(String url, String name, int value) {
        JSONObject headers = new JSONObject();
        try {
            JSONObject payload = new JSONObject();
            payload.put(name, value);

            headers.put(JS_TYPE, WS_REQUEST);
            headers.put(JS_ID, String.valueOf(nextRequestId++));
            headers.put(JS_URI, url);
            headers.put(JS_PAYLOAD, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers.toString();
    }
*/

    // Return payload for SSAP_ keys with String parameter
    private static String getPayloadURL(String url, String name, String value) {
        JSONObject headers = new JSONObject();
        try {
            JSONObject payload = new JSONObject();
            payload.put(name, value);

            headers.put(JS_TYPE, WS_REQUEST);
            headers.put(JS_ID, String.valueOf(nextRequestId++));
            headers.put(JS_URI, url);
            headers.put(JS_PAYLOAD, payload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers.toString();
    }

    // Return payload for pairing
    private static String getRegisterURL(String value) {
        JSONObject headers = new JSONObject();
        try {
            headers.put(JS_TYPE, WS_REGISTER);
            headers.put(JS_ID, "register_0");

            if(is_registered()) {
                try {
                    JSONObject obj = new JSONObject(value);
                    obj.put(JS_CLIENT_KEY, client_key);
                    headers.put(JS_PAYLOAD, obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                headers.put(JS_PAYLOAD, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers.toString();
    }

    // Return payload for BTN_ keys
    private static String getButtonURL(String keyName) {
        m_keyName = keyName;
        return getSimpleURL(null, SSAP_MOUSE_SOCKET);
        //return "type:button\n" + "name:" + keyName + "\n" + "\n";
    }


    void ExecuteURL(String url, String keyValue) {
        resolveIP();
        ExecuteURL(getMyIP(), url, keyValue);
    }

    void ExecuteURL(String IP, String url, String keyValue) {
        try {
            if( (mWebSocketClient==null) || !mWebSocketClient.isOpen() ||
                    (mWebSocketClient.getConnection() == null) ||
                    ((mWebSocketClient.getConnection() != null) && !mWebSocketClient.getConnection().isOpen()) ||
                    (m_keyName!=null /*&& (mInputSocket==null || !mInputSocket.isOpen() ||
                    (mInputSocket.getConnection()==null) ||
                    ((mInputSocket.getConnection()!=null)&&!mInputSocket.getConnection().isOpen()) )*/ ))
                connectWebSocket(getDecodeurURL(IP,""), url, keyValue);
            else {
                Log.i("JSON_URL", url);
                // todo: Part to debug
                if(m_keyName!=null) {
                    if(mInputSocket!=null) {
                        mInputSocket.send("type:button\n" + "name:" + m_keyName + "\n" + "\n");
                        m_keyName=null;
                    }
                    else {
                        if(MainActivity.m_debugMode)
                            Toast.makeText(this, "Error: input socket is null !", Toast.LENGTH_LONG).show();
                    }
                }
                else
                    mWebSocketClient.send(url);

                if (!keyValue.isEmpty())
                    Toast.makeText(this, keyValue, Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            if(MainActivity.m_debugMode) Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Wake on lan, use the API: https://github.com/stealthcopter/AndroidNetworkTools
    private void wakeOnLan() {

        if(!MainActivity.isWifiAvailable(this)) {
            postToastMessage(this.getString(R.string.wifi_not_connected), Toast.LENGTH_LONG);
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    String ipAddress = getMyIP();
                    String[] ip = ipAddress.split("\\.");

                    if( ip.length != 4 ) {
                        InetAddress inetAddr = InetAddress.getByName(ipAddress);
                        byte[] addr = inetAddr.getAddress();
                        ipAddress = new String(addr);
                    }

                    Log.d("WOL", "IP: " + ipAddress);
                    String macAddress = ARPInfo.getMACFromIPAddress(ipAddress); //.getMacFromArpCache(ipAddress);
                    Log.d("WOL", "MAC address: " + macAddress);
                    WakeOnLan.sendWakeOnLan(ipAddress, macAddress);
                    if(MainActivity.m_debugMode)
                        postToastMessage("Power On (MAC: " + macAddress + ")", Toast.LENGTH_LONG);
                    else
                        postToastMessage("Power On", Toast.LENGTH_SHORT);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("WOL", "Exception: " + e.getMessage());
                    if(MainActivity.m_debugMode)
                        postToastMessage("Exception: " + e.getMessage(), Toast.LENGTH_LONG);
                }
            }
        }).start();
    }

    // Send the requested key to the TV
    public void send_key(String key, MainActivity.KEY_INDEX cmd_index) {
        switch (cmd_index)
        {
            case ZERO:
            case ONE:
            case TWO:
            case THREE:
            case FOUR:
            case FIVE:
            case SIX:
            case SEVEN:
            case HEIGHT:
            case NINE:
                ExecuteURL(getPayloadURL(SSAP_OPEN_CHANNEL, CHANNEL_NUMBER, key), key);
                break;
            case TV:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_LIVE_TV), key);
                break;
            case YOUTUBE:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_YOUTUBE), key);
                break;
            case NETFLIX:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_NETFLIX), key);
                break;
            case AMAZON:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_AMAZON), key);
                break;
            case HDMI1:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_HDMI1), key);
                break;
            case HDMI2:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_HDMI2), key);
                break;
            case HDMI3:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_HDMI3), key);
                break;
            case COMPONENT:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_COMPONENT), key);
                break;
            case AV1:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_AV1), key);
                break;
            case GUIDE:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_USER_GUIDE), key);
                break;
            case SMART_SHARE:
                ExecuteURL(getPayloadURL(SSAP_APP_LAUNCH, APP_ID, APP_SMART_SHARE), key);
                break;
            case ON:
                // Some LG cannot be power on.
                ExecuteURL(getSimpleURL(SSAP_ON), key);
                // So, let's try wake on lan:
                wakeOnLan(); // Maybe it need to be activated in parameter off the TV.
                break;
            case OFF:
                ExecuteURL(getSimpleURL(SSAP_OFF), key);
                break;
            case MUTE:
                ExecuteURL(getPayloadURL(SSAP_MUTE, MUTE, m_isMute), key);
                m_isMute=!m_isMute;
                break;
            case VOLUME_INCREASE:
                ExecuteURL(getSimpleURL(SSAP_VOLUME_UP), key);
                break;
            case VOLUME_DECREASE:
                ExecuteURL(getSimpleURL(SSAP_VOLUME_DOWN), key);
                break;
            case CHANNEL_INCREASE:
                ExecuteURL(getSimpleURL(SSAP_CHANNEL_UP), key);
                break;
            case CHANNEL_DECREASE:
                ExecuteURL(getSimpleURL(SSAP_CHANNEL_DOWN), key);
                break;
            case PLAY:
                ExecuteURL(getSimpleURL(SSAP_PLAY), key);
                break;
            case PAUSE:
                ExecuteURL(getSimpleURL(SSAP_PAUSE), key);
                break;
            case STOP:
                ExecuteURL(getSimpleURL(SSAP_STOP), key);
                break;
            case REWIND:
                ExecuteURL(getSimpleURL(SSAP_REWIND), key);
                break;
            case FORWARD:
                ExecuteURL(getSimpleURL(SSAP_FORWARD), key);
                break;
            case NEXT:
                ExecuteURL(getButtonURL(BTN_GOTONEXT), key);
                break;
            case PREVIOUS:
                ExecuteURL(getButtonURL(BTN_GOTOPREV), key);
                break;
            case PROGRAM:
                ExecuteURL(getSimpleURL(SSAP_EPG), key);
                break;
            case SOURCE:
                ExecuteURL(getSimpleURL(SSAP_UPDATE_INPUT), key);
                break;
            case INTERNET:
                ExecuteURL(getSimpleURL(SSAP_APP_BROWSER), key);
                break;
            case BACK:
                ExecuteURL(getButtonURL(BTN_BACK), key);
                break;
            case UP:
                ExecuteURL(getButtonURL(BTN_UP), key);
                break;
            case DOWN:
                ExecuteURL(getButtonURL(BTN_DOWN), key);
                break;
            case LEFT:
                ExecuteURL(getButtonURL(BTN_LEFT), key);
                break;
            case RIGHT:
                ExecuteURL(getButtonURL(BTN_RIGHT), key);
                break;
            case ENTER:
                ExecuteURL(getButtonURL(BTN_ENTER), key);
                break;
            case EXIT:
                ExecuteURL(getButtonURL(BTN_EXIT), key);
                break;
            case DASH:
                ExecuteURL(getButtonURL(BTN_DASH), key);
                break;
            case HOME:
                ExecuteURL(getButtonURL(BTN_HOME), key);
                break;
            case RED:
                ExecuteURL(getButtonURL(BTN_RED), key);
                break;
            case GREEN:
                ExecuteURL(getButtonURL(BTN_GREEN), key);
                break;
            case YELLOW:
                ExecuteURL(getButtonURL(BTN_YELLOW), key);
                break;
            case BLUE:
                ExecuteURL(getButtonURL(BTN_BLUE), key);
                break;
            case THREE_D:
                ExecuteURL(getButtonURL(BTN_3D_MODE), key);
                break;
            default:
                break;
        }
    }

    // Pair the TV with the application
    public void TV_Pairing() {
        try {
            if(MainActivity.isWifiAvailable(this))
                ExecuteURL(getDecodeurBasicURL(""), PAIRING);
            else
                Toast.makeText(this, getString(R.string.wifi_not_connected), Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            e.printStackTrace();
            if(MainActivity.m_debugMode) Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Connection with the TV by WebSocket (API: https://github.com/TooTallNate/Java-WebSocket)
    private void connectWebSocket(final String ws, final String json_data, final String channel_number) {
        URI uri;
        try {

            uri = new URI(ws);
        } catch (Exception e) {
            e.printStackTrace();
            mConnected = false;
            if (MainActivity.m_debugMode)
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            int connectTimeout = 1000;

            mWebSocketClient = new WebSocketClient(uri, new Draft_6455(), null, connectTimeout) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i("Websocket", "Opened");

                    if(!getDecodeurBasicURL("").equalsIgnoreCase(json_data)) {
                        send(getDecodeurBasicURL(""));
                        SystemClock.sleep(400);
                    }

                    Log.i("JSON_OPEN", json_data);
                    send(json_data);

                    mConnected = true;
                }

                @Override
                public void onMessage(String s) {

                    Log.i("Websocket", "onMessage: " + s);
                    try {
                        JSONObject obj = new JSONObject(s);

                        // In case of the response contain socketPath
                        // We need to open a new socket on this path (LG protocol)
                        // It is use by every button starting with: BTN_
                        // Handle pairing response and error response too
                        handleMessage(obj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i("Websocket", "Closed: " + s);
                    mConnected = false;
                }

                @Override
                public void onError(Exception e) {
                    Log.e("Websocket", "Error: " + e.getMessage());
                    mConnected = false;
                    postToastMessage(e.getMessage(), Toast.LENGTH_LONG);
                }
            };
            mWebSocketClient.setConnectionLostTimeout(WEBSOCKET_TIMEOUT);
            mWebSocketClient.connect(); // Don't put code below there
        } catch (Exception e) {
            e.printStackTrace();
            mConnected = false;
            Log.e("Websocket", "Exception: " + e.getMessage());
            if (MainActivity.m_debugMode)
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Check if the response contain a SocketPath
    // If yes: Open a socket with the path
    // If the response contain the client-key: Save it for next time (Use during pairing)
    // If the response contain an error: display it
    protected void handleMessage(JSONObject message) {
        String type = message.optString(JS_TYPE);
        Object payload = message.opt(JS_PAYLOAD);

        if (type.length() == 0)
            return;

        if (JS_RESPONSE.equals(type)) {
            if (payload != null) {
                try {
                    JSONObject jsonObj = (JSONObject)payload;
                    String socketPath = (String) jsonObj.get(JS_SOCKET_PATH);
                    if(!socketPath.isEmpty()) {
                        try {
                            URI uri = new URI(socketPath);
                            // Connect using the path value
                            connectPointer(uri);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            // In case an exception occurs
                            // Display the message to know why
                            // Not sure to be on main UI thread here, send debug message on UI thread
                            if(MainActivity.m_debugMode)
                                postToastMessage("URISyntaxException: " + e.getMessage(), Toast.LENGTH_LONG);
                        }
                    }
                } catch (JSONException e) {
                    Log.d(LGTV, "JSONException: " + e.getMessage());
                }
            }
        } else if (JS_REGISTERED.equals(type)) {
            if (payload instanceof JSONObject) {
                client_key = ((JSONObject) payload).optString(JS_CLIENT_KEY);

                // Save client key for next time
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_CLIENT_KEY, client_key);
                editor.apply();
            }
        } else if (JS_ERROR.equals(type)) {
            String error = message.optString(JS_ERROR);
            if (error.length() == 0)
                return;

            int errorCode = -1;
            String errorDesc = null;
            String toastMessage = "";

            try {
                String [] parts = error.split(" ", 2);
                errorCode = Integer.parseInt(parts[0]);
                errorDesc = parts[1];
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (payload != null) {
                toastMessage = "Error Payload: " + payload.toString() + " ";
                Log.d(LGTV, toastMessage);
            }

            if (message.has("id")) {
                toastMessage += "Error Desc: " + errorDesc + " ";
                Log.d(LGTV, toastMessage);
            }
            if(errorCode != -1) {
                toastMessage += "Error code: " + errorCode + " ";
                Log.d(LGTV, toastMessage);
            }

            if(MainActivity.m_debugMode && !toastMessage.isEmpty()) {
                postToastMessage(toastMessage, Toast.LENGTH_LONG);
            }
        }
    }

    // Toast (debug information) must be executed on main thread
    // the main thread is reserved to the UI (graphic part)
    @UiThread
    public void postToastMessage(final String message, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(LGTV.this, message, duration).show();
            }
        });
    }

    // Function to trust every one during TLS connection
    // No need to understand what is it, just call it once for all
    private void trustEveryone() {
        try {
            Log.d("TV", "Trust everyone !");
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    Log.d("TRUST", hostname);
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                    Log.d("TRUST", "checkClientTrusted - authType = " + authType);
                }
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                    Log.d("TRUST", "checkServerTrusted - authType = " + authType);
                }
                public X509Certificate[] getAcceptedIssuers() {
                    Log.d("TRUST", "getAcceptedIssuers");
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
            Log.d("TRUST", "Trust exception = " + e.getMessage());
            if(MainActivity.m_debugMode)
                postToastMessage("Trust exception = " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    // Open a new Web Socket with the path in parameter
    public void connectPointer(URI uri) {
        try {
            if (mInputSocket != null) {
                // In case a socket already open, we close it
                mInputSocket.close();
                mInputSocket = null;
            }

            // Call once for all todo: maybe move this else where
            trustEveryone();

            mInputSocket = new WebSocketClient(uri, new Draft_6455(), null, 2000) {
                @Override
                public void onOpen(ServerHandshake arg0) {
                    Log.d("PtrAndKeyboardFragment", "connected to " + uri.toString());
                    mInputSocket.send("type:button\n" + "name:" + m_keyName + "\n" + "\n");
                    m_keyName=null;
                }

                @Override
                public void onMessage(String arg0) {
                    Log.i("Inputsocket", "onMessage: " + arg0);
                }

                @Override
                public void onError(Exception arg0) {
                    Log.e("Inputsocket", "Error: " + arg0.getMessage());
                    postToastMessage("Inputsocket: " + arg0.getMessage(), Toast.LENGTH_LONG);
                    // todo: HERE to correct connectPointer() because error:
                    // Inputsocket: Connection closed by peer
                }

                @Override
                public void onClose(int arg0, String arg1, boolean arg2) {
                    Log.i("Inputsocket", "Closed: " + arg1);
                }
            };
        } catch (Exception e) {
            Log.e("WebSocketClient", "Exception: " + e.getMessage());
            e.printStackTrace();
            postToastMessage("WebSocketClient: " + e.getMessage(), Toast.LENGTH_LONG);
        }

        try {
            //ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // todo : Test if TLS v1.1 or v1.2 is working better or not
            //SSLContext sslContext = SSLContext.getInstance("TLSv1.1");
            //SSLContext sslContext = SSLContext.getInstance("TLSv1.2");

            WebOSTVTrustManager customTrustManager = new WebOSTVTrustManager();
            sslContext.init(null, new WebOSTVTrustManager[] {customTrustManager}, null);
            //Web-Socket 1.3.7 patch
            //mInputSocket.setSocket(sslContext.getSocketFactory().createSocket());
            mInputSocket.setSocketFactory(sslContext.getSocketFactory());
            mInputSocket.setConnectionLostTimeout(0);
        } catch (Exception e) {
            Log.e("SSLContext", "Exception: " + e.getMessage());
            e.printStackTrace();
            postToastMessage("SSLContext: " + e.getMessage(), Toast.LENGTH_LONG);
        }
        //patch ends
        mInputSocket.connect();
    }

}