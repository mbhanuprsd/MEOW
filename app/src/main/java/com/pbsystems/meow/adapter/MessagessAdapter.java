package com.pbsystems.meow.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pbsystems.meow.R;
import com.pbsystems.meow.data.CMessage;

public class MessagessAdapter extends ArrayAdapter<CMessage> {
	public MessagessAdapter(Context context, ArrayList<CMessage> messagess) {
		super(context, 0, messagess);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Get the data item for this position
		CMessage cmessage = getItem(position);
		// Check if an existing view is being reused, otherwise inflate the
		// view
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(
					R.layout.chatlistitem, parent, false);
		}
		// Lookup view for data population
		TextView tvMeesage = (TextView) convertView.findViewById(R.id.text1);

		LinearLayout parentlay = (LinearLayout) convertView
				.findViewById(R.id.singleMessageContainer);
		// Populate the data into the template view using the data object

		tvMeesage.setText(cmessage.text);
		if (cmessage.isLeft) {
			parentlay.setGravity(Gravity.LEFT);
			tvMeesage.setBackgroundResource(R.drawable.bubble_yellow);
		} else {
			parentlay.setGravity(Gravity.RIGHT);
			tvMeesage.setBackgroundResource(R.drawable.bubble_green);
		}

		// Return the completed view to render on screen
		return convertView;
	}
}
