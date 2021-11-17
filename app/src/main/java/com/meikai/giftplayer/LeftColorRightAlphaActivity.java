package com.meikai.giftplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LeftColorRightAlphaActivity extends AppCompatActivity {

    private ViewGroup layoutContainer;
    private GiftVideoView giftVideoView;
    private TextView tvTip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        layoutContainer = findViewById(R.id.layout_container);
        giftVideoView = findViewById(R.id.gift_video_view);
        tvTip = findViewById(R.id.tv_tip);


        giftVideoView.setOnVideoEndedListener(new GiftVideoView.OnVideoEndedListener() {
            @Override
            public void onVideoEnded() {
                findViewById(R.id.layout_btn).setVisibility(View.VISIBLE);
            }
        });

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bg_red:
                layoutContainer.setBackgroundColor(Color.RED);
                tvTip.setTextColor(Color.BLACK);
                break;
            case R.id.btn_bg_white:
                layoutContainer.setBackgroundColor(Color.WHITE);
                tvTip.setTextColor(Color.BLACK);
                break;
            case R.id.btn_bg_black:
                layoutContainer.setBackgroundColor(Color.BLACK);
                tvTip.setTextColor(Color.WHITE);
                break;
            case R.id.btn_start:
                giftVideoView.setVideoFromAssets("leftColorRightAlpha_1.mp4");
                break;
            case R.id.btn_hide:
                findViewById(R.id.layout_btn).setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }
}