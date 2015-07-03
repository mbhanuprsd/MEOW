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
import com.pbsystems.meow.activity.GroupChatActivity;
import com.pbsystems.meow.adapter.GroupMessageAdapter;


public class GroupChatFragment extends Fragment {

    View rootview = null;
    LinearLayout sendLayout;
    Animation animSlideUp, animSlideDown;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_groupchat, container, false);
        return rootview;
    }

    public void updateGroupChatListView(GroupMessageAdapter adapter) {
        if (rootview != null) {
            ListView lv = (ListView) rootview.findViewById(R.id.groupChatListView);
            lv.setAdapter(adapter);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (rootview != null) {
            GroupChatActivity gca = (GroupChatActivity) getActivity();
            sendLayout = (LinearLayout) gca.findViewById(R.id.linearLayout21);
            animSlideUp = AnimationUtils.loadAnimation(gca.getApplicationContext(), R.anim.slide_in_left);
            animSlideDown = AnimationUtils.loadAnimation(gca.getApplicationContext(), R.anim.slide_out_right);
            animSlideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    sendLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (isVisibleToUser) {
                gca.setListAdapter();
                sendLayout.setVisibility(View.VISIBLE);
                sendLayout.startAnimation(animSlideUp);
            } else {
                hideSoftKeyboard(gca);
                sendLayout.startAnimation(animSlideDown);
            }
        }
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
