package com.pbsystems.meow.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.pbsystems.meow.R;
import com.pbsystems.meow.activity.ChatActivity;


public class FriendsFragment extends Fragment {

    View rootView = null;
    ListView frndsListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_friends, container, false);
        frndsListView = (ListView) rootView.findViewById(R.id.friendsListView);
        frndsListener = (OnFriendsListUpdateListener) getActivity();
        updateFriendsList();
        frndsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatActivity ca = (ChatActivity) getActivity();
                ca.setSelectedFrnd(position);
            }
        });
        frndsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ChatActivity ca = (ChatActivity) getActivity();
                try {
                    ca.openProfileOfFriend(position);
                }catch (Exception e){

                }
                return true;
            }
        });
        return rootView;
    }

    public void updateFriendsList(){
        if(rootView != null) {
            Log.i("FriendsFragment", "Friends list updating");
            frndsListener.onFriendsUpdate(frndsListView);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            updateFriendsList();
        }
    }

    OnFriendsListUpdateListener frndsListener;

    public interface OnFriendsListUpdateListener {
        void onFriendsUpdate(ListView lv);
    }
}