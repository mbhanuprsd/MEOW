
package com.pbsystems.meow.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.pbsystems.meow.R;
import com.pbsystems.meow.service.ChatService;

public class SipCallActivity extends Activity implements ChatService.OnCallStatusUpdateListener{
    TextView btnSipHang, sipLabel;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sipcall);
        btnSipHang = (TextView) findViewById(R.id.btnSipHang);
        sipLabel = (TextView) findViewById(R.id.sipLabel);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        btnSipHang.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("End call", "true");
                Intent serviceIntent = new Intent(getBaseContext(), ChatService.class);
                serviceIntent.putExtra("EndSipCall", "end");
                startService(serviceIntent);
            }
        });
    }

    @Override
    public void onCallStatusUpdate(String status) {
        if(sipLabel != null){
            sipLabel.setText(status);
        }
    }

    @Override
    public void onCallEnded() {
        finish();
    }
}