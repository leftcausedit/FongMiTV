package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.fongmi.android.tv.R; // 引入R类
import com.fongmi.android.tv.utils.ImgUtil; // 引入 ImgUtil
import com.fongmi.android.tv.utils.Notify; 
import com.fongmi.android.tv.bean.Vod;
import com.github.panpf.zoomimage.ZoomImageView;
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

    private GestureDetector gestureDetector;

    private ZoomImageView zoomImageView;

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
        currentIndex = getIntent().getIntExtra("currentIndex",0);
        currentVod = vodList.get(currentIndex);

        zoomImageView = new ZoomImageView(this);

        // gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
        //     @Override
        //     public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //         float distanceX = e2.getX() - e1.getX();
        //         if (distanceX > 0) {
        //             // 右滑
        //             showNextImage();
        //         } else {
        //             // 左滑
        //             showPreviousImage();
        //         }
        //         return true;
        //     }
        // });

        // 创建 CustomTarget 对象，处理图片加载回调
        target = new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                zoomImageView.setImageDrawable(resource);
            }
            @Override
            public void onLoadCleared(Drawable placeholder) {}
            @Override
            public void onLoadFailed(Drawable errorDrawable) {
                zoomImageView.setImageResource(R.drawable.ic_photo_empty);
            }
        };

        setContentView(zoomImageView);
        zoomImageView.setScrollBar(null);

        loadImage();

        zoomImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });     
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 继续手势监听
        gestureDetector = new GestureDetector(this, new MyGestureListener());
    }

    private void loadImage() {
        imageUrl = currentVod.getVodPic();
        ImgUtil.load(imageUrl, R.drawable.ic_photo_empty, target);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY) &&
                    Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                // 左右滑动
                if (diffX < 0) {
                    // 向右滑动
                    loadNextImage();
                } else {
                    // 向左滑动
                    loadPreviousImage();
                }
                return true;
            }

            return false;
        }
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