/*
 * Copyright (C) 2012 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package ch.fixme.status;

import java.io.ByteArrayOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

    private Context mCtxt;
    private AppWidgetManager mManager;

    public void onUpdate(Context ctxt, AppWidgetManager manager,
            int[] appWidgetIds) {
        Log.e("TEST", "onUpdate");
        mCtxt = ctxt;
        mManager = manager;
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            new GetApiTask(appWidgetId).execute(PreferenceManager
                    .getDefaultSharedPreferences(ctxt).getString(Main.API_KEY,
                            Main.API_DEFAULT));
        }
        super.onUpdate(ctxt, manager, appWidgetIds);
    }

    private class GetImage extends AsyncTask<String, Void, byte[]> {

        private int mId;

        public GetImage(int id) {
            mId = id;
        }

        @Override
        protected byte[] doInBackground(String... url) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                new Net(url[0], os);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return os.toByteArray();
        }

        @Override
        protected void onPostExecute(byte[] result) {
            updateWidget(mCtxt, mId, mManager,
                    BitmapFactory.decodeByteArray(result, 0, result.length));
        }

    }

    protected static void updateWidget(final Context ctxt, int widgetId,
            AppWidgetManager manager, Bitmap bitmap) {
        RemoteViews views = new RemoteViews(ctxt.getPackageName(),
                R.layout.widget);
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_image, bitmap);
        } else {
            views.setImageViewResource(R.id.widget_image,
                    android.R.drawable.ic_popup_sync);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(ctxt, 0,
                new Intent(ctxt, Main.class), 0);
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
        manager.updateAppWidget(widgetId, views);
    }

    private class GetApiTask extends AsyncTask<String, Void, String> {

        private int mId;

        public GetApiTask(int id) {
            mId = id;
        }

        @Override
        protected String doInBackground(String... url) {
            ByteArrayOutputStream spaceOs = new ByteArrayOutputStream();
            try {
                new Net(url[0], spaceOs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return spaceOs.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject api = new JSONObject(result);
                // Mandatory fields
                String status = Main.API_ICON_CLOSED;
                if (api.getBoolean(Main.API_STATUS)) {
                    status = Main.API_ICON_OPEN;
                }
                // Status icon or space icon
                if (!api.isNull(Main.API_ICON)) {
                    JSONObject status_icon = api.getJSONObject(Main.API_ICON);
                    if (!status_icon.isNull(status)) {
                        new GetImage(mId)
                                .execute(status_icon.getString(status));
                    }
                } else {
                    new GetImage(mId).execute(api.getString(Main.API_LOGO));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}