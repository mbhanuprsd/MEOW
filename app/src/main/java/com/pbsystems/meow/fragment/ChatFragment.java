package com.pbsystems.meow.fragment;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.pbsystems.meow.R;
import com.pbsystems.meow.activity.ChatActivity;
import com.pbsystems.meow.adapter.MessagessAdapter;

public class ChatFragment extends Fragment {

    View rootview = null;
    Animation animSlideUp, animSlideDown;
    LinearLayout sendLay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_chat, container, false);
        return rootview;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (rootview != null) {
            ChatActivity ca = (ChatActivity) getActivity();
            sendLay = (LinearLayout) ca.findViewById(R.id.linearLayout2);
            animSlideUp = AnimationUtils.loadAnimation(ca.getApplicationContext(), R.anim.slide_in_left);
            animSlideDown = AnimationUtils.loadAnimation(ca.getApplicationContext(), R.anim.slide_out_right);
            animSlideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    sendLay.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (isVisibleToUser) {
                ca.setListAdapter();
                if (!ca.getSelectedFrnd().equals("")) {
                    sendLay.setVisibility(View.VISIBLE);
                    sendLay.startAnimation(animSlideUp);
                }
            } else {
                hideSoftKeyboard(ca);
                sendLay.startAnimation(animSlideDown);
            }
        }
    }

    public void updateChatListView(MessagessAdapter adapter) {
        ListView lv = (ListView) rootview.findViewById(R.id.chatListView);
        lv.setAdapter(adapter);
    }

    public static void hideSoftKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
