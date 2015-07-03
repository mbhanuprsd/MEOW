package com.pbsystems.meow.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.pbsystems.meow.R;
import com.pbsystems.meow.activity.GroupChatActivity;
import com.pbsystems.meow.adapter.GroupsAdapter;
import com.pbsystems.meow.app.MyApplication;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;

import java.util.ArrayList;
import java.util.Set;

public class GroupsFragment extends Fragment {

    AbstractXMPPConnection connection;
    View rootview = null;
    GroupChatActivity gca;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_groups, container, false);
        gca = (GroupChatActivity) getActivity();
        MyApplication app = (MyApplication) gca.getApplication();
        connection = app.getConnection();
        refreshGroupsList();
        return rootview;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            refreshGroupsList();
        }
    }

    public void refreshGroupsList() {
        if (rootview != null) {
            ListView lv = (ListView) rootview.findViewById(R.id.groupsListView);
            GroupsAdapter adapter = new GroupsAdapter(gca, getRooms());
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    gca.switchFragments();
                }
            });
        }
    }

    ArrayList<RoomInfo> getRooms() {
        ArrayList<RoomInfo> rooms = new ArrayList<RoomInfo>();
        MultiUserChatManager m = MultiUserChatManager.getInstanceFor(connection);
        Set<String> rms = m.getJoinedRooms();
        for (String rm : rms) {
            try {
                rooms.add(m.getRoomInfo(rm));
            } catch (Exception e) {
                Log.i("Room", "Error adding room to list");
            }
        }
        return rooms;
    }
}
