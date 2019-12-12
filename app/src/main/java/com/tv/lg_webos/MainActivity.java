package com.tv.lg_webos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

// This is the main class called by Android at application start up
// Note: This is call too when the screen rotate !
public class MainActivity extends AppCompatActivity {

    private static LGTV m_tv;
    private static String m_current_key;
    private static int m_current_index;
    private static boolean m_firstLaunch = true;
    public static boolean m_debugMode;

    // This enum must be in same order of res > values > strings.xml : action_array
    public enum KEY_INDEX {
        ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, HEIGHT, NINE,
            TV, YOUTUBE, NETFLIX, AMAZON, HDMI1, HDMI2, HDMI3,
            COMPONENT, AV1, GUIDE, SMART_SHARE, ON, OFF, MUTE,
            VOLUME_INCREASE, VOLUME_DECREASE, CHANNEL_INCREASE,
            CHANNEL_DECREASE, PLAY, PAUSE, STOP, REWIND, FORWARD,
            NEXT, PREVIOUS, PROGRAM, SOURCE, INTERNET, BACK, UP,
            DOWN, LEFT, RIGHT, ENTER, EXIT, DASH, HOME, RED, GREEN,
            YELLOW, BLUE, THREE_D;

        static KEY_INDEX fromInt(int i) {
            return values()[i];
        }
    }

    // First function call by Android
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the view from res > layout > activity_main.xml
        setContentView(R.layout.activity_main);

        // Internal boolean, use to display message for debug purpose
        // this boolean will set to false on release compilation
        if (BuildConfig.DEBUG)
            m_debugMode = true;

        // Instantiate the class that will handle the connection to the LG Smart TV
        m_tv = new LGTV(this);
        // Load existing IP, Port etc inside the class
        m_tv.loadMainPreferences();

        // Find the IP value in activity_main.xml
        EditText ip_view = findViewById(R.id.editText);
        // Set the default IP value (previously saved)
        ip_view.setText(m_tv.getMyIP());
        // Set a text watch to save the IP if someone update it
        ip_view.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // Set the new IP in the LGTV class
                m_tv.setMyIP(s.toString());
                // Save the IP (use for next application restart)
                m_tv.saveIPPreference();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                // Nothing to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                // Nothing to do
            }
        });

        // Find the combo box/spinner value in activity_main.xml
        Spinner staticSpinner = findViewById(R.id.spinner);
        // By default insert every command in res > values > strings.xml : action_array
        // Creation of an adapter to set to the view
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(this, R.array.action_array,
                        android.R.layout.simple_spinner_item);
        // Set a default layout, here a dropdown item
        staticAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter to the view
        staticSpinner.setAdapter(staticAdapter);
        // Detect when some one click on a value in the spinner list
        staticSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.d("item", (String) parent.getItemAtPosition(position));
                // Save in global variable the selection action (string and position)
                // The position will be used in the enum
                m_current_key = (String) parent.getItemAtPosition(position);
                m_current_index = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Called when user click the button 'SEND'
    public void onSendButtonClick(View view) {
        if(m_current_key!=null && !m_current_key.isEmpty()) {
            Log.d("TV", "Send key: " + m_current_key);

            // We detect that the detect button has been clicked before this one
            if(m_firstLaunch) {
                Toast.makeText(this, getString(R.string.detect_tv_first), Toast.LENGTH_SHORT).show();
            } else {
                m_tv.send_key(m_current_key, KEY_INDEX.fromInt(m_current_index)); //KEY_INDEX.values()[m_current_index]);
            }
        }
        else
            Toast.makeText(this, getString(R.string.select_action_first), Toast.LENGTH_SHORT).show();
    }

    // Called when user click the button 'DETECT'
    public void onDetectButtonClick(View view) {
        m_tv.TV_Pairing();
        m_firstLaunch = false;
    }

    // Detect if the WIFI is available
    // Because you need to be on same network to control your Smart TV
    public static boolean isWifiAvailable(Context context)
    {
        if (BuildConfig.DEBUG)
            return true; // Temp for debug

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
        if(cm!=null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return (capabilities != null) && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
            } else {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                return (ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI);
            }
        } else
            return false;
    }
}
