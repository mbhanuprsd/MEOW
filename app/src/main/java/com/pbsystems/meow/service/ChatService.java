package com.pbsystems.meow.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.pbsystems.meow.R;
import com.pbsystems.meow.activity.ChatActivity;
import com.pbsystems.meow.activity.LoginActivity;
import com.pbsystems.meow.activity.SipCallActivity;
import com.pbsystems.meow.app.MyApplication;
import com.pbsystems.meow.database.ChatDatabaseManager;
import com.pbsystems.meow.utils.NetworkUtil;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;

import java.io.File;
import java.util.Collection;

public class ChatService extends Service {
    String HOST, USERNAME, PASSWORD;
    int PORT;

    ChatDatabaseManager mydb;

    AbstractXMPPConnection connection = null;
    String frndname, fnd;

    MyMessageListener messageListener;
    RequestListener myrequestListener;
    MyRosterListener rosterListener;
    FileTransferManager manager;

    NetworkChangeReciever netReceiver;
    IntentFilter filter;
    NotificationManager mNotifyManager;
    Notification.Builder mBuilder;

    byte[] imgbyteArray;

    SipManager sipmanager = null;
    SipProfile me = null;
    String domain;
    SipAudioCall sipCall;

    @Override
    public void onCreate() {
        super.onCreate();

        loginFromPrefs();
        Toast.makeText(ChatService.this, "Servcie started", Toast.LENGTH_LONG)
                .show();

        netReceiver = new NetworkChangeReciever();
        filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.SipDemo.INCOMING_CALL");
        registerReceiver(netReceiver, filter);
    }

