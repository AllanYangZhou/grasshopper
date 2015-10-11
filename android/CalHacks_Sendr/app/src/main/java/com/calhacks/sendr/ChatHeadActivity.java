package com.calhacks.sendr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.magnet.mmx.client.api.MMX;

public class ChatHeadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // get youtube data
        Bundle extras = getIntent().getExtras();
        String data = extras.getString(Intent.EXTRA_TEXT);

        // format the string to get http link only
        int index = data.indexOf("http");
        data = data.substring(index, data.length());

        // start the chat head server
        Intent chatHeadService = new Intent(this, ChatHeadService.class);
        chatHeadService.putExtra("youtube", data);
        this.startService(chatHeadService);

        // close the activity
        finish();
    }

}
