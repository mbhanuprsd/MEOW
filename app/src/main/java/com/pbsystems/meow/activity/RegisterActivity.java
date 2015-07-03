package com.pbsystems.meow.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pbsystems.meow.R;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends Activity {

    public String HOST = "";
    public int PORT = 0;

    AbstractXMPPConnection conn;

    Button registerbutton, loginbn;

    EditText regUser, regpass, regname, regemail;

    String struser, strpass, strname, stremail = "";

    boolean passwordShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setupUI(findViewById(R.id.registerMainView));

        SharedPreferences prefs = getSharedPreferences("Chat_User",
                MODE_PRIVATE);
        if (prefs != null) {
            HOST = prefs.getString("Host", "");
            PORT = prefs.getInt("Port", 5222);
        } else {
            Toast.makeText(RegisterActivity.this, "Set host property",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        regUser = (EditText) findViewById(R.id.regUsername);
        regpass = (EditText) findViewById(R.id.regPassword);
        regname = (EditText) findViewById(R.id.regName);
        regemail = (EditText) findViewById(R.id.regEmail);

        Drawable drawable = getResources().getDrawable(R.drawable.user);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * 0.75),
                (int) (drawable.getIntrinsicHeight() * 0.75));
        ScaleDrawable sd = new ScaleDrawable(drawable, 0, -1, -1);
        regUser.setCompoundDrawables(sd.getDrawable(), null, null, null);

        drawable = getResources().getDrawable(R.drawable.decrypt);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * 0.75),
                (int) (drawable.getIntrinsicHeight() * 0.75));
        sd = new ScaleDrawable(drawable, 0, -1, -1);
        regpass.setCompoundDrawables(sd.getDrawable(), null, null, null);

        drawable = getResources().getDrawable(R.drawable.fullname);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * 0.75),
                (int) (drawable.getIntrinsicHeight() * 0.75));
        sd = new ScaleDrawable(drawable, 0, -1, -1);
        regname.setCompoundDrawables(sd.getDrawable(), null, null, null);

        drawable = getResources().getDrawable(R.drawable.mail);
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * 0.75),
                (int) (drawable.getIntrinsicHeight() * 0.75));
        sd = new ScaleDrawable(drawable, 0, -1, -1);
        regemail.setCompoundDrawables(sd.getDrawable(), null, null, null);

        registerbutton = (Button) findViewById(R.id.btnRegister);
        registerbutton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                struser = regUser.getText().toString().trim();
                strpass = regpass.getText().toString().trim();
                strname = regname.getText().toString().trim();
                stremail = regemail.getText().toString().trim();

                if (isValidUsername(struser) && isValidPassword(strpass) && isValidFullName(strname)
                        && !struser.contains("admin") && isValidEmail(stremail)) {
                    try {
                        register();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(),
                                "Error registering in", Toast.LENGTH_LONG)
                                .show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Enter valid details to register. Check",
                            Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alert = new AlertDialog.Builder(RegisterActivity.this);
                    alert.setTitle("Invalid details");
                    alert.setMessage("Username(3-15): a-z 0-9 _ -" +
                            "\n\nPassword(6-20): Atleast one small letter, one capital, one number, one symbol from @#$%^&+=");
                    alert.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alert.show();
                }
            }
        });

        loginbn = (Button) findViewById(R.id.btnbacktologin);
        loginbn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });


        TextView togglePass = (TextView) findViewById(R.id.regTogglePassword);
        togglePass.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passwordShown) {
                    regpass.setInputType(129);
                    passwordShown = false;
                } else {
                    regpass.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordShown = true;
                }
            }
        });
    }

    public void register() {
        final ProgressDialog dialog = ProgressDialog.show(this,
                "Registering...", "Please wait...", false);

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    // Create a connection
                    XMPPTCPConnectionConfiguration connConfig = XMPPTCPConnectionConfiguration
                            .builder().setHost(HOST).setPort(PORT)
                            .setUsernameAndPassword("admin", "pa55w0rd!")
                            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                            .setServiceName(HOST).build();
                    AbstractXMPPConnection connection = new XMPPTCPConnection(connConfig);
                    connection.connect();
                    connection.login();
                    Presence presence = new Presence(Presence.Type.available);
                    connection.sendStanza(presence);
                    setconnecction(connection);
                    Roster roster = Roster.getInstanceFor(connection);
                    roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

                    try {
                        Map<String, String> newRegAttrMap = new HashMap<String, String>();
                        newRegAttrMap.put("name", strname);
                        newRegAttrMap.put("email", stremail);
                        AccountManager accountManager = AccountManager
                                .getInstance(connection);
                        accountManager.sensitiveOperationOverInsecureConnection(true);
                        if (accountManager.supportsAccountCreation()) {
                            accountManager.createAccount(struser, strpass,
                                    newRegAttrMap);

                            // Adding user to admin roster
                            try {
                                Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);

                                String jid = struser + "@" + HOST + "/Smack";
                                Roster.getInstanceFor(connection).createEntry(
                                        jid, jid, null);
                                Presence pres = new Presence(
                                        Presence.Type.subscribe);
                                pres.setTo(jid);
                                connection.sendStanza(pres);
                            } catch (Exception e) {
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),
                                                "Error adding user to admin",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                                Log.e("Register", e.getLocalizedMessage());
                            }

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(RegisterActivity.this);
                                    alert.setTitle("Registered!");
                                    alert.setMessage(struser + " has been registered successfully.\nNow you can login");
                                    alert.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    alert.show();
                                }
                            });
                            regUser.setText("");
                            regpass.setText("");
                            regname.setText("");
                            regemail.setText("");
                            conn.disconnect();
                        } else {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(),
                                            "Registration not supported",
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                        }

                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Incorrect details to register. Try Again",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        Log.e("Register", e.getLocalizedMessage());
                    }
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Incorrect details to register. Try Again",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.e("Register", e.getLocalizedMessage());
                }

                dialog.dismiss();
            }
        });
        t.start();
        dialog.show();
    }

    void setconnecction(AbstractXMPPConnection con) {
        this.conn = con;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (conn != null)
                conn.disconnect();
        } catch (Exception e) {
            Log.e("Reg/OnDestroy", e.getLocalizedMessage());
        }
    }

    @Override
    protected void onPause() {
        regUser.setText("");
        regpass.setText("");
        regname.setText("");
        regemail.setText("");
        try {
            if (conn != null)
                conn.disconnect();
        } catch (Exception e) {
            Log.e("Reg/OnPause", e.getLocalizedMessage());
        }
        super.onPause();
    }

    public void onBackPressed() {
        finish();
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void setupUI(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(RegisterActivity.this);
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

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static boolean isValidUsername(CharSequence username) {
        return !TextUtils.isEmpty(username) && Pattern.compile("^[a-z0-9_-]{3,15}$").matcher(username).matches();
    }

    public static boolean isValidFullName(CharSequence username) {
        return !TextUtils.isEmpty(username) && Pattern.compile("^[\\p{L} .'-]+$").matcher(username).matches();
    }

    public static boolean isValidPassword(CharSequence username) {
        return !TextUtils.isEmpty(username) && Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[.@#$%^&+=])(?=\\S+$).{6,20}$").matcher(username).matches();
    }
}