    @Override
    public void onDestroy() {
        SharedPreferences.Editor editor = getSharedPreferences("Chat_User",
                MODE_PRIVATE).edit();
        editor.remove("bg_red");
        editor.remove("bg_green");
        editor.remove("bg_blue");
        editor.commit();

        unregisterReceiver(netReceiver);

        NotificationManager notificationManager = (NotificationManager) getSystemService(ChatService.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        stopForeground(true);

        if (connection != null) {
            if (rosterListener != null)
                Roster.getInstanceFor(connection).removeRosterListener(
                        rosterListener);
            if (messageListener != null)
                connection.removeAsyncStanzaListener(messageListener);
            if (myrequestListener != null)
                connection.removeAsyncStanzaListener(myrequestListener);
            try {
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            connection = null;
            rosterListener = null;
            messageListener = null;
            myrequestListener = null;
        }
        Toast.makeText(ChatService.this, "Service stopped", Toast.LENGTH_SHORT)
                .show();
        super.onDestroy();
    }

    void loginFromPrefs() {

        loadPreferences();

        if (USERNAME.length() > 0 && PASSWORD.length() > 0 && HOST.length() > 0) {
            new Connect().execute("");
        } else {
            Toast.makeText(ChatService.this,
                    "Error getting login details to service", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("Chat_User",
                MODE_PRIVATE);
        USERNAME = prefs.getString("Username", "");
        PASSWORD = prefs.getString("Password", "");
        HOST = prefs.getString("Host", "");
        PORT = prefs.getInt("Port", 0);
        domain = "officesip.meow";
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("service", "started");
        if (intent.getStringExtra("FriendName") != null) {
            setupChat(intent.getStringExtra("FriendName"));
        }

        if (intent.getStringExtra("SetupAddfriend") != null) {
            this.connection = ((MyApplication) getApplication()).getConnection();
            setupAddfriends();

            Intent i = new Intent(ChatService.this, ChatActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(ChatService.this,
                    0, i, 0);

            Notification n = new Notification.Builder(ChatService.this)
                    .setContentTitle("Meow!").setContentText("Running..")
                    .setSmallIcon(R.drawable.ic_launcher).setAutoCancel(true)
                    .setContentIntent(pIntent).build();
            startForeground(3389, n);
        }

        if (intent.getStringExtra("reopen") != null) {
            MyApplication p = (MyApplication) getApplication();
            p.setConnection(connection);
        }

        if (intent.getStringExtra("sentFile") != null) {
            sendFile(intent.getStringExtra("sentFile"),
                    intent.getStringExtra("Receiver"));
        }

        if (intent.getSerializableExtra("image") != null) {
            imgbyteArray = (byte[]) intent.getSerializableExtra("image");
            new SaveVcard().execute("");
        }

        if (intent.getStringExtra("SipCall") != null) {
            initiateCall(intent.getStringExtra("SipCall"));
        }

        if (intent.getStringExtra("EndSipCall") != null) {
            closeCall();
        }
        return START_STICKY;
    }

    void sendFile(final String fn, final String frnd) {
        String[] segments = fn.split("/");
        String fname = segments[segments.length - 1];
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new Notification.Builder(this);
        mBuilder.setContentTitle(fname + " to " + frnd)
                .setContentText("Sending in progress")
                .setSmallIcon(R.drawable.attach);

        new Thread() {
            public void run() {
                File file = new File(fn);
                OutgoingFileTransfer transfer = manager
                        .createOutgoingFileTransfer(frnd + "@"
                                + connection.getHost() + "/Smack");
                try {
                    transfer.sendFile(file, file.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                while (!transfer.isDone()) {
                    mBuilder.setProgress(100,
                            (int) (transfer.getProgress() * 100), false);
                    // Displays the progress bar for the first time.
                    mNotifyManager.notify(1432, mBuilder.build());

                    if (transfer.getStatus().equals(Status.error)) {
                        Log.i("Transfer send",
                                "ERROR!!! " + transfer.getError());
                    } else if (transfer.getStatus().equals(Status.cancelled)
                            || transfer.getStatus().equals(Status.refused)) {
                        Log.i("Transfer send",
                                "Cancelled!!! " + transfer.getError());
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (transfer.getStatus().equals(Status.refused)
                        || transfer.getStatus().equals(Status.error)
                        || transfer.getStatus().equals(Status.cancelled)) {
                    mBuilder.setContentText("Sending interrupted")
                            // Removes the progress bar
                            .setProgress(0, 0, false);
                    mNotifyManager.notify(1432, mBuilder.build());
                    Log.i("Transfer send", "refused cancelled error "
                            + transfer.getError());
                } else {
                    mBuilder.setContentText("Sending complete")
                            // Removes the progress bar
                            .setProgress(0, 0, false);
                    mNotifyManager.notify(1432, mBuilder.build());
                    Log.i("Transfer send", "Success");
                    String[] segments = fn.split("/");
                    String fname = segments[segments.length - 1];
                    // sending message
                    Message msg = new Message(frnd + "@" + connection.getHost()
                            + "/Smack",
                            org.jivesoftware.smack.packet.Message.Type.chat);
                    msg.setBody(fname);
                    long recid = System.currentTimeMillis();
                    msg.setStanzaId("" + recid);
                    try {
                        connection.sendStanza(msg);
                        Intent in = new Intent();
                        in.setAction("fileSent");
                        in.putExtra("message", fname);
                        sendBroadcast(in);

                        if (mydb.insertMessage(frnd, "me", fname, recid)) {
                            Log.i("Database",
                                    "Message sent is inserted into database");
                        } else {
                            Log.e("Database error",
                                    "Message sent is not able to insert into database");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }.start();
    }

    void setupChat(String frnd) {

        StanzaFilter filter = new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {
                if (packet instanceof Message) {
                    if (((Message) packet).getBody() != null && ((Message) packet).getType() == Message.Type.chat) {
                        return true;
                    }
                }
                return false;
            }
        };

        fnd = frnd;

        if (messageListener != null)
            connection.removeAsyncStanzaListener(messageListener);
        messageListener = new MyMessageListener();
        connection.addAsyncStanzaListener(messageListener, filter);
    }

    void setupAddfriends() {
        Roster.getInstanceFor(connection).setSubscriptionMode(
                Roster.SubscriptionMode.accept_all);

        StanzaFilter requestFilter = new StanzaFilter() {

            @Override
            public boolean accept(Stanza packet) {
                if (packet instanceof Presence) {
                    Presence presence = (Presence) packet;
                    if (presence.getType().equals(Presence.Type.subscribed)
                            || presence.getType().equals(
                            Presence.Type.subscribe)
                            || presence.getType().equals(
                            Presence.Type.unsubscribed)
                            || presence.getType().equals(
                            Presence.Type.unsubscribe)) {
                        return true;
                    }
                }
                return false;
            }
        };

        if (myrequestListener != null)
            connection.removeAsyncStanzaListener(myrequestListener);
        myrequestListener = new RequestListener();
        connection.addAsyncStanzaListener(myrequestListener, requestFilter);

        rosterListener = new MyRosterListener();
        Roster.getInstanceFor(connection).addRosterListener(rosterListener);
        receiveFile(connection);
    }

    private void receiveFile(AbstractXMPPConnection connection2) {
        if (connection2 != null) {

            // FileTransferNegotiator.IBB_ONLY = true;
            ServiceDiscoveryManager sdm = ServiceDiscoveryManager
                    .getInstanceFor(connection2);
            if (sdm == null) {
                sdm = ServiceDiscoveryManager.getInstanceFor(connection2);
            }
            sdm.addFeature("http://jabber.org/protocol/disco#info");
            sdm.addFeature("http://jabber.org/protocol/disco#item");
            sdm.addFeature("jabber:iq:privacy");

            manager = FileTransferManager.getInstanceFor(connection2);
            manager.addFileTransferListener(new FileTransferListener() {

                public void fileTransferRequest(
                        final FileTransferRequest request) {

                    mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mBuilder = new Notification.Builder(ChatService.this);
                    mBuilder.setContentTitle("File Download")
                            .setContentText("Download in progress")
                            .setSmallIcon(R.drawable.attach);

                    new Thread() {
                        public void run() {
                            IncomingFileTransfer transfer = request.accept();
                            File mf = Environment.getExternalStorageDirectory();
                            File file = new File(mf.getAbsoluteFile()
                                    + "/Meow/" + transfer.getFileName());

                            try {
                                transfer.recieveFile(file);
                                while (!transfer.isDone()) {
                                    mBuilder.setProgress(
                                            100,
                                            (int) (transfer.getProgress() * 100),
                                            false);
                                    // Displays the progress bar for the first
                                    // time.
                                    mNotifyManager.notify(1433,
                                            mBuilder.build());
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        Log.e("", e.getMessage());
                                    }
                                    if (transfer.getStatus().equals(
                                            Status.error)) {
                                        Log.e("ERROR!!! ", transfer.getError()
                                                + "");
                                    }
                                    if (transfer.getException() != null) {
                                        mBuilder.setContentText(
                                                "Download interrupted")
                                                // Removes the progress bar
                                                .setProgress(0, 0, false);
                                        mNotifyManager.notify(1433,
                                                mBuilder.build());
                                        transfer.getException()
                                                .printStackTrace();
                                    }
                                }
                                mBuilder.setContentText("Download complete")
                                        // Removes the progress bar
                                        .setProgress(0, 0, false);
                                mNotifyManager.notify(1433, mBuilder.build());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            });
        }
    }

    private class Connect extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            XMPPTCPConnectionConfiguration connConfig = XMPPTCPConnectionConfiguration
                    .builder().setHost(HOST).setPort(PORT)
                    .setUsernameAndPassword(USERNAME, PASSWORD)
                    .setSecurityMode(SecurityMode.disabled)
                    .setServiceName(HOST).build();
            connection = new XMPPTCPConnection(connConfig);
            try {
                connection.connect();
                Log.i("XMPPChatDemoActivity",
                        "Connected to " + connection.getHost());

                connection.login();

                // Set the status to available
                Presence presence = new Presence(Type.available);
                presence.setMode(Presence.Mode.available);
                connection.sendStanza(presence);
                MyApplication app = (MyApplication) getApplication();
                app.setConnection(connection);


            } catch (Exception ex) {
                connection = null;
                Log.e("XMPPChatDemoActivity", "Failed to connect");
                Log.e("XMPPChatDemoActivity", ex.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (connection != null) {
                try {
                    Intent intent = new Intent(ChatService.this,
                            ChatActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    mydb = new ChatDatabaseManager(ChatService.this, connection
                            .getUser().substring(0,
                                    connection.getUser().indexOf("@")));
                    PackageManager pm = ChatService.this.getPackageManager();

                    if (pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) && pm.hasSystemFeature(PackageManager.FEATURE_SIP_VOIP)
                            && pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                        initializeManager();
                    } else {
                        Toast.makeText(ChatService.this, "Required Features for Call are not available", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // when connection didn't connect
                Toast.makeText(ChatService.this, "Couldn't Login",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ChatService.this,
                        LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                stopSelf();
            }
        }
    }

    public void initializeManager() {
        if (sipmanager == null) {
            sipmanager = SipManager.newInstance(this);
        }
        initializeLocalProfile();
    }

    public void initializeLocalProfile() {
        Log.v("me", me + "  get velus");
        if (me != null) {
            closeLocalProfile();
        }

        try {
            Log.v("SipProfile.Builder", "SipProfile.Builder");

            SipProfile.Builder builder = new SipProfile.Builder(USERNAME,
                    domain);
            builder.setOutboundProxy(HOST);
            builder.setPassword(PASSWORD);
            me = builder.build();
            Log.v("me", me + "  get velus");

            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            sipmanager.open(me, pi, null);
            Log.v("intent call ", "intent call");

            sipmanager.setRegistrationListener(me.getUriString(),
                    new SipRegistrationListener() {
                        public void onRegistering(String localProfileUri) {
                            updateStatus(0);
                        }

                        public void onRegistrationDone(String localProfileUri, long expiryTime) {
                            updateStatus(1);
                            MyApplication ap = (MyApplication) getApplication();
                            ap.setMySipProfile(me);
                        }

                        public void onRegistrationFailed(
                                String localProfileUri, int errorCode,
                                String errorMessage) {
                            updateStatus(2);
                        }
                    });
        } catch (Exception pe) {
            updateStatus(3);
        }
    }

    public void closeLocalProfile() {
        Log.v("closeLocalProfile ", sipmanager + "  closeLocalProfile");
        if (sipmanager == null) {
            return;
        }
        try {
            Log.v("closeLocalProfile me", me + "  get velus");

            if (me != null) {
                Log.v("close me URI ", me.getUriString() + "  get velus");
                sipmanager.close(me.getUriString());
            }
        } catch (Exception ee) {
            Log.d("onDestroy", "Failed to close local profile.", ee);
        }
    }

    public void initiateCall(final String sipAddress) {

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    if (call.isMuted()) {
                        call.toggleMute();
                    }
                    callStatusListener = (OnCallStatusUpdateListener) ChatService.this;
                    callStatusListener.onCallStatusUpdate("Call Established : "+call.getPeerProfile().getUserName());
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    callStatusListener = (OnCallStatusUpdateListener) ChatService.this;
                    callStatusListener.onCallStatusUpdate("Call Ended : "+call.getPeerProfile().getUserName());
                    callStatusListener.onCallEnded();
                }
            };

            sipCall = sipmanager.makeAudioCall(me.getUriString(), sipAddress,
                    listener, 30);
            Intent intent = new Intent(ChatService.this,
                    SipCallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } catch (Exception e) {
            Log.i("InitiateCall",
                    "Error when trying to close manager.", e);
            if (me != null) {
                try {
                    sipmanager.close(me.getUriString());
                } catch (Exception ee) {
                    Log.i("InitiateCall",
                            "Error when trying to close manager.", ee);
                    ee.printStackTrace();
                }
            }
            if (sipCall != null) {
                sipCall.close();
            }
        }
    }

    void closeCall() {
        if (sipCall != null) {
            try {
                sipCall.endCall();
            } catch (Exception e) {
                Log.e("SipCall", "Cannot end call");
            }
            sipCall.close();
            sipCall = null;
        }
    }

    void updateStatus(int status) {
        Intent intent = new Intent();
        intent.setAction("SipProfileStatus");
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    private class SaveVcard extends AsyncTask<String, Void, String> {
        VCard userVCard = null;

        @Override
        protected String doInBackground(String... params) {
            try {
                ProviderManager.addIQProvider("vCard", "vcard-temp", new VCardProvider());
                VCardManager cardManager = VCardManager.getInstanceFor(connection);
                userVCard = new VCard();
                if (imgbyteArray != null) {
                    userVCard.setAvatar(imgbyteArray);
                }
                cardManager.saveVCard(userVCard);
                Log.i("VCARD", "Vcard saved");
            } catch (Exception e) {
                Log.i("VCARD", "Vcard couldn't be saved : " + e.getLocalizedMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (userVCard != null) {
                Toast.makeText(ChatService.this, "Profile pic saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatService.this, "Failed saving user profile pic", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // RosterListener to detect changes
    class MyRosterListener implements RosterListener {
        @Override
        public void presenceChanged(Presence arg0) {
            Intent in = new Intent();
            in.setAction("StatusChange");
            sendBroadcast(in);
        }

        @Override
        public void entriesUpdated(Collection<String> arg0) {
        }

        @Override
        public void entriesDeleted(Collection<String> arg0) {
        }

        @Override
        public void entriesAdded(Collection<String> arg0) {
            if (frndname != null) {
                Notification n = new Notification.Builder(ChatService.this)
                        .setContentTitle("Friend Added")
                        .setContentText(
                                frndname + " is added to your friends list")
                        .setAutoCancel(true).setSmallIcon(R.drawable.add_user)
                        .build();

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(1993, n);
            } else {
                if (arg0.size() > 0) {
                    Notification n = new Notification.Builder(ChatService.this)
                            .setContentTitle("Friend request completed")
                            .setContentText("New friend added").setAutoCancel(true)
                            .setSmallIcon(R.drawable.add_user).build();

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(1993, n);
                }
            }
        }
    }

    // Request listener for adding friends

    class RequestListener implements StanzaListener {
        @Override
        public void processPacket(Stanza paramPacket) {
            System.out.println("\n\n");
            if (paramPacket instanceof Presence) {
                Presence presence = (Presence) paramPacket;
                String email = presence.getFrom();
                System.out.println("chat invite status changed by user: : "
                        + email + " calling listner");
                System.out.println("presence: " + presence.getFrom()
                        + "; type: " + presence.getType() + "; to: "
                        + presence.getTo() + "; " + presence.toXML());
                Roster roster = Roster.getInstanceFor(connection);
                for (RosterEntry rosterEntry : roster.getEntries()) {
                    System.out.println("jid: " + rosterEntry.getUser()
                            + "; type: " + rosterEntry.getType() + "; status: "
                            + rosterEntry.getStatus());
                }
                System.out.println("\n\n\n");
                if (presence.getType().equals(Presence.Type.subscribe)) {
                    Presence newp = new Presence(Presence.Type.subscribed);
                    newp.setMode(Presence.Mode.available);
                    newp.setPriority(24);
                    newp.setTo(presence.getFrom());
                    try {
                        connection.sendStanza(newp);
                        Presence subscription = new Presence(
                                Presence.Type.subscribe);
                        subscription.setTo(presence.getFrom());
                        connection.sendStanza(subscription);

                        frndname = presence.getFrom().substring(0,
                                presence.getFrom().indexOf("@"));
                        try {
                            Toast.makeText(ChatService.this,
                                    "'" + frndname + "' added as your friend",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            Log.i("ChatService",
                                    "Cannot show notification or toast");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (presence.getType().equals(Presence.Type.unsubscribe)) {
                    Presence newp = new Presence(Presence.Type.unsubscribed);
                    newp.setMode(Presence.Mode.available);
                    newp.setPriority(24);
                    newp.setTo(presence.getFrom());
                    try {
                        connection.sendStanza(newp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Message listener for getting messages

    class MyMessageListener implements StanzaListener {
        @Override
        public void processPacket(Stanza packet) {
            Message msg = (Message) packet;
            String frnmessage = msg.getBody();

            if (msg.getFrom().contains("admin")) {
                Notification n = new Notification.Builder(ChatService.this)
                        .setContentTitle("Admin Notification")
                        .setContentText(frnmessage).setAutoCancel(true)
                        .setSmallIcon(R.drawable.adminmsg).setAutoCancel(true)
                        .build();

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(007, n);
            } else {
                final String name = msg.getFrom().substring(0,
                        msg.getFrom().indexOf("@"));

                if (!mydb.checkIfMessageIsAlreadyInserted(msg.getStanzaId())) {

                    if (mydb.insertMessage(name, "you", frnmessage, Long.parseLong(msg.getStanzaId()))) {
                        Log.i("Database",
                                "Message recieved is inserted into database");

                        Notification n = new Notification.Builder(
                                ChatService.this).setContentTitle(name)
                                .setContentText(frnmessage).setAutoCancel(true)
                                .setSmallIcon(R.drawable.msg)
                                .setAutoCancel(true).build();

                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(0405, n);

                        if (msg.getFrom().contains(fnd)) {
                            Intent in = new Intent();
                            in.setAction("chatMessage");
                            in.putExtra("message", frnmessage);
                            in.putExtra("friend", name);
                            sendBroadcast(in);
                        }
                    } else {
                        Log.e("Database error",
                                "Message recieved is not able to insert into database");
                    }
                }
            }
        }
    }

    class NetworkChangeReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = NetworkUtil.getConnectivityStatusString(context);
            if (status.equals("Not connected to Internet")) {
                Toast.makeText(ChatService.this,
                        "Internet connection lost. Logging out",
                        Toast.LENGTH_SHORT).show();
                ChatService.this.stopSelf();
                Intent in = new Intent();
                in.setAction("NetDisc");
                sendBroadcast(in);
            }
            if (intent.getAction().equals("android.SipDemo.INCOMING_CALL")) {
                if (sipCall == null) {
                    SipAudioCall incomingCall = null;
                    try {

                        SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                            @Override
                            public void onRinging(SipAudioCall call, SipProfile caller) {
                                try {
                                    call.answerCall(5000);
                                } catch (Exception e) {
                                    callStatusListener = (OnCallStatusUpdateListener) ChatService.this;
                                    callStatusListener.onCallStatusUpdate("Call received but cannot be answered : "+call.getPeerProfile().getUserName());
                                    callStatusListener.onCallEnded();
                                }
                            }

                            @Override
                            public void onCallEnded(SipAudioCall call) {
                                callStatusListener = (OnCallStatusUpdateListener) ChatService.this;
                                callStatusListener.onCallEnded();
                                super.onCallEnded(call);
                            }
                        };

                        incomingCall = sipmanager.takeAudioCall(intent, listener);
                        incomingCall.answerCall(30);
                        incomingCall.startAudio();
                        incomingCall.setSpeakerMode(true);
                        if (incomingCall.isMuted()) {
                            incomingCall.toggleMute();
                        }
                        sipCall = incomingCall;
                        Intent in = new Intent(ChatService.this,
                                SipCallActivity.class);
                        in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(in);
                        Log.v("grt Values", incomingCall + " get");
                    } catch (Exception e) {
                        if (incomingCall != null) {
                            incomingCall.close();
                            Log.v("end call", "incoming call");
                        }
                    }
                }
            }
        }
    }

    OnCallStatusUpdateListener callStatusListener;

    public interface OnCallStatusUpdateListener{
        void onCallStatusUpdate(String status);
        void onCallEnded();
    }
}
