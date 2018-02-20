package com.cogentworks.overwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import layout.OverWidgetConfigure;

import static android.app.Activity.RESULT_OK;

/**
 * Created by cyun on 8/6/17.
 */

public class RestOperation extends AsyncTask<String, Void, Profile> {
    private static final String TAG = "RestOperation";

    private Context context;
    private AppWidgetManager appWidgetManager;
    private int appWidgetId;
    private HttpsURLConnection urlConnection;

    private Activity mActivity;
    private boolean checkProfileExists;
    public boolean ShowToast = true;

    private String battleTag;
    private String platform;
    private String region;

    private String errorMsg = "Error";

    public RestOperation(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        this.context = context;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
        this.checkProfileExists = false;
    }

    public RestOperation(Context context, int appWidgetId) {
        this.context = context;
        this.appWidgetId = appWidgetId;
        this.mActivity = (Activity) context;
        this.checkProfileExists = true;
    }

    @Override
    protected Profile doInBackground(String... params) {
        Profile result = null;

        this.battleTag = params[0];
        this.platform = params[1];
        if (platform != null) {
            if (platform.equals("PC")) {
                this.region = params[2]; // Set region on PC
            } else {
                this.region = "any"; // Set region to any on console
            }

            if (battleTag != null) {
                try {
                    // Create URL
                    URL endpoint = new URL("https://owapi.net/api/v3/u/" + battleTag.replace('#', '-') + "/blob?platform=" + platform.toLowerCase());
                    Log.d(TAG, endpoint.toString());

                    // Create connection
                    urlConnection = (HttpsURLConnection) endpoint.openConnection();

                    // Set methods and timeouts
                    urlConnection.setRequestMethod("GET");
                    //urlconnection.setReadTimeout(READ_TIMEOUT);
                    //urlconnection.setConnectTimeout(CONNECTION_TIMEOUT);

                    // Connect to URL
                    urlConnection.connect();

                    if (urlConnection.getResponseCode() == 200) {
                        result = new Profile();
                        // Success
                        InputStream responseBody = new BufferedInputStream(urlConnection.getInputStream());

                        // Parser
                        JsonParser jsonParser = new JsonParser();
                        JsonObject stats = jsonParser.parse(new InputStreamReader(responseBody, "UTF-8"))
                                .getAsJsonObject().get(region.toLowerCase()) // Select Region
                                .getAsJsonObject().get("stats")
                                .getAsJsonObject().get("competitive")
                                .getAsJsonObject().getAsJsonObject("overall_stats");

                        result.SetUser(battleTag, stats.get("avatar").getAsString());
                        Log.d(TAG, "SetUser");
                        result.SetLevel(stats.get("level").getAsString(), stats.get("prestige").getAsString(), stats.get("rank_image").getAsString());
                        Log.d(TAG, "SetLevel");
                        try {
                            result.SetRank(stats.get("comprank").getAsString(), stats.get("tier").getAsString());
                        } catch (UnsupportedOperationException e) {
                            result.SetRank("- - -", "nullrank");
                        }
                        Log.d(TAG, "SetRank");

                        /*JsonObject heroStats = jsonParser.parse(new InputStreamReader(responseBody, "UTF-8"))
                                .getAsJsonObject().get(region.toLowerCase()) // Select Region
                                .getAsJsonObject().get("heroes")
                                .getAsJsonObject().get("playtime")
                                .getAsJsonObject().getAsJsonObject("quickplay");*/

                        //result.SetHero(heroStats.entrySet()[0].getKey());

                        responseBody.close();
                        Log.d(TAG, "responseBody.close");
                    } else {
                        // Other response code
                        errorMsg = urlConnection.getResponseMessage();
                        Log.e(TAG, errorMsg);
                        result = null;
                    }
                    urlConnection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    result = null;
                }
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(Profile result) {
        super.onPostExecute(result);

        if (!checkProfileExists) { // Normal (from home screen)
            if (result != null) {
                // Convert Profile to Gson and save to SharedPrefs
                toGson(result);

                WidgetUtils.setWidgetViews(context, result, this.appWidgetId, this.appWidgetManager);
                Log.d(TAG, "RestOperation completed");
            } else { // Error
                if (ShowToast) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                }
                WidgetUtils.setSyncClicked(context, this.appWidgetId, this.appWidgetManager);
            }
        } else { // From ConfigureActivity
            ProgressBar progressBar = mActivity.findViewById(R.id.progress_bar);
            LinearLayout content = mActivity.findViewById(R.id.layout_main);
            FloatingActionButton fab = mActivity.findViewById(R.id.fab);

            if (result != null) {
                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                WidgetUtils.setWidgetViews(context, result, this.appWidgetId, appWidgetManager);

                // Convert Profile to Gson and save to SharedPrefs
                toGson(result);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                mActivity.setResult(RESULT_OK, resultValue);
                mActivity.finish();
                Log.d(TAG, "Check profile completed");
            } else {
                //progressBar.setVisibility(View.GONE);
                //content.setVisibility(View.VISIBLE);
                OverWidgetConfigure.crossfade(context, content, progressBar);
                fab.show();
                Snackbar.make(content, errorMsg, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void toGson(Profile result) {
        SharedPreferences.Editor newPrefs = context.getSharedPreferences(OverWidgetConfigure.PREFS_NAME, 0).edit();
        Gson gson = new Gson();
        String profileJson = gson.toJson(result);
        newPrefs.putString(OverWidgetConfigure.PREF_PREFIX_KEY + appWidgetId + "_profile", profileJson);
        newPrefs.apply();
    }
}