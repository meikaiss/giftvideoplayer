package com.demo.app2;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.demo.libgiftplayer2.GiftPlayerView;

public class MainActivity4 extends AppCompatActivity {

    private GiftPlayerView giftPlayerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_4);

        giftPlayerView = findViewById(R.id.gift);

        //when set 0.5f, then got Black vertical line
//        giftPlayerView.init(this, "test2.mp4");

        //when set 0.4956f, then it is ok.  So the alpha video file is bad.
        giftPlayerView.init(this, "test2.mp4", 0.4956f);
    }
}
