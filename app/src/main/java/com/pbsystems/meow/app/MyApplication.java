package com.pbsystems.meow.app;

import org.jivesoftware.smack.AbstractXMPPConnection;

import android.app.Application;
import android.net.sip.SipProfile;

public class MyApplication extends Application {

	private boolean secureChat = false;

	private AbstractXMPPConnection connection = null;

	private static MyApplication instance = null;

	private SipProfile mySipProfile = null;

	public synchronized static MyApplication getInstance() {
		if (instance == null) {
			instance = new MyApplication();
		}
		return instance;
	}

	public void setConnection(AbstractXMPPConnection connection) {
		this.connection = connection;
	}

	public AbstractXMPPConnection getConnection() {
		return this.connection;
	}

	public boolean isSecureChat() {
		return secureChat;
	}

	public void setSecureChat(boolean secureChat) {
		this.secureChat = secureChat;
	}

    public SipProfile getMySipProfile() {
        return mySipProfile;
    }

    public void setMySipProfile(SipProfile mySipProfile) {
        this.mySipProfile = mySipProfile;
    }
}
