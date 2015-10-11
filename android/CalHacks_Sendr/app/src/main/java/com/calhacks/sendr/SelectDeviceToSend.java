package com.calhacks.sendr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class SelectDeviceToSend extends AppCompatActivity
        implements android.widget.CompoundButton.OnCheckedChangeListener
{
    public static final String SHARE_URL = "http://40.122.208.196:3002/api/share";

    private ListView deviceListView;
    private ArrayList<DeviceListDataClass> rowDataArray;
    private DeviceListAdapter deviceAdapter;
    private JSONObject json;
    private SharedPreferences prefs;
    private String link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device_to_send);

        // get youtube link
        link = getIntent().getExtras().getString("youtube");

        // get json from prefs
        prefs = getSharedPreferences("com.calhacks.sendr", Context.MODE_PRIVATE);
        rowDataArray = new ArrayList<>();
        try
        {
            json = new JSONObject(prefs.getString("json", "Epic JSON Failed Tim"));

            // loop through the json and add data text
            JSONArray jsonArray = json.getJSONArray("connected_data");
            for (int index = 0; index < jsonArray.length(); index++)
            {
                String uid = jsonArray.getJSONObject(index).getString("uid");
                String name = jsonArray.getJSONObject(index).getString("name");
                String device = jsonArray.getJSONObject(index).getString("device_type");

                // create a new DeviceListDataClass instance
                DeviceListDataClass data = new DeviceListDataClass(name, device, uid);
                rowDataArray.add(data);
            }
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }

        deviceListView = (ListView) findViewById(R.id.list_device);
        deviceAdapter = new DeviceListAdapter(rowDataArray, this);
        deviceListView.setAdapter(deviceAdapter);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        int pos = deviceListView.getPositionForView(buttonView);
        if (pos != ListView.INVALID_POSITION) {
            DeviceListDataClass data = rowDataArray.get(pos);
            data.setSelected(isChecked);
        }
    }

    public void onSendClicked(View view)
    {
        try
        {
            JSONObject dataToSend = new JSONObject();
            dataToSend.put("content_type", "link");
            dataToSend.put("content", link);
            dataToSend.put("src_uid", prefs.getString("uid", "Epic UID Failed Tim!"));

            JSONArray jsonArray = new JSONArray();
            for (int index = 0; index < rowDataArray.size(); index++)
            {
                if (rowDataArray.get(index).isSelected())
                    jsonArray.put(rowDataArray.get(index).getUID());
            }
            dataToSend.put("target_uids", jsonArray.toString());

            // send the data to server
            Log.d("Share JSON: ", dataToSend.toString());
            new PostData().execute(dataToSend);
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }
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
            finish();
        }
    }
}
