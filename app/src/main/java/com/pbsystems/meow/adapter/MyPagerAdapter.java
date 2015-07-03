package com.pbsystems.meow.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pbsystems.meow.fragment.FriendsFragment;
import com.pbsystems.meow.fragment.ChatFragment;


public class MyPagerAdapter extends FragmentPagerAdapter {

    public MyPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Friends";
            case 1:
                return "Chat";
            default:
                return "";
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new FriendsFragment();
            case 1:
                return new ChatFragment();
            default:
                return null;
        }
    }
}
