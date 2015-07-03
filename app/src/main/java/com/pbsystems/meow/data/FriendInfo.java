package com.pbsystems.meow.data;

import android.graphics.Bitmap;

public class FriendInfo{

    public String userid;
    public Bitmap image;
    public boolean isOnline;
    public String name;
    public String email;

    public FriendInfo(String userid, Bitmap image, boolean isOnline, String name, String email) {
        this.userid = userid;
        this.image = image;
        this.isOnline = isOnline;
        this.name = name;
        this.email = email;
    }
}
