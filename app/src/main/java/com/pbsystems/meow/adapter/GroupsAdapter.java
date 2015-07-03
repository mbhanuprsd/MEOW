package com.pbsystems.meow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.pbsystems.meow.R;

import org.jivesoftware.smackx.muc.RoomInfo;

import java.util.ArrayList;
public class GroupsAdapter extends ArrayAdapter<RoomInfo> {
    public GroupsAdapter(Context context, ArrayList<RoomInfo> rooms) {
        super(context, 0, rooms);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        RoomInfo room = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the
        // view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.groupslistitem, parent, false);
        }
        // Lookup view for data population
        TextView tvgroupname = (TextView) convertView.findViewById(R.id.itemGroupName);
        TextView tvnumber = (TextView) convertView.findViewById(R.id.itemNoofusers);
        // Populate the data into the template view using the data object

        tvgroupname.setText(room.getName());
        tvnumber.setText("" + room.getOccupantsCount());
        // Return the completed view to render on screen
        return convertView;
    }

}
