package com.pbsystems.meow.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.pbsystems.meow.R;
import com.pbsystems.meow.adapter.FriendsAdapter;
import com.pbsystems.meow.adapter.MessagessAdapter;
import com.pbsystems.meow.adapter.MyPagerAdapter;
import com.pbsystems.meow.app.MyApplication;
import com.pbsystems.meow.data.CMessage;
import com.pbsystems.meow.data.FriendInfo;
import com.pbsystems.meow.database.ChatDatabaseManager;
import com.pbsystems.meow.fragment.ChatFragment;
import com.pbsystems.meow.fragment.FriendsFragment;
import com.pbsystems.meow.service.ChatService;
import com.pbsystems.meow.utils.OnSwipeTouchListener;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jivesoftware.smackx.xdata.Form;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ChatActivity extends FragmentActivity implements View.OnClickListener, FriendsFragment.OnFriendsListUpdateListener {
    private AbstractXMPPConnection connection;
    private ArrayList<CMessage> messages = new ArrayList<CMessage>();
    MyReciever myRecieve;
    IntentFilter filter;

    private Handler mHandler = new Handler();

    ChatDatabaseManager database;
    private EditText textMessage;
    private ListView listview;
    private LinearLayout chatLayout;

    ArrayList<RosterEntry> aryRoster = new ArrayList<RosterEntry>();
    ArrayList<String> usersList = new ArrayList<String>();
    ArrayList<FriendInfo> allfriends = new ArrayList<FriendInfo>();

    boolean chatSecured = false;

    public String selectedFrnd = "";
    public Bitmap selectedFrndPic = null;
    public String selectedFile = "";
    String userid;

    private static final int FILE_SELECT_CODE = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    VCard userVcard;

    FriendsAdapter frndarrayAdapter;
    String frndUserid;
    Bitmap frndProfileBitmap;

    TextView tvTitle, tvTitleIcon;

    TextView btnChatMenu, btnCallUser, btnSendPic, btnAddFriend, btnUserProfile, btnEncrypt, btnGroupChat, btnSendFile, btnClearChat, btnLogout;
    FrameLayout chatOptionsView;
    LinearLayout mainView;
    Animation animSlideOutRight, animSlideInRight;
    int slide = 0;

    MyPagerAdapter pagerAdapter;
    ViewPager pager;
    LinearLayout sendLayout;

    int screenWidth;

    File mainfolder, sentImageFolder, photoFile;

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(myRecieve);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("Chat_User",
                MODE_PRIVATE);
        if (prefs.getInt("bg_red", 300) > 255) {
            chatLayout.setBackground(getResources().getDrawable(
                    R.drawable.chat_background));
        } else {
            chatLayout.setBackgroundColor(Color.rgb(prefs.getInt("bg_red", 0),
                    prefs.getInt("bg_green", 0), prefs.getInt("bg_blue", 0)));
        }

        myRecieve = new MyReciever();
        filter = new IntentFilter();
        filter.addAction("chatMessage");
        filter.addAction("StatusChange");
        filter.addAction("fileSent");
        filter.addAction("NetDisc");
        filter.addAction("SipProfileStatus");
        filter.addAction("SipCallStatus");

        userVcard = null;

        registerReceiver(myRecieve, filter);
        // doBindService();

        MyApplication app = (MyApplication) getApplication();
        if (app.getConnection() == null) {
            Intent serviceIntent = new Intent(getBaseContext(),
                    ChatService.class);
            serviceIntent.putExtra("reopen", "setConn");
            startService(serviceIntent);
        }

        this.connection = app.getConnection();

        chatSecured = app.isSecureChat();

        try {
            if (connection.getUser() == null) {
                if (isMyServiceRunning()) {
                    unregisterReceiver(myRecieve);
                    stopService(new Intent(getBaseContext(), ChatService.class));
                    app = (MyApplication) getApplication();
                    app.setConnection(null);
                    connection = null;
                }
                finish();
            } else {
                userid = connection.getUser().substring(0,
                        connection.getUser().indexOf("@"));
            }
        } catch (Exception e) {
            if (isMyServiceRunning()) {
                unregisterReceiver(myRecieve);
                stopService(new Intent(getBaseContext(), ChatService.class));
                app = (MyApplication) getApplication();
                app.setConnection(null);
                connection = null;
            }
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setupUI(findViewById(R.id.chat_layout));

        sendLayout = (LinearLayout) findViewById(R.id.linearLayout2);
        sendLayout.setVisibility(View.GONE);
        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pagerAdapter.notifyDataSetChanged();


        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenWidth = displaymetrics.widthPixels;

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setBackgroundColor(Color.argb(40, 0, 0, 0));
        tabs.setDividerColor(Color.TRANSPARENT);
        tabs.setIndicatorHeight(screenWidth / 100);
        tabs.setAllCaps(false);
        tabs.setIndicatorColor(Color.WHITE);
        tabs.setTextColor(Color.WHITE);
        tabs.setTextSize(screenWidth / 22);
        tabs.setShouldExpand(true);
        tabs.setViewPager(pager);

        textMessage = (EditText) this.findViewById(R.id.chatET);
        chatLayout = (LinearLayout) findViewById(R.id.chat_layout);
        listview = (ListView) this.findViewById(R.id.listMessages);

        tvTitle = (TextView) this.findViewById(R.id.chat_title);
        tvTitleIcon = (TextView) this.findViewById(R.id.chat_title_icon);

        MyApplication app = (MyApplication) getApplication();
        if (app.getConnection() == null) {
            Intent serviceIntent = new Intent(getBaseContext(),
                    ChatService.class);
            serviceIntent.putExtra("reopen", "setConn");
            startService(serviceIntent);
        }

        this.connection = app.getConnection();

        selectedFrnd = "";
        userid = connection.getUser().substring(0, connection.getUser().indexOf("@"));
        database = new ChatDatabaseManager(ChatActivity.this, userid);


        listview.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();

                int red = randomNumber(0, 255);
                int green = randomNumber(0, 255);
                int blue = randomNumber(0, 255);

                SharedPreferences.Editor editor = getSharedPreferences(
                        "Chat_User", MODE_PRIVATE).edit();
                editor.putInt("bg_red", red);
                editor.putInt("bg_green", green);
                editor.putInt("bg_blue", blue);
                editor.apply();
                chatLayout.setBackgroundColor(Color.rgb(red, green, blue));
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                SharedPreferences.Editor editor = getSharedPreferences(
                        "Chat_User", MODE_PRIVATE).edit();
                editor.remove("bg_red");
                editor.remove("bg_green");
                editor.remove("bg_blue");
                editor.apply();
                chatLayout.setBackground(getResources().getDrawable(
                        R.drawable.chat_background));
            }
        });

        Intent serviceIntent = new Intent(getBaseContext(), ChatService.class);
        serviceIntent.putExtra("SetupAddfriend", "Setting");
        startService(serviceIntent);

        try {
            connectandgetlist();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error retrieving data",
                    Toast.LENGTH_SHORT).show();
            finish();
            stopService(new Intent(getBaseContext(), ChatService.class));
        }

        final ImageButton send = (ImageButton) this.findViewById(R.id.sendBtn);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Using chat
                String text = textMessage.getText().toString().trim();
                if (text.length() > 0) {
                    if (connection != null) {
                        try {
                            if (selectedFrnd.length() > 3
                                    && usersList.contains(selectedFrnd)) {
                                Message msg = new Message(selectedFrnd + "@"
                                        + connection.getHost() + "/Smack",
                                        Type.chat);
                                msg.setBody(text);
                                long recid = System.currentTimeMillis();
                                msg.setStanzaId("" + recid);
                                connection.sendStanza(msg);

                                messages.add(new CMessage(text, false));

                                if (database.insertMessage(selectedFrnd, "me", text, recid)) {
                                    textMessage.setText("");
                                    Log.i("Database",
                                            "Message sent is inserted into database");
                                } else {
                                    Log.e("Database error",
                                            "Message sent is not able to insert into database");
                                }
                                setListAdapter();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Cannot send message",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "Error sending message", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }
            }
        });

        setTitle(selectedFrnd);

        File mf = Environment.getExternalStorageDirectory();
        mainfolder = new File(mf.getAbsoluteFile() + "/Meow/");
        sentImageFolder = new File(mainfolder.getAbsoluteFile() + "/Sent/");

        if (!mainfolder.exists()) {
            mainfolder.mkdir();
        }

        if (!sentImageFolder.exists()) {
            sentImageFolder.mkdir();
        }

        loadViews();
    }

    void loadViews() {
        chatOptionsView = (FrameLayout) this.findViewById(R.id.chat_options);
        mainView = (LinearLayout) this.findViewById(R.id.mainView);
        chatOptionsView.setVisibility(View.GONE);
        animSlideOutRight = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_right);
        animSlideInRight = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right);

        animSlideOutRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                chatOptionsView.setVisibility(View.GONE);
                mainView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        btnChatMenu = (TextView) this.findViewById(R.id.btnChatMenu);
        btnCallUser = (TextView) this.findViewById(R.id.btnCallUser);
        btnAddFriend = (TextView) this.findViewById(R.id.btnAddFriend);
        btnUserProfile = (TextView) this.findViewById(R.id.btnUserProfile);
        btnSendFile = (TextView) this.findViewById(R.id.btnSendFile);
        btnEncrypt = (TextView) this.findViewById(R.id.btnEncrypt);
        btnGroupChat = (TextView) this.findViewById(R.id.btnGroupChat);
        btnLogout = (TextView) this.findViewById(R.id.btnLogout);
        btnSendPic = (TextView) this.findViewById(R.id.btnSendPic);
        btnClearChat = (TextView) this.findViewById(R.id.btnClearChat);

        btnAddFriend.setOnClickListener(this);
        btnChatMenu.setOnClickListener(this);
        btnSendPic.setOnClickListener(this);
        btnCallUser.setOnClickListener(this);
        btnUserProfile.setOnClickListener(this);
        btnSendFile.setOnClickListener(this);
        btnEncrypt.setOnClickListener(this);
        btnGroupChat.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnClearChat.setOnClickListener(this);
        mainView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.mainView:
                if (chatOptionsView.getVisibility() == View.VISIBLE) {
                    chatOptionsView.setVisibility(View.GONE);
                    mainView.setVisibility(View.GONE);
                }
                chatOptionsView.startAnimation(animSlideOutRight);
                slide = 0;
                break;

            case R.id.btnSendPic:
                try {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Ensure that there's a camera activity to handle the intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        photoFile = null;
                        try {
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                            String imageFileName = "JPEG_" + timeStamp + "_";
                            photoFile = File.createTempFile(imageFileName, ".jpg", sentImageFolder);
                        } catch (Exception ex) {
                            // Error occurred while creating the File
                            Log.i("Camera", "Error while creating file, " + ex.getLocalizedMessage());
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                    }
                } catch (Exception e) {
                    Log.i("Camera", "Cannot capture image");
                }
                break;

            case R.id.btnChatMenu:
                if (chatOptionsView.getVisibility() == View.VISIBLE) {
                    chatOptionsView.setVisibility(View.GONE);
                    mainView.setVisibility(View.GONE);
                }
                if (slide == 0) {
                    chatOptionsView.setVisibility(View.VISIBLE);
                    chatOptionsView.startAnimation(animSlideInRight);

                    mainView.setVisibility(View.VISIBLE);
                    slide++;
                } else {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                break;

            case R.id.btnCallUser:
                //Jingle call implement
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                openFriendsList();

                break;

            case R.id.btnAddFriend:
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                openAddUserDialog();
                break;

            case R.id.btnUserProfile:
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                startActivity(new Intent(ChatActivity.this, ProfileActivity.class));
                break;

            case R.id.btnSendFile:
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                if (selectedFrnd.length() > 3) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    try {
                        startActivityForResult(Intent.createChooser(intent,
                                "Select a File to Upload"), FILE_SELECT_CODE);
                    } catch (Exception ex) {
                        // Potentially direct the user to the Market with a Dialog
                        Toast.makeText(this, "Please install a File Manager.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Friend not selected",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnEncrypt:
                if (chatSecured) {
                    btnEncrypt.setText("Encrypt");
                    btnEncrypt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.encrypt, 0, 0, 0);
                    chatSecured = false;
                } else {
                    btnEncrypt.setText("Decrypt");
                    btnEncrypt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.decrypt, 0, 0, 0);
                    chatSecured = true;
                }
                MyApplication p = (MyApplication) getApplication();
                p.setSecureChat(chatSecured);
                setListAdapter();
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                break;

            case R.id.btnGroupChat:
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                startActivity(new Intent(ChatActivity.this, GroupChatActivity.class));
                break;

            case R.id.btnLogout:

                if (isMyServiceRunning()) {
                    unregisterReceiver(myRecieve);
                    stopService(new Intent(getBaseContext(), ChatService.class));
                    MyApplication app = (MyApplication) getApplication();
                    app.setConnection(null);
                    connection = null;
                }
                finish();
                break;

            case R.id.btnClearChat:
                if (slide != 0) {
                    chatOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                try {
                    File databaseFile = ChatActivity.this.getDatabasePath(userid + "_pbs.sql").getAbsoluteFile();
                    databaseFile.delete();
                    userid = connection.getUser().substring(0, connection.getUser().indexOf("@"));
                    database = new ChatDatabaseManager(ChatActivity.this, userid);
                    setListAdapter();
                } catch (Exception e) {
                    Log.i("DeleteDatabase", "Cannot clear chats");
                }
                break;

//            case R.id.uploadDb:
//                String dbname = userid + "_pbs.sql";
//                String sourceFilename = ChatActivity.this.getDatabasePath(dbname)
//                        .getAbsolutePath();
//
//                File src = new File(sourceFilename);
//                File dest = new File(Environment.getExternalStorageDirectory()
//                        + "/Meow/" + dbname);
//                try {
//                    copy(src, dest);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                break;

            default:

                break;
        }
    }

    void openFriendsList() {
        final Dialog dialog = new Dialog(ChatActivity.this);

        dialog.setContentView(R.layout.address_list);
        dialog.setTitle("Select to call");

        ListView add_list = (ListView) dialog.findViewById(R.id.add_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                usersList);
        add_list.setAdapter(adapter);

        add_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sipAddress = "sip:" + usersList.get(position) + "@officesip.meow";
                Intent serviceIntent = new Intent(getBaseContext(), ChatService.class);
                serviceIntent.putExtra("SipCall", sipAddress);
                startService(serviceIntent);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    void settingupchat() {
        messages = new ArrayList<CMessage>();

        if (selectedFrnd.length() > 3) {
            try {
                if (database.getmessagesofFriend(selectedFrnd) != null) {
                    messages = database.getmessagesofFriend(selectedFrnd);
                }
            } catch (Exception e) {
                Log.e("Databse Error",
                        "Error retrieving messages from database");
            }

            Intent serviceIntent = new Intent(getBaseContext(),
                    ChatService.class);
            serviceIntent.putExtra("FriendName", selectedFrnd + "@"
                    + connection.getHost() + "/Smack");
            startService(serviceIntent);
        }
        setListAdapter();
    }

    void connectandgetlist() throws Exception {
        aryRoster = new ArrayList<RosterEntry>();
        usersList = new ArrayList<String>();
        allfriends = new ArrayList<FriendInfo>();

        Roster roster = Roster.getInstanceFor(connection);
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {

            Log.d("XMPPChatDemoActivity",
                    "--------------------------------------");
            Log.d("XMPPChatDemoActivity", "RosterEntry " + entry);
            Log.d("XMPPChatDemoActivity", "User: " + entry.getUser());
            Log.d("XMPPChatDemoActivity", "Name: " + entry.getName());
            Log.d("XMPPChatDemoActivity", "Status: " + entry.getStatus());
            Log.d("XMPPChatDemoActivity", "Type: " + entry.getType());
            Presence entryPresence = roster.getPresence(entry.getUser());

            Log.d("XMPPChatDemoActivity",
                    "Presence Status: " + entryPresence.getStatus());
            Log.d("XMPPChatDemoActivity",
                    "Presence Type: " + entryPresence.getType());

            Presence.Type type = entryPresence.getType();
            if (type == Presence.Type.available
                    && !entry.getUser().contains("admin")) {
                aryRoster.add(entry);
                usersList.add(entry.getUser().substring(0,
                        entry.getUser().indexOf("@")));
                Log.d("XMPPChatDemoActivity", "Presence AVIALABLE");
            }

            if (!entry.getUser().contains("admin")) {
                frndUserid = entry.getUser().substring(0, entry.getUser().indexOf("@"));
                frndProfileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user);
                Bitmap img = getUserImage(frndUserid);
                if (!ifFriendExits(frndUserid)) {
                    HashMap<String, String> details = getFriendDetails(frndUserid);
                    String name = details.get("name");
                    String email = details.get("email");
                    allfriends.add(new FriendInfo(frndUserid, (img == null) ? frndProfileBitmap : img, usersList.contains(frndUserid), name, email));
                }
            }
            Log.d("XMPPChatDemoActivity", "Presence : " + entryPresence);

            if (!selectedFrnd.equals("") && ifFriendExits(selectedFrnd) && !usersList.contains(selectedFrnd)) {
                Toast.makeText(ChatActivity.this,
                        selectedFrnd + " went offline. Cannot chat",
                        Toast.LENGTH_LONG).show();
                setSelectedFrnd("");
            }
        }
        Intent serviceIntent = new Intent(getBaseContext(),
                ChatService.class);
        serviceIntent.putExtra("FriendName", "@"
                + connection.getHost() + "/Meow");
        startService(serviceIntent);
    }

    public String getSelectedFrnd() {
        return selectedFrnd;
    }

    public void setSelectedFrnd(int selectedid) {
        if (usersList.contains(allfriends.get(selectedid).userid)) {
            selectedFrnd = allfriends.get(selectedid).userid;
            pager.setCurrentItem(1, true);
            setTitle(selectedFrnd);
            Toast.makeText(
                    ChatActivity.this,
                    selectedFrnd + " is online. Start Chatting",
                    Toast.LENGTH_SHORT).show();
            settingupchat();
        } else {
            Toast.makeText(
                    ChatActivity.this,
                    allfriends.get(selectedid).userid + " is offline. Cannot Chat",
                    Toast.LENGTH_SHORT).show();
            selectedFrnd = "";
            setTitle("Meow!");
            settingupchat();
        }
    }

    public void setSelectedFrnd(String selected) {
        if (!selected.equals("") && usersList.contains(selected)) {
            selectedFrnd = selected;
            pager.setCurrentItem(1, true);
            setTitle(selectedFrnd);
            Toast.makeText(
                    ChatActivity.this,
                    selectedFrnd + " is online. Start Chatting",
                    Toast.LENGTH_SHORT).show();
            settingupchat();
        } else {
            pager.setCurrentItem(0, true);
            if (!selected.equals("")) {
                Toast.makeText(
                        ChatActivity.this,
                        selected + " is offline. Cannot Chat",
                        Toast.LENGTH_SHORT).show();
            }
            selectedFrnd = "";
            setTitle("Meow!");
            settingupchat();
        }
    }

    @Override
    public void onFriendsUpdate(ListView lv) {
        frndarrayAdapter = new FriendsAdapter(ChatActivity.this, allfriends);
        frndarrayAdapter.notifyDataSetChanged();
        lv.setAdapter(frndarrayAdapter);
    }

    void setTitle(String frndid) {
        try {
            if (frndid.length() > 1) {
                tvTitle.setText(frndid);
                selectedFrndPic = getUserImage(frndid);
                Bitmap defaultpic = BitmapFactory.decodeResource(getResources(), R.drawable.user);
                Resources res = getResources();
                BitmapDrawable icon = new BitmapDrawable(res, (selectedFrndPic == null) ? defaultpic : selectedFrndPic);
                tvTitleIcon.setBackground(icon);
            } else {
                tvTitle.setText("Meow!");
                Resources res = getResources();
                Bitmap defaultpic = BitmapFactory.decodeResource(getResources(), R.drawable.user);
                BitmapDrawable icon = new BitmapDrawable(res, defaultpic);
                tvTitleIcon.setBackground(icon);
            }
        } catch (Exception e) {
            Log.i("ActionBar", "Cannot set actionbar");
        }

    }

    public Bitmap getUserImage(String user) {
        Bitmap bmp = null;
        VCard vcard;
        try {
            ProviderManager.addIQProvider("vCard", "vcard-temp", new VCardProvider());
            VCardManager manager = VCardManager.getInstanceFor(connection);
            vcard = manager.loadVCard(user + "@" + connection.getHost());

            if (vcard == null || vcard.getAvatar() == null) {
                return null;
            }
            bmp = BitmapFactory.decodeByteArray(vcard.getAvatar(), 0, vcard.getAvatar().length);
            Log.i("Vcard", "Image of " + user + " is loaded");
        } catch (Exception e) {
            Log.i("VCard", "Cannot load profile image of " + user + " : " + e.getLocalizedMessage());
        }
        return bmp;
    }


    boolean ifFriendExits(String id) {
        for (FriendInfo f : allfriends) {
            if (f.userid.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public void setListAdapter() {

        ArrayList<CMessage> newMsgs = new ArrayList<CMessage>();

        if (chatSecured) {
            for (CMessage cm : messages) {
                CMessage newcm = new CMessage(encodeMessage(cm.text), cm.isLeft);
                newMsgs.add(newcm);
            }
        } else {
            newMsgs = messages;
        }

        if (pager.getCurrentItem() == 1) {
            final MessagessAdapter adapter = new MessagessAdapter(this, newMsgs);
            ChatFragment frag = (ChatFragment) getSupportFragmentManager().getFragments().get(1);
            frag.updateChatListView(adapter);
        }
    }

    public void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();

                    if (uri.toString().length() > 3) {
                        selectedFile = getRealPathFromURI(uri);

                        if (connection != null && selectedFrnd.length() > 2) {
                            Intent serviceIntent = new Intent(getBaseContext(),
                                    ChatService.class);
                            serviceIntent.putExtra("sentFile", selectedFile);
                            serviceIntent.putExtra("Receiver", selectedFrnd);
                            startService(serviceIntent);
                        }
                    }
                }
                break;

            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    try {
                        if (connection != null && selectedFrnd.length() > 2 && photoFile != null) {
                            selectedFile = photoFile.getAbsolutePath();
                            Intent serviceIntent = new Intent(getBaseContext(),
                                    ChatService.class);
                            serviceIntent.putExtra("sentFile", selectedFile);
                            serviceIntent.putExtra("Receiver", selectedFrnd);
                            startService(serviceIntent);
                        }
                    } catch (Exception e) {
                        Log.i("Camera", e.getLocalizedMessage());
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null,
                null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor
                    .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    public Boolean checkIfUserExists(String user) throws Exception {
        UserSearchManager search = new UserSearchManager(connection);
        Form searchForm = search.getSearchForm("search." + connection.getServiceName());
        Form answerForm = searchForm.createAnswerForm();
        answerForm.setAnswer("Username", true);
        answerForm.setAnswer("search", user);
        ReportedData data = search.getSearchResults(answerForm, "search." + connection.getServiceName());
        if (data.getRows() != null) {
            List<ReportedData.Row> it = data.getRows();
            for (ReportedData.Row row : it) {
                List<String> iterator = row.getValues("jid");
                if (iterator.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public HashMap<String, String> getFriendDetails(String userid) {
        HashMap<String, String> details = new HashMap<String, String>();
        try {
            UserSearchManager search = new UserSearchManager(connection);
            Form searchForm = search.getSearchForm("search." + connection.getServiceName());
            Form answerForm = searchForm.createAnswerForm();
            answerForm.setAnswer("Username", true);
            answerForm.setAnswer("search", userid);
            ReportedData data = search.getSearchResults(answerForm, "search." + connection.getServiceName());
            if (data.getRows() != null) {
                List<ReportedData.Row> it = data.getRows();
                for (ReportedData.Row row : it) {
                    List<String> names = row.getValues("Name");
                    List<String> emails = row.getValues("Email");
                    details.put("name", names.get(0));
                    details.put("email", emails.get(0));
                    return details;
                }
            }
        } catch (Exception e) {
            Log.e("Friends Details", "Error getting details - " + e.getLocalizedMessage());
        }
        details = new HashMap<String, String>();
        details.put("name", userid);
        details.put("email", "-");
        return details;
    }

    void openAddUserDialog() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Add Friend");
        alertDialog.setMessage("Enter friend's username");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Add",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        ProgressDialog dlg = ProgressDialog.show(
                                ChatActivity.this, "Connecting...",
                                "Please wait...", false);
                        dlg.show();

                        String usertoadd = input.getText().toString().trim();
                        try {
                            if (checkIfUserExists(usertoadd)) {
                                try {
                                    Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);

                                    String jid = usertoadd + "@" + connection.getHost()
                                            + "/Smack";
                                    Roster.getInstanceFor(connection).createEntry(jid,
                                            jid, null);
                                    Presence pres = new Presence(
                                            Presence.Type.subscribe);
                                    pres.setTo(jid);
                                    connection.sendStanza(pres);

                                    try {
                                        connectandgetlist();
                                        FriendsFragment ff = (FriendsFragment) ChatActivity.this.getSupportFragmentManager().getFragments().get(0);
                                        ff.updateFriendsList();
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(),
                                                "Error retrieving data",
                                                Toast.LENGTH_SHORT).show();
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e1) {
                                            e1.printStackTrace();
                                        }
                                        startActivity(new Intent(ChatActivity.this,
                                                LoginActivity.class));
                                        e.printStackTrace();
                                    }

                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(),
                                            "User cannot be added", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        usertoadd + " does not exist", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        dlg.dismiss();
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
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
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    private class MyReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("chatMessage")) {
                if (selectedFrnd == null || selectedFrnd.equals("")) {
                    setSelectedFrnd(intent.getStringExtra("friend"));
                }
                messages.add(new CMessage(intent.getStringExtra("message"), true));

                // Add the incoming message to the list view
                mHandler.post(new Runnable() {
                    public void run() {
                        setListAdapter();
                    }
                });
            }

            if (intent.getAction().equals("fileSent")) {
                messages.add(new CMessage(intent.getStringExtra("message"), false));

                // Add the file sent message to the list view
                mHandler.post(new Runnable() {
                    public void run() {
                        setListAdapter();
                    }
                });
            }

            if (intent.getAction().equals("StatusChange")) {
                try {
                    connectandgetlist();
                    FriendsFragment ff = (FriendsFragment) ChatActivity.this.getSupportFragmentManager().getFragments().get(0);
                    ff.updateFriendsList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (intent.getAction().equals("NetDisc")) {
                finish();
            }

            if (intent.getAction().equals("SipProfileStatus")) {
                switch (intent.getIntExtra("status", 999)) {
                    case 0:
                        btnCallUser.setClickable(false);
                        break;

                    case 1:
                        btnCallUser.setClickable(true);
                        break;

                    case 2:
                        btnCallUser.setClickable(false);
                        break;

                    case 3:
                        btnCallUser.setClickable(true);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    String encodeMessage(String decoded) {
        ArrayList<Character> chars = new ArrayList<Character>();
        char[] msg = decoded.toCharArray();
        for (int i = 0; i < msg.length; i++) {
            chars.add(msg[msg.length - i - 1]);
        }
        Character[] cs = chars.toArray(new Character[chars.size()]);

        String str = "";
        for (Character c : cs)
            str += c.toString();
        return str;
    }

    int randomNumber(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        super.onDestroy();
    }

    public void setupUI(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(ChatActivity.this);
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

    public void openProfileOfFriend(int position) {
        Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.friendinfo_dialog);
        ImageView imgv = (ImageView) dlg.findViewById(R.id.frnd_pic);
        TextView tvname = (TextView) dlg.findViewById(R.id.frnd_name);
        TextView tvemail = (TextView) dlg.findViewById(R.id.frnd_email);

        try {
            FriendInfo sel = allfriends.get(position);
            dlg.setTitle(sel.userid);
            imgv.setImageBitmap(sel.image);
            tvname.setText(sel.name);
            tvemail.setText(sel.email);
            dlg.show();
        } catch (Exception e) {
            Log.e("Friend profile", "Error loading friends profile");
        }
    }
}
