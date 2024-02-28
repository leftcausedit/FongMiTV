package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class RealTitleDialog extends InputDialog{

    private Callback mCallback;

    public RealTitleDialog(Activity activity) {
        super(activity);
    }

    public RealTitleDialog(Activity activity, Callback callback) {
        super(activity);
        mCallback = callback;
    }

    public static RealTitleDialog create(Activity activity, Callback callback) {
        return new RealTitleDialog(activity, callback);
    }

    public static RealTitleDialog create(Activity activity) {
        return new RealTitleDialog(activity);
    }

    public RealTitleDialog callback(Callback callback) {
        this.mCallback = callback;
        return this;
    }

    @Override
    protected void initView() {
        super.initView();
        mBinding.detail.setVisibility(View.GONE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mBinding.input.getLayoutParams());
//        layoutParams.width = mActivity.getWindow().getDecorView().getWidth();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.gravity = Gravity.CENTER;
        mBinding.input.setLayoutParams(layoutParams);
    }

    @Override
    public void onPositive(DialogInterface dialog, int which) {
        mCallback.setRealTitle(mBinding.input.getText().toString());
        dialog.dismiss();
    }

    public interface Callback {
        void setRealTitle(String string);
    }
}
