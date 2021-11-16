package com.meikai.giftplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DemoActivity extends AppCompatActivity {

    private ViewGroup layoutContainer;
    private GiftVideoView giftVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        layoutContainer = findViewById(R.id.layout_container);
        giftVideoView = findViewById(R.id.gift_video_view);


    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bg_red:
                layoutContainer.setBackgroundColor(Color.RED);
                break;
            case R.id.btn_bg_white:
                layoutContainer.setBackgroundColor(Color.WHITE);
                break;
            case R.id.btn_bg_black:
                layoutContainer.setBackgroundColor(Color.BLACK);
                break;
            case R.id.btn_start:
                giftVideoView.setVideoFromAssets("leftColorRightAlpha_1.mp4");
                break;
            case R.id.btn_hide:
                findViewById(R.id.layout_btn).setVisibility(View.GONE);
                Toast.makeText(view.getContext(), "若要再次显示按钮，请返回重新打开", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
}