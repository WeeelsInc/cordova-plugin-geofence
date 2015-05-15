package com.cowbell.cordova.geofence;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import android.content.SharedPreferences;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

public class ReceiveTransitionsIntentService extends IntentService {
    protected BeepHelper beepHelper;
    protected GeoNotificationNotifier notifier;
    protected GeoNotificationStore store;
    protected SharedPreferences settings;
    protected String apiUrl;
    protected String authToken;
    public static final String PREFS_NAME = "GeofencePluginPrefsFile";

    /**
     * Sets an identifier for the service
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
        beepHelper = new BeepHelper();
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
    }

    /**
     * Handles incoming intents
     *
     * @param intent
     *            The Intent sent by Location Services. This Intent is provided
     *            to Location Services (inside a PendingIntent) when you call
     *            addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        notifier = new GeoNotificationNotifier(
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE),
                this
        );

        Logger logger = Logger.getLogger();
        logger.log(Log.DEBUG, "ReceiveTransitionsIntentService - onHandleIntent");

        settings = this.getSharedPreferences(PREFS_NAME, 0);
        apiUrl = settings.getString("apiUrl", "");
        authToken = settings.getString("authToken", "");

        

        // First check for errors
        if (LocationClient.hasError(intent)) {
            // Get the error code with a static method
            int errorCode = LocationClient.getErrorCode(intent);
            // Log the error
            logger.log(Log.ERROR,
                    "Location Services error: " + Integer.toString(errorCode));
            /*
             * You can also send the error code to an Activity or Fragment with
             * a broadcast Intent
             */
            /*
             * If there's no error, get the transition type and the IDs of the
             * geofence or geofences that triggered the transition
             */
        } else {
            // Get the type of transition (entry or exit)
            int transitionType =
                    LocationClient.getGeofenceTransition(intent);
              if  ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                 ||
                (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
               ) {
                logger.log(Log.DEBUG, "Geofence transition detected");
                List <Geofence> triggerList = LocationClient.getTriggeringGeofences(intent);
                List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
                for(Geofence fence : triggerList){
                    String fenceId = fence.getRequestId();
                    if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        postUpdate(fenceId);
                    }
                }
            }
            else {
                logger.log(Log.ERROR,
                        "Geofence transition error: " +
                        transitionType);
            }
        }
    }

    private boolean postUpdate(String fenceId) {
        Logger logger = Logger.getLogger();
        if (apiUrl == "") {
            logger.log(Log.ERROR, "Could not post geofence update, apiUrl was empty");
            return false;
        }
        try {
            String url = apiUrl+"/hubs/"+fenceId+"/enter.json";
            logger.log(Log.INFO, "Using url: " + url);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(url);

            JSONObject params = new JSONObject();
            params.put("authToken", authToken);
            params.put("auth_token", authToken);

            StringEntity se = new StringEntity(params.toString());

            request.setEntity(se);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            logger.log(Log.INFO, "Posting to " + request.getURI().toString());
            HttpResponse response = httpClient.execute(request);
            logger.log(Log.INFO, "Response received: " + response.getStatusLine());
            if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.log(Log.ERROR, "Exception posting geofence update: " + e);
            e.printStackTrace();
            return false;
        }
    }

}
