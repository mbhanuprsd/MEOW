package com.pbsystems.meow.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pbsystems.meow.R;
import com.pbsystems.meow.data.FriendInfo;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class FriendsAdapter extends ArrayAdapter<FriendInfo> {
    public FriendsAdapter(Context context, ArrayList<FriendInfo> frnds) {
        super(context, 0, frnds);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        FriendInfo frnd = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the
        // view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.friendlistitem, parent, false);
        }
        // Lookup view for data population
        TextView tvfrndname = (TextView) convertView.findViewById(R.id.tvfrndname);
        ImageView ivFrndpic = (ImageView) convertView.findViewById(R.id.tvfrndpic);

        if(frnd.isOnline){
            tvfrndname.setTextColor(Color.parseColor("#045cb5"));
        }else{
            tvfrndname.setTextColor(Color.RED);
        }
        // Populate the data into the template view using the data object

        tvfrndname.setText(frnd.name);
        ivFrndpic.setImageBitmap(frnd.image);
        // Return the completed view to render on screen
        return convertView;
    }


}
