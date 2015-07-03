package com.pbsystems.meow.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.pbsystems.meow.R;
import com.pbsystems.meow.adapter.GroupMessageAdapter;
import com.pbsystems.meow.adapter.GroupPagerAdapter;
import com.pbsystems.meow.app.MyApplication;
import com.pbsystems.meow.data.GroupMessage;
import com.pbsystems.meow.fragment.GroupChatFragment;
import com.pbsystems.meow.fragment.GroupsFragment;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.ArrayList;
import java.util.Locale;

public class GroupChatActivity extends FragmentActivity implements MessageListener, View.OnClickListener {

    AbstractXMPPConnection connection = null;
    EditText textMessage;
    ListView listview;
    ImageButton send;

    MultiUserChat muc;

    ArrayList<GroupMessage> msgs = new ArrayList<GroupMessage>();
    String nickname;

    Handler mhandler = new Handler();

    String roomname = "allusers";

    TextView btnCreateGroup, btnJoinGroup, btnCloseGroupChat, btnGroupOprions, tvtitle;
    FrameLayout groupOptionsView;
    Animation animSlideOutRight, animSlideInRight;
    LinearLayout mainView;
    int slide = 0;


    ViewPager groupPager;
    GroupPagerAdapter groupPagerAdapter;

    int screenWidth = 0;


    @Override
    protected void onPause() {
        try {
            muc.leave();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LinearLayout chatLayout = (LinearLayout) findViewById(R.id.chat_layout1);

        SharedPreferences prefs = getSharedPreferences("Chat_User",
                MODE_PRIVATE);
        if (prefs.getInt("bg_red", 300) > 255) {
            chatLayout.setBackground(getResources().getDrawable(
                    R.drawable.chat_background));
        } else {
            chatLayout.setBackgroundColor(Color.rgb(prefs.getInt("bg_red", 0),
                    prefs.getInt("bg_green", 0), prefs.getInt("bg_blue", 0)));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groupchat);

        setupUI(findViewById(R.id.chat_layout1));

        groupPager = (ViewPager) findViewById(R.id.grouppager);
        groupPagerAdapter = new GroupPagerAdapter(getSupportFragmentManager());
        groupPager.setAdapter(groupPagerAdapter);
        groupPagerAdapter.notifyDataSetChanged();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenWidth = displaymetrics.widthPixels;

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.grouptabs);
        tabs.setBackgroundColor(Color.argb(40, 0, 0, 0));
        tabs.setDividerColor(Color.TRANSPARENT);
        tabs.setIndicatorHeight(screenWidth / 100);
        tabs.setAllCaps(false);
        tabs.setIndicatorColor(Color.WHITE);
        tabs.setTextColor(Color.WHITE);
        tabs.setTextSize(screenWidth / 22);
        tabs.setShouldExpand(true);
        tabs.setViewPager(groupPager);

        textMessage = (EditText) this.findViewById(R.id.chatET1);
        listview = (ListView) this.findViewById(R.id.listMessages1);
        send = (ImageButton) this.findViewById(R.id.sendBtn12);

        msgs = new ArrayList<GroupMessage>();

        try {
            Class.forName(org.jivesoftware.smackx.muc.MultiUserChat.class.getName(),
                    true, this.getClassLoader());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        MyApplication app = (MyApplication) getApplication();
        connection = app.getConnection();

        try {
            nickname = connection.getUser().substring(0,
                    connection.getUser().indexOf("@"));
        } catch (Exception e) {
            Toast.makeText(GroupChatActivity.this,
                    "Cannot get user from connection", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        loadViews();

        try {
            MultiUserChatManager manager = MultiUserChatManager
                    .getInstanceFor(connection);
            muc = manager.getMultiUserChat(roomname + "@conference." + connection.getHost());
            muc.join(nickname);
            String title = muc.getRoom().toUpperCase(Locale.ENGLISH).substring(0, muc.getRoom().toUpperCase(Locale.ENGLISH).indexOf("@"));
            tvtitle.setText(title);

            send.setOnClickListener(this);
            muc.addMessageListener(this);
        } catch (Exception e) {
            Toast.makeText(GroupChatActivity.this,
                    "Cannot join room", Toast.LENGTH_SHORT)
                    .show();
            tvtitle.setText("Join/Create a room");
        }
    }

    void loadViews() {
        groupOptionsView = (FrameLayout) findViewById(R.id.group_options);
        groupOptionsView.setVisibility(View.GONE);

        mainView = (LinearLayout) findViewById(R.id.groupMainView);

        animSlideOutRight = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_right);
        animSlideInRight = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right);

        animSlideOutRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                groupOptionsView.setVisibility(View.GONE);
                mainView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        btnGroupOprions = (TextView) findViewById(R.id.btnGroupOptions);
        btnJoinGroup = (TextView) findViewById(R.id.btnJoinGroup);
        btnCreateGroup = (TextView) findViewById(R.id.btnCreateGroup);
        btnCloseGroupChat = (TextView) findViewById(R.id.btnCloseGroupCHat);
        tvtitle = (TextView) findViewById(R.id.group_title);

        btnCreateGroup.setOnClickListener(this);
        btnJoinGroup.setOnClickListener(this);
        btnCloseGroupChat.setOnClickListener(this);
        btnGroupOprions.setOnClickListener(this);
        mainView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.groupMainView:
                if (groupOptionsView.getVisibility() == View.VISIBLE) {
                    groupOptionsView.setVisibility(View.GONE);
                    mainView.setVisibility(View.GONE);
                }
                groupOptionsView.startAnimation(animSlideOutRight);
                slide = 0;
                break;

            case R.id.sendBtn12:
                if (slide != 0) {
                    groupOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                String text = textMessage.getText().toString().trim();
                if (text.length() > 0) {
                    if (connection != null) {
                        try {
                            Message msg = new Message(roomname + "@conference."
                                    + connection.getHost(),
                                    Message.Type.groupchat);
                            msg.setBody(text);
                            long recid = System.currentTimeMillis();
                            msg.setStanzaId("" + recid);
                            msg.setSubject(nickname);
                            muc.sendMessage(msg);
                            textMessage.setText("");
                        } catch (Exception e) {
                            Toast.makeText(GroupChatActivity.this,
                                    "Error sending message", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }
                break;

            case R.id.btnGroupOptions:
                if (groupOptionsView.getVisibility() == View.VISIBLE) {
                    groupOptionsView.setVisibility(View.GONE);
                    mainView.setVisibility(View.GONE);
                }
                if (slide == 0) {
                    groupOptionsView.setVisibility(View.VISIBLE);
                    mainView.setVisibility(View.VISIBLE);
                    groupOptionsView.startAnimation(animSlideInRight);
                    slide++;
                } else {
                    groupOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                break;

            case R.id.btnJoinGroup:
                if (slide != 0) {
                    groupOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                openDialog(false);
                break;

            case R.id.btnCreateGroup:
                if (slide != 0) {
                    groupOptionsView.startAnimation(animSlideOutRight);
                    slide = 0;
                }
                openDialog(true);
                break;

            case R.id.btnCloseGroupCHat:
                finish();
                break;

            default:
                break;
        }
    }

    @Override
    public void processMessage(Message message) {
        if (message.getBody() != null) {
            String from = message.getSubject();
            String Body = message.getBody();
            // Add incoming message to the list view or similar
            if (!from.equals(nickname)) {
                msgs.add(new GroupMessage(from + " : " + Body, true));
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setListAdapter();
                    }
                });
            } else {
                msgs.add(new GroupMessage("Me : " + Body, false));
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setListAdapter();
                    }
                });
            }
        }
    }

