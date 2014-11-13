package com.cowbell.cordova.geofence;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.location.Geofence;
import com.google.gson.Gson;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import android.content.SharedPreferences;


public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "GeofencePlugin";
    private GeoNotificationManager geoNotificationManager;
    private Context context;
    protected static Boolean isInBackground = true;
    private static CordovaWebView webView;
    public static final String PREFS_NAME = "GeofencePluginPrefsFile";
    protected SharedPreferences settings;

    /**
     * @param cordova The context of the main Activity.
     * @param webView The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView = webView;
        context = this.cordova.getActivity().getApplicationContext();
        Logger.setLogger(new Logger(TAG, context, false));
        geoNotificationManager = new GeoNotificationManager(context);
        settings = context.getSharedPreferences(PREFS_NAME, 0);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "GeofencePlugin execute action: "+ action + " args: " + args.toString());

        if(action.equals("addOrUpdate")) {
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
            for(int i=0; i<args.length();i++) {
            	GeoNotification not = parseFromJSONObject(args.getJSONObject(i));
            	if(not != null){
                    geoNotifications.add(not);
            	}
            }
            geoNotificationManager.addGeoNotifications(geoNotifications, callbackContext);
        }
        else if(action.equals("remove")) {
            List<String> ids = new ArrayList<String>();
            for(int i=0; i<args.length();i++){
                ids.add(args.getString(i));
            }
            geoNotificationManager.removeGeoNotifications(ids, callbackContext);
        }
        else if(action.equals("removeAll")) {
            geoNotificationManager.removeAllGeoNotifications(callbackContext);
        }
        else if(action.equals("getWatched")) {
        	List<GeoNotification> geoNotifications = geoNotificationManager.getWatched();
        	Gson gson = new Gson();
        	callbackContext.success(gson.toJson(geoNotifications));
        }
        else if(action.equals("initialize")) {

        }
        else if(action.equals("configApi")) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("apiUrl", args.getString(0));
            editor.putString("authToken", args.getString(1));
            editor.commit();

            Log.d(TAG, "Stored apiUrl: "+settings.getString("apiUrl", ""));
            Log.d(TAG, "Stored authToken: "+settings.getString("authToken", ""));

        }
        else {
            return false;
        }
        return true;

    }

    private GeoNotification parseFromJSONObject(JSONObject object){
        GeoNotification geo = null;
        geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    public static void fireRecieveTransition (List<GeoNotification> notifications) {
    	Gson gson = new Gson();
        String js     = "setTimeout('geofence.recieveTransition("+ gson.toJson(notifications)  + ")',0)";
        webView.sendJavascript(js);
    }

}