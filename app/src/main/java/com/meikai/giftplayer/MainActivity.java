package com.meikai.giftplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_01:
                VideoViewActivity.launch(this, "leftColorRightAlpha_1.mp4");
                break;
            case R.id.btn_02:
                Intent intent = new Intent(view.getContext(), LeftColorRightAlphaActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_03:
                VideoViewActivity.launch(this, "topColorBottomAlpha_1.mp4");
                break;
            case R.id.btn_04:
                Intent intent2 = new Intent(view.getContext(), TopColorBottomAlphaActivity.class);
                startActivity(intent2);
                break;
            default:
                break;
        }
    }
}
