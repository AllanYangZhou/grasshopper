package com.calhacks.sendr;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Image;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChatHeadService extends Service
{
    public static final String SHARE_URL = "http://40.122.208.196:3002/api/share";

    private WindowManager windowManager;
    private ImageView chatHead, closeButton, allButton, selectButton;
    private RelativeLayout buttonsView;
    private SharedPreferences prefs;
    private JSONObject json;
    private String youtubeLink;

    @Override
    public IBinder onBind(Intent intent)
    {
        // not used
        return null;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        youtubeLink = intent.getExtras().getString("youtube");

        Log.d("Link: ", youtubeLink);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        // get json from prefs
        prefs = getSharedPreferences("com.calhacks.sendr", Context.MODE_PRIVATE);

        try
        {
            json = new JSONObject(prefs.getString("json", "Epic JSON Failed Tim!"));
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // get screen size
        Display display = windowManager.getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);

        // get the layout view and buttons
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        buttonsView = (RelativeLayout) inflater.inflate(R.layout.buttons, null);
        final WindowManager.LayoutParams paramButtonsView = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        buttonsView.setVisibility(View.GONE);
        allButton = (ImageView) buttonsView.findViewById(R.id.all);
        selectButton = (ImageView) buttonsView.findViewById(R.id.select);
        closeButton = (ImageView) buttonsView.findViewById(R.id.close);

        // create chat head
        chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.round_logo);

        final WindowManager.LayoutParams paramsChatHead = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        paramsChatHead.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        paramsChatHead.width = 200;
        paramsChatHead.height = 200;

        // handle chatHead touch listener
        chatHead.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // show the buttons
                        buttonsView.setVisibility(View.VISIBLE);

                        initialX = paramsChatHead.x;
                        initialY = paramsChatHead.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                    case MotionEvent.ACTION_UP:
                        // hide the buttons
                        buttonsView.setVisibility(View.GONE);

                        int chatHeadCenterX = paramsChatHead.x + size.x / 2;
                        int chatHeadCenterY = paramsChatHead.y + size.y / 2;

                        // handle drag chat head to close button
                        if ((closeButton.getLeft() <= chatHeadCenterX) && (chatHeadCenterX <= closeButton.getRight()) &&
                                (closeButton.getTop() <= chatHeadCenterY) && (chatHeadCenterY <= closeButton.getBottom()))
                            stopService(new Intent(ChatHeadService.this, ChatHeadService.class));

                        // handle drag chat head to all button
                        if ((allButton.getLeft() <= chatHeadCenterX) && (chatHeadCenterX <= allButton.getRight()) &&
                                (allButton.getTop() <= chatHeadCenterY) && (chatHeadCenterY <= allButton.getBottom()))
                        {
                            try
                            {
                                JSONObject dataToSend = new JSONObject();
                                dataToSend.put("content_type", "link");
                                dataToSend.put("content", youtubeLink);
                                dataToSend.put("src_uid", prefs.getString("uid", "Epic UID Failed Tim!"));

                                JSONArray listAllDevices = new JSONArray();
                                JSONArray devices = json.getJSONArray("connected_data");
                                for (int index = 0; index < devices.length(); index++)
                                {
                                    listAllDevices.put(devices.getJSONObject(index).getString("uid"));
                                }
                                dataToSend.put("target_uids", listAllDevices.toString());

                                // send the data to server
                                Log.d("Share JSON: ", dataToSend.toString());
                                new PostData().execute(dataToSend);
                            }
                            catch (JSONException exception)
                            {
                                exception.printStackTrace();
                            }
                        }

                        // handle drag chat head to select button
                        if ((selectButton.getLeft() <= chatHeadCenterX) && (chatHeadCenterX <= selectButton.getRight()) &&
                                (selectButton.getTop() <= chatHeadCenterY) && (chatHeadCenterY <= selectButton.getBottom()))
                        {
                            Intent selectDeviceIntent = new Intent(getApplicationContext(), SelectDeviceToSend.class);
                            selectDeviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            selectDeviceIntent.putExtra("youtube", youtubeLink);
                            startActivity(selectDeviceIntent);
                            stopService(new Intent(ChatHeadService.this, ChatHeadService.class));
                        }

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // show the buttons
                        buttonsView.setVisibility(View.VISIBLE);

                        paramsChatHead.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramsChatHead.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(chatHead, paramsChatHead);
                        return true;
                }
                return false;
            }
        });

        // add the image views to window
        windowManager.addView(buttonsView, paramButtonsView);
        windowManager.addView(chatHead, paramsChatHead);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (chatHead != null)
            windowManager.removeView(chatHead);
    }

    private class PostData extends AsyncTask<JSONObject, Void, JSONObject>
    {
        @Override
        protected JSONObject doInBackground(JSONObject... data)
        {
            // send data
            try
            {
                URL url = new URL(SHARE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");
                connection.connect();

                // Write
                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(data[0].toString());
                writer.close();
                os.close();

                Log.d("Status Code: ", String.valueOf(connection.getResponseCode()));

                // Read
//                InputStream in = new BufferedInputStream(connection.getInputStream());
//                try
//                {
//                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//                    StringBuilder responseStrBuilder = new StringBuilder();
//
//                    String inputStr;
//                    while ((inputStr = streamReader.readLine()) != null)
//                        responseStrBuilder.append(inputStr);
//
//                    return new JSONObject(responseStrBuilder.toString());
//                }
//                catch (JSONException exception)
//                {
//                    exception.printStackTrace();
//                }
            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject result)
        {
            stopService(new Intent(ChatHeadService.this, ChatHeadService.class));
        }
    }
}
