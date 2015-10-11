package com.calhacks.sendr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXMessage;

import java.util.Map;

public class MagnetService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        // not used
        return null;
    }

    @Override
    public void onCreate()
    {
        Log.d("Magnet", "Magnet Service Started");

        SharedPreferences prefs = getSharedPreferences("com.calhacks.sendr", Context.MODE_PRIVATE);

        // login the user in Magnet
        byte[] password = "magnet".getBytes();
        MMX.login(prefs.getString("name", "Epic Name Failed Tim"), password, new MMX.OnFinishedListener<Void>() {
            public void onSuccess(Void aVoid) {
                MMX.start();
                Log.d("Magnet", "Login Success");
            }

            public void onFailure(MMX.FailureCode failureCode, Throwable throwable) {
                if (MMX.FailureCode.SERVER_AUTH_FAILED.equals(failureCode)) {
                    Log.d("Magnet", "Login Failed");
                }
            }
        });

        // create event listener
        MMX.EventListener eventListener =
                new MMX.EventListener() {
                    public boolean onMessageReceived(MMXMessage message) {
                        Log.d("Magnet", "onMessageReceived(): " + message.getId());
                        Map<String, String> response = message.getContent();
                        String link = response.get("message");

                        // handle youtube link or browser link
                        Intent openApp = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(openApp);

                        return false;
                    }
                };
        MMX.registerListener(eventListener);
    }
}
