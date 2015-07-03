package com.pbsystems.meow.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pbsystems.meow.R;
import com.pbsystems.meow.service.ChatService;

public class LoginActivity extends Activity{

    private String UnameValue, PasswordValue = "";

    public String HOST = "172.16.32.162";
    public static final int PORT = 5222;

    EditText username, password;
    Button loginbutton, toregisterbutton;

    TextView securedtxt, crafttxt;
    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupUI(findViewById(R.id.loginMainView));

        dialog = ProgressDialog.show(this,
                "Logging In...", "Please wait...", false);

        username = (EditText) findViewById(R.id.txtusername);
        password = (EditText) findViewById(R.id.txtpassword);

        Drawable drawable = getResources().getDrawable(R.drawable.user);
        drawable.setBounds(0, 0, (int)(drawable.getIntrinsicWidth()*0.75),
                (int)(drawable.getIntrinsicHeight()*0.75));
        ScaleDrawable sd = new ScaleDrawable(drawable, 0, -1, -1);
        username.setCompoundDrawables(sd.getDrawable(), null, null, null);

        drawable = getResources().getDrawable(R.drawable.decrypt);
        drawable.setBounds(0, 0, (int)(drawable.getIntrinsicWidth()*0.75),
                (int)(drawable.getIntrinsicHeight()*0.75));
        sd = new ScaleDrawable(drawable, 0, -1, -1);
        password.setCompoundDrawables(sd.getDrawable(), null, null, null);

        securedtxt = (TextView) findViewById(R.id.tvSecured);
        crafttxt = (TextView) findViewById(R.id.tvCraft);

        Typeface nixieFace = Typeface.createFromAsset(getAssets(),
                "NixieOne-Regular.otf");
        securedtxt.setTypeface(nixieFace);

        Typeface econFace = Typeface.createFromAsset(getAssets(),
                "Economica-Regular-OTF.otf");
        crafttxt.setTypeface(econFace);

        loginbutton = (Button) findViewById(R.id.btnlogin);
        loginbutton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    UnameValue = username.getText().toString().trim();
                    PasswordValue = password.getText().toString().trim();
                    if (UnameValue.length() > 4 && PasswordValue.length() > 5) {
                        savePreferences();
                        login();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Incorrect credentials. Try Again",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Error logging in\n" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        toregisterbutton = (Button) findViewById(R.id.btntoregister);
        toregisterbutton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent("com.pbsystems.meow.REGISTERACTIVITY"));
            }
        });

        if (isMyServiceRunning()) {
            finish();
            startActivity(new Intent("com.pbsystems.meow.CHATACTIVITY"));
        }
    }

    protected void savePreferences() {
        SharedPreferences.Editor editor = getSharedPreferences("Chat_User",
                MODE_PRIVATE).edit();
        if (UnameValue != null)
            editor.putString("Username", UnameValue);
        if (PasswordValue != null)
            editor.putString("Password", PasswordValue);
        editor.putString("Host", HOST);
        editor.putInt("Port", PORT);
        editor.commit();
    }

    public void login() {
        if (!isMyServiceRunning()) {
            dialog.show();
            Intent serviceIntent = new Intent(getBaseContext(),
                    ChatService.class);
            startService(serviceIntent);
        } else {
            Toast.makeText(getApplicationContext(), "Service already running",
                    Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent("com.pbsystems.meow.CHATACTIVITY"));
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (ChatService.class.getName().equals(
                    service.service.getClassName())) {

                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onResume();
        username.setText("");
        password.setText("");
    }

    @Override
    protected void onPause() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onPause();
    }

    public void setupUI(View view) {
        if(!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(LoginActivity.this);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if(dialog.isShowing()){
            dialog.dismiss();
        }else{
            super.onBackPressed();
        }
    }
}
