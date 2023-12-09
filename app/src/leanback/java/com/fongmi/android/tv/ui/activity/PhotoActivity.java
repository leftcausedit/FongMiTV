package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.bean.Vod;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends Activity {

    private List<Vod> vodList;
    private Vod currentVod;
    private int currentIndex;
    private CustomTarget<Drawable> target;
    private String imageUrl;

    private ImageView imageView;

    private float originalScaleX;
    private float originalScaleY;
    private float times;

    public static void start(Activity activity, List<Vod> vodList, int currentIndex) {
        Intent intent = new Intent(activity, PhotoActivity.class);
        intent.putParcelableArrayListExtra("vodList", new ArrayList<>(vodList));
        intent.putExtra("currentIndex", currentIndex);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vodList = getIntent().getParcelableArrayListExtra("vodList");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);
        currentVod = vodList.get(currentIndex);

        imageView = new ImageView(this);

        // 创建 CustomTarget 对象，处理图片加载回调
        target = new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                imageView.setImageDrawable(resource);
            }

            @Override
            public void onLoadCleared(Drawable placeholder) {
                // imageView.setImageResource(R.drawable.ic_photo_empty);
            }

            @Override
            public void onLoadFailed(Drawable errorDrawable) {
                imageView.setImageResource(R.drawable.ic_photo_empty);
            }
        };

        setContentView(imageView);

        imageView.setOnClickListener(v -> onImageClick());
        imageView.setOnLongClickListener(v -> onImageLongClick());
        originalScaleX = imageView.getScaleX();
        originalScaleY = imageView.getScaleY();
        times = 1;
        loadImage();
    }

    private void onImageClick() {
        times = times + (float) 0.5;
        imageView.setScaleX(originalScaleX * times);
        imageView.setScaleY(originalScaleY * times);
    }

    private boolean onImageLongClick() {
        times = 1;
        imageView.setScaleX(originalScaleX);
        imageView.setScaleY(originalScaleY);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 在电视端，使用方向键模拟手势滑动
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                loadPreviousImage();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                loadNextImage();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                moveImageUp();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                moveImageDown();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void moveImageUp() {
        imageView.setTranslationY(imageView.getTranslationY() + 100.0f);
    }

    private void moveImageDown() {
        imageView.setTranslationY(imageView.getTranslationY() - 100.0f);
    }


    private void loadImage() {
        imageUrl = currentVod.getVodPic();
        ImgUtil.load(imageUrl, R.drawable.ic_photo_empty, target);
    }

    private void loadNextImage() {
        if (currentIndex < vodList.size() - 1) {
            currentIndex++;
            currentVod = vodList.get(currentIndex);
            loadImage();
        } else {
            Notify.show("没有下一张图片了哟~");
        }
    }

    private void loadPreviousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            currentVod = vodList.get(currentIndex);
            loadImage();
        } else {
            Notify.show("没有上一张图片了哟~");
        }
    }
}
