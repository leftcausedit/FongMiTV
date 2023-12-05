package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R; // 引入R类

public class PhotoActivity extends Activity {

    private static final String EXTRA_IMAGE_URL = "https://cdn-icons-png.flaticon.com/128/1160/1160307.png";

    public static void start(Activity activity, String imageUrl) {
        Intent intent = new Intent(activity, PhotoActivity.class);
        intent.putExtra(EXTRA_IMAGE_URL, imageUrl);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        ImageView imageView = findViewById(R.id.imageView);

        // 使用Glide加载图片
        Glide.with(this).load(imageUrl).into(imageView);

        // 设置点击图片关闭Activity
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}