package com.pbsystems.meow.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pbsystems.meow.fragment.ChatFragment;
import com.pbsystems.meow.fragment.FriendsFragment;
import com.pbsystems.meow.fragment.GroupChatFragment;
import com.pbsystems.meow.fragment.GroupsFragment;

/**
 * Created by Bhanu on 09/06/15.
 */
public class GroupPagerAdapter extends FragmentPagerAdapter{

    public GroupPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Groups";
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
                return new GroupsFragment();
            case 1:
                return new GroupChatFragment();
            default:
                return null;
        }
    }
}
