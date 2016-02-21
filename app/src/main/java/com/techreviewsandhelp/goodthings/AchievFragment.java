package com.techreviewsandhelp.goodthings;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class AchievFragment extends Fragment {
    ArrayList<TextView> goodCoins;
    static AchievFragment achievFragment;

    public AchievFragment() {}
    public static AchievFragment newInstance() {
        if (achievFragment == null) achievFragment = new AchievFragment();
        return achievFragment;
    }

    public ArrayList<TextView> getGoodCoins(){
        return goodCoins;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("lyfecycles", "Achiev.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_achievements, container, false);
        goodCoins = new ArrayList<>();
        goodCoins.add((TextView)rootView.findViewById(R.id.good_count));
        goodCoins.add((TextView)rootView.findViewById(R.id.deals_count));
        goodCoins.add((TextView) rootView.findViewById(R.id.better_count));
        //GoodThingsTableHelper.refreshAchievements(getContext(), this);

        ImageButton shareButton = (ImageButton)rootView.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String link = "http://play.google.com/";
                String stringToSend = String.format(getString(R.string.sharing_string),goodCoins.get(0).getText(),goodCoins.get(1).getText(),goodCoins.get(2).getText(),link);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, stringToSend);
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("lyfecycles", "Achiev.onViewCreated");

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d("lyfecycles", "Achiev.onHiddenChanged");
    }


}
