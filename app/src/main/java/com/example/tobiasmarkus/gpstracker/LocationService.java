package com.example.tobiasmarkus.gpstracker;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.util.Xml;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dsv on 16.09.15.
 */
public class LocationService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //private SensorManager sensorManager;
    //private Sensor sensorOrientation;
    //private SensorEventListener sensorListener;

    private static LocationManager locationManager;
    private static LocationListener locationListener;
    private static final String TAG = "fhflSensorService";

    private static final String _baseString = "com.example.gpstracker";
    public static final String actionGPSProviderStatus = _baseString + ".gps_provider_status";
    public static final String actionGPSLocationChanged = _baseString + ".gps_location_changed";
    public static final String actionServiceStarted = _baseString + ".service_started";
    public static final String actionServiceHeartbeat = _baseString + ".gps_heartbeat";

    private static Boolean isRunning = false;

    private static TextFile textFile;
    private static TextFile kmlFile;

    private static String gpxContent =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"GPSTracker\">\n" +
            "    <metadata>\n" +
            "        <name>Test file created by GPSTracker</name>\n" +
            "    </metadata>\n" +
            "    <trk>\n" +
            "        <name>%name%</name>\n" +
            "        <trkseg>\n" +
            "             %trkpt\n" +
            "        </trkseg>\n" +
            "    </trk>\n" +
            "</gpx>";

    public static String kmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://earth.google.com/kml/2.1\"\n" +
            "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "  <Document>\n" +
            "    <name>GPS device</name>\n" +
            "    <Snippet>Created 08/13/13 17:23:21</Snippet>\n" +
            "<!-- Normal track style -->\n" +
            "    <Style id=\"track_n\">\n" +
            "      <IconStyle>\n" +
            "        <Icon>\n" +
            "          <href>http://earth.google.com/images/kml-icons/track-directional/track-none.png</href>\n" +
            "        </Icon>\n" +
            "      </IconStyle>\n" +
            "    </Style>\n" +
            "<!-- Highlighted track style -->\n" +
            "    <Style id=\"track_h\">\n" +
            "      <IconStyle>\n" +
            "        <scale>1.2</scale>\n" +
            "        <Icon>\n" +
            "          <href>http://earth.google.com/images/kml-icons/track-directional/track-none.png</href>\n" +
            "        </Icon>\n" +
            "      </IconStyle>\n" +
            "    </Style>\n" +
            "    <StyleMap id=\"track\">\n" +
            "      <Pair>\n" +
            "        <key>normal</key>\n" +
            "        <styleUrl>#track_n</styleUrl>\n" +
            "      </Pair>\n" +
            "      <Pair>\n" +
            "        <key>highlight</key>\n" +
            "        <styleUrl>#track_h</styleUrl>\n" +
            "      </Pair>\n" +
            "    </StyleMap>\n" +
            "<!-- Normal waypoint style -->\n" +
            "    <Style id=\"waypoint_n\">\n" +
            "      <IconStyle>\n" +
            "        <Icon>\n" +
            "          <href>http://maps.google.com/mapfiles/kml/pal4/icon61.png</href>\n" +
            "        </Icon>\n" +
            "      </IconStyle>\n" +
            "    </Style>\n" +
            "<!-- Highlighted waypoint style -->\n" +
            "    <Style id=\"waypoint_h\">\n" +
            "      <IconStyle>\n" +
            "        <scale>1.2</scale>\n" +
            "        <Icon>\n" +
            "          <href>http://maps.google.com/mapfiles/kml/pal4/icon61.png</href>\n" +
            "        </Icon>\n" +
            "      </IconStyle>\n" +
            "    </Style>\n" +
            "    <StyleMap id=\"waypoint\">\n" +
            "      <Pair>\n" +
            "        <key>normal</key>\n" +
            "        <styleUrl>#waypoint_n</styleUrl>\n" +
            "      </Pair>\n" +
            "      <Pair>\n" +
            "        <key>highlight</key>\n" +
            "        <styleUrl>#waypoint_h</styleUrl>\n" +
            "      </Pair>\n" +
            "    </StyleMap>\n" +
            "    <Style id=\"lineStyle\">\n" +
            "      <LineStyle>\n" +
            "        <color>99ffac59</color>\n" +
            "        <width>3</width>\n" +
            "      </LineStyle>\n" +
            "    </Style>\n" +
            "    <Folder>\n" +
            "      <name>Waypoints</name>\n" +
            "    </Folder>\n" +
            "    <Folder>\n" +
            "      <name>Tracks</name>\n" +
            "      <Folder>\n" +
            "        <name>%name%</name>\n" +
            "        <Snippet/>\n" +
            "        <Placemark>\n" +
            "          <name>Path</name>\n" +
            "          <styleUrl>#lineStyle</styleUrl>\n" +
            "          <LineString>\n" +
            "            <tessellate>1</tessellate>\n" +
            "            <coordinates>\n" +
           // "              9.435892,54.782644,57.000000\n" +
            "            %trkpt\n" +
            "            </coordinates>\n" +
            "          </LineString>\n" +
            "        </Placemark>\n" +
            "      </Folder>\n" +
            "    </Folder>\n" +
            "  </Document>\n" +
            "</kml>\n";

    @Override
    public void onCreate() {
        super.onCreate();

        //GPS
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());
        textFile = new TextFile("gpstracker", currentDateandTime + ".gpx", false);
        kmlFile = new TextFile("gpstracker", currentDateandTime + ".kml", false);
        gpxContent = gpxContent.replaceFirst("%name%", "gpstrack_" + currentDateandTime);
        kmlContent = kmlContent.replaceFirst("%name%", "gpstrack_" + currentDateandTime);
        textFile.saveText(gpxContent.replaceFirst("%trkpt", ""));
        kmlFile.saveText(kmlContent.replace("%trkpt", ""));

        locationListener = new LocationListener() {

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // TODO Auto-generated method stub
                Log.v(TAG, "locationListener.onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub
                Log.v(TAG, "locationListener.onProviderEnabled()");
                Intent broadcast = new Intent();
                broadcast.setAction(actionGPSProviderStatus);
                broadcast.putExtra("gps_enabled", true);
                sendBroadcast(broadcast);
            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub
                Log.v(TAG, "locationListener.onProviderDisabled()");
                Intent broadcast = new Intent();
                broadcast.setAction(actionGPSProviderStatus);
                broadcast.putExtra("gps_enabled", false);
                sendBroadcast(broadcast);
            }

            @Override
            public void onLocationChanged(Location location) {
                // TODO Auto-generated method stub
                Log.v(TAG, "locationListener.onLocationChanged()");
                Intent broadcast = new Intent();
                broadcast.setAction(actionGPSLocationChanged);
                broadcast.putExtra("gps_location", location);
                gpxContent = gpxContent.replace("%trkpt", "" +
                        "<trkpt lon=\"" + location.getLongitude() + "\" " +
                               "lat=\"" + location.getLatitude() + "\">\n" +
                        "                <ele>" + location.getAltitude() +"</ele>\n" +
                        "            </trkpt>\n" +
                        "%trkpt");

                kmlContent = kmlContent.replaceFirst("%trkpt",
                        location.getLatitude() + ","
                                + location.getLongitude() + "," + location.getAltitude()+"\n%trkpt");

                textFile.saveText(gpxContent.replaceFirst("%trkpt", ""));
                kmlFile.saveText(kmlContent.replaceFirst("%trkpt", ""));
                sendBroadcast(broadcast);
            }
        };

        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private long time = 0;

            @Override
            public void run() {
                if(!isRunning)
                    return;
                time += 1000;
                Intent broadcast = new Intent();
                broadcast.setAction(actionServiceHeartbeat);
                sendBroadcast(broadcast);
                h.postDelayed(this, 1000);
            }
        }, 1000); // 1 second delay (takes millis)

        isRunning = true;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null)
            return super.onStartCommand(intent, flags, startId);

        // Get service settings:
        // Get value of "gps_interval" or set to 0 (default value)
        long minTime      = intent.getLongExtra("gps_interval", 0);
        float minDistance = intent.getFloatExtra("gps_distance", 0);

        Intent broadcast = new Intent();
        broadcast.setAction(actionGPSProviderStatus);
        broadcast.putExtra("gps_enabled", locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        sendBroadcast(broadcast);

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, minTime, minDistance, locationListener);
        }
        catch(SecurityException e)
        {
            // We got no permissions.
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DESTROY", "Service destroyed");
    }

    public static class ShutdownReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if(locationManager != null)
                  locationManager.removeUpdates(locationListener);
                isRunning = false;
            } catch (SecurityException e)
            {
                // We got no permissions.
            }
        }
    }
}

