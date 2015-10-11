package com.calhacks.sendr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXUser;

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
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String LOGIN_URL = "http://40.122.208.196:3002/api/connect";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init Magnet
        MMX.init(this, R.raw.sendr);

        prefs = getSharedPreferences("com.calhacks.sendr", Context.MODE_PRIVATE);

        if (prefs.contains("name"))
        {
            // hide all images, edit texts, and buttons
            ImageView image = (ImageView) findViewById(R.id.animal_icon);
            image.setVisibility(View.GONE);
            EditText editText = (EditText) findViewById(R.id.name);
            editText.setVisibility(View.GONE);
            Button button = (Button) findViewById(R.id.send_button);
            button.setVisibility(View.GONE);

            this.sendMessageStored();
        }
        else
        {
            TextView loginText = (TextView) findViewById(R.id.login);
            loginText.setText("");
        }
    }

    public void sendMessage(View view)
    {
        String deviceId = this.getUID();
        prefs.edit().putString("uid", deviceId).apply();

        // get the editText message
        EditText editText = (EditText) findViewById(R.id.name);
        String name = editText.getText().toString();
        prefs.edit().putString("name", name).apply();

        // login magnet with the name
        byte[] password = "magnet".getBytes();

        MMXUser user = new MMXUser.Builder().username(name).build();
        user.register(password, new MMXUser.OnFinishedListener<Void>() {
            public void onSuccess(Void aVoid) {
                Log.d("Magnet", "Registration Success");
            }

            public void onFailure(MMXUser.FailureCode failureCode, Throwable throwable) {

                if (MMXUser.FailureCode.REGISTRATION_INVALID_USERNAME.equals(failureCode)) {
                    //handle registration failure
                    Log.d("Magnet", "Registration Failure");
                }
            }
        });

        // we will send the data to the server to query list of users nearby and then move to another activity
        JSONObject data = new JSONObject();
        try
        {
            data.put("uid", deviceId);
            data.put("name", name);
            data.put("device_type", "mobile");

            // send data
            new SendData(prefs).execute(data);
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }
    }

    private String getUID()
    {
        // get phone unique id
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());

        return deviceUuid.toString();
    }

    private void sendMessageStored()
    {
        JSONObject data = new JSONObject();
        try
        {
            data.put("uid", prefs.getString("uid", "Epic UID Failed Tim!"));
            data.put("name", prefs.getString("name", "Epic Name Failed Tim!"));
            data.put("device_type", "mobile");

            // send data
            new SendData(prefs).execute(data);
        }
        catch (JSONException exception)
        {
            exception.printStackTrace();
        }
    }

    private class SendData extends AsyncTask<JSONObject, Void, JSONObject>
    {
        private SharedPreferences prefs;

        public SendData(SharedPreferences prefs)
        {
            this.prefs = prefs;
        }

        @Override
        protected JSONObject doInBackground(JSONObject... data)
        {
            // send data
            try
            {
                URL url = new URL(LOGIN_URL);
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

                // Read
                InputStream in = new BufferedInputStream(connection.getInputStream());
                try
                {
                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder responseStrBuilder = new StringBuilder();

                    String inputStr;
                    while ((inputStr = streamReader.readLine()) != null)
                        responseStrBuilder.append(inputStr);

                    return new JSONObject(responseStrBuilder.toString());
                }
                catch (JSONException exception)
                {
                    exception.printStackTrace();
                }
            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject result)
        {
            Log.d("Connected Data JSON: ", result.toString());
            this.prefs.edit().putString("json", result.toString()).apply();

            // start magnet service
            startService(new Intent(getApplicationContext(), MagnetService.class));

            finish();
        }
    }
}
