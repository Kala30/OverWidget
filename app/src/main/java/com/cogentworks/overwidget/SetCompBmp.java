package com.cogentworks.overwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class SetCompBmp extends AsyncTask<String, Void, Bitmap> {

    Context context;
    RemoteViews views;
    AppWidgetManager appWidgetManager;
    int appWidgetId;

    public SetCompBmp(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views) {
        this.context = context;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
        this.views = views;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        Bitmap result = null;
        String url = params[0];

        if (url != null) {
            try {
                result = BuildAvatarBmp(url);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);

        views.setImageViewBitmap(R.id.appwidget_tier, result);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }

    private static Bitmap BuildAvatarBmp(String url) throws IOException {
        URL avatarUrl = new URL(url);
        InputStream inputStream = avatarUrl.openConnection().getInputStream();
        Bitmap avatarBmp = BitmapFactory.decodeStream(inputStream);

        inputStream.close();
        return avatarBmp;
    }
}