    public void setListAdapter() {
        GroupMessageAdapter adapter = new GroupMessageAdapter(this, msgs);
        listview.setAdapter(adapter);
        if (groupPager.getCurrentItem() == 1) {
            GroupChatFragment frag = (GroupChatFragment) getSupportFragmentManager().getFragments().get(1);
            frag.updateGroupChatListView(adapter);
        }
    }

    public void switchFragments(){
        if(groupPager.getCurrentItem() == 0){
            groupPager.setCurrentItem(1, true);
        }else{
            groupPager.setCurrentItem(0, true);
        }
    }

    void openDialog(final boolean toCreate) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(toCreate ? "Create room" : "Join room");
        alertDialog.setMessage("Enter room's name");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton(toCreate ? "Create" : "Join",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        ProgressDialog dlg = ProgressDialog.show(
                                GroupChatActivity.this, toCreate ? "Creating..." : "Joining", "Please wait...", false);
                        dlg.show();
                        try {
                            if (muc.isJoined()) {
                                muc.removeMessageListener(GroupChatActivity.this);
                                muc.leave();
                                muc = null;
                            }
                            // Get the MultiUserChatManager
                            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
                            muc = manager.getMultiUserChat(input.getText().toString() + "@conference." + connection.getHost());

                            if (toCreate) {
                                muc.create(nickname);
                                muc.sendConfigurationForm(new Form(DataForm.Type.submit));
                            } else {
                                muc.join(nickname);
                            }
                            String title = muc.getRoom().toUpperCase(Locale.ENGLISH).substring(0, muc.getRoom().toUpperCase(Locale.ENGLISH).indexOf("@"));
                            tvtitle.setText(title);
                            roomname = input.getText().toString();

                            msgs = new ArrayList<GroupMessage>();
                            send.setOnClickListener(GroupChatActivity.this);
                            muc.addMessageListener(GroupChatActivity.this);
                            setListAdapter();
                            if(groupPager.getCurrentItem() == 1){
                                groupPager.setCurrentItem(0, true);
                            }else{
                                GroupsFragment gf = (GroupsFragment) getSupportFragmentManager().getFragments().get(0);
                                gf.refreshGroupsList();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "Room cannot be created", Toast.LENGTH_SHORT)
                                    .show();
                            tvtitle.setText("Join/Create a room");
                            if (muc.isJoined()) {
                                muc.removeMessageListener(GroupChatActivity.this);
                                try {
                                    muc.leave();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                                muc = null;
                            }
                            msgs = new ArrayList<GroupMessage>();
                            setListAdapter();
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

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (muc != null) {
            muc.removeMessageListener(this);
            muc = null;
        }
        super.onDestroy();
    }

    public void setupUI(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(GroupChatActivity.this);
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
}