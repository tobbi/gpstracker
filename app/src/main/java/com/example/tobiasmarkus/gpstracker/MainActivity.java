package com.example.tobiasmarkus.gpstracker;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public MainActivity this_ = this;

    private static EditText editTextGpsCoordinates;
    private static TextView textViewGpsEnabled;
    private static Button startStopButton;

    private static boolean isStarted = false;
    private static CountDownTimer startStopButtonCountdown;
    private static SeekBar seekBarInterval;
    private static SeekBar seekBarMinDistance;
    private TextView tv_seekbar1, tv_seekbar2;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("running", isStarted);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startStopButton = (Button)findViewById(R.id.button_startStopService);
        startStopButton.setEnabled(false);
        startStopButtonCountdown = new CountDownTimer(4000, 1000) {

            public void onTick(long millisUntilFinished) {
                startStopButton.setText("Checking status (" + ((millisUntilFinished / 1000) - 1) + ")");
                if(isStarted) {
                    enableServiceButton();
                    if(startStopButtonCountdown != null)
                        startStopButtonCountdown.cancel();
                }
            }

            public void onFinish() {
                startStopButton.setEnabled(true);
                startStopButton.setText("Start service");
                isStarted = false;
            }
        }.start();

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent locationServiceIntent = new Intent(this_, LocationService.class);

                if (!isStarted) {
                    locationServiceIntent.putExtra("gps_interval", (long) seekBarInterval.getProgress());
                    locationServiceIntent.putExtra("gps_distance", (float) seekBarMinDistance.getProgress());
                    seekBarInterval.setVisibility(View.INVISIBLE);
                    seekBarMinDistance.setVisibility(View.INVISIBLE);
                    startService(locationServiceIntent);

                } else {
                    seekBarInterval.setVisibility(View.VISIBLE);
                    seekBarMinDistance.setVisibility(View.VISIBLE);
                    Intent broadcast = new Intent();
                    broadcast.setAction("com.example.gpstracker.gps_listener_unbind");
                    sendBroadcast(broadcast);

                    stopService(locationServiceIntent);
                }
                isStarted = !isStarted;
                enableServiceButton();
            }
        });

        editTextGpsCoordinates = (EditText)findViewById(R.id.editText_gpsCoordinates);
        textViewGpsEnabled = (TextView)findViewById(R.id.textView_gpsStatus);
        seekBarInterval = (SeekBar)findViewById(R.id.seekBar);
        tv_seekbar1 = (TextView)findViewById(R.id.tv_Setting1);
        tv_seekbar2 = (TextView)findViewById(R.id.tv_Setting2);
        seekBarInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tv_seekbar1.setText("Interval (" + i + ")");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarMinDistance = (SeekBar)findViewById(R.id.minDistanceSeekbar);
        seekBarMinDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tv_seekbar2.setText("Min. Dist. (" + i + ")");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarInterval.setMax(10000);
        seekBarMinDistance.setMax(100);

        if(editTextGpsCoordinates != null)
            editTextGpsCoordinates.append("-- Initialized. Debug messages will appear below --\r\n");
    }

    public static void enableServiceButton()
    {
        if(startStopButton == null)
            return;
        if(isStarted)
        {
            startStopButton.setText("Stop service");
        }
        else
            startStopButton.setText("Start service");
        startStopButton.setEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public static class GPSReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String currentDateandTime = sdf.format(new Date());
            if(action.equals(LocationService.actionGPSLocationChanged))
            {
                Location gps_location = intent.getParcelableExtra("gps_location");
                editTextGpsCoordinates.append("-- " + currentDateandTime + " GPS location follows: --\r\n");
                editTextGpsCoordinates.append(gps_location.toString() + "\r\n");
                textViewGpsEnabled.setText("GPS on");
                textViewGpsEnabled.setTextColor(Color.GREEN);
            }
            if(action.equals(LocationService.actionGPSProviderStatus)) {
                Boolean gps_enabled = intent.getBooleanExtra("gps_enabled", false);
                editTextGpsCoordinates.append("-- " + currentDateandTime + " GPS provider status: " + gps_enabled + " --\r\n");
                textViewGpsEnabled.setText(gps_enabled ? "GPS on" : "GPS off");
                textViewGpsEnabled.setTextColor(gps_enabled ? Color.GREEN : Color.RED);
            }
            if(action.equals(LocationService.actionServiceHeartbeat)) {
                if(editTextGpsCoordinates != null)
                    editTextGpsCoordinates.append("-- " + currentDateandTime + " GPS Service Heartbeat --\r\n");
                if (!isStarted) {
                    isStarted = true;
                    enableServiceButton();
                }
            }
        }
    }
}
