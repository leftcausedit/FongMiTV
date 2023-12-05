package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R; // 引入R类
import com.github.chrisbanes.photoview.PhotoView; // 引入 PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher;

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
        // ImageView imageView = findViewById(R.id.imageView);
        PhotoView photoView = findViewById(R.id.photoView); // 修改为 PhotoView

        // 使用Glide加载图片
        // Glide.with(this).load(imageUrl).into(imageView);
        Glide.with(this).load(imageUrl).into(photoView);

        // 设置点击图片关闭Activity
        // imageView.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         finish();
        //     }
        // });

        // 设置点击图片关闭Activity（如果需要）
        // photoView.setOnClickListener(v -> finish());

        // 添加缩放功能
        PhotoViewAttacher photoAttacher = new PhotoViewAttacher(photoView);
        photoAttacher.update();
    }
}