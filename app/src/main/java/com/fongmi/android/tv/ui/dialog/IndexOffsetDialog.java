package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

public class IndexOffsetDialog extends InputDialog{

    private Callback mCallback;

    public IndexOffsetDialog(Activity activity) {
        super(activity);
    }

    public IndexOffsetDialog(Activity activity, Callback callback) {
        super(activity);
        mCallback = callback;
    }

    public static IndexOffsetDialog create(Activity activity, Callback callback) {
        return new IndexOffsetDialog(activity, callback);
    }

    public static IndexOffsetDialog create(Activity activity) {
        return new IndexOffsetDialog(activity);
    }

    public IndexOffsetDialog callback(Callback callback) {
        this.mCallback = callback;
        return this;
    }

    @Override
    protected void initView() {
        super.initView();
        mBinding.detail.setVisibility(View.GONE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mBinding.input.getLayoutParams());
        layoutParams.width = mActivity.getWindow().getDecorView().getWidth() / 4;
        layoutParams.gravity = Gravity.CENTER;
        mBinding.input.setLayoutParams(layoutParams);
    }

    @Override
    public void onPositive(DialogInterface dialog, int which) {
        mCallback.setIndexOffset(mBinding.input.getText().toString());
        dialog.dismiss();
    }

    public interface Callback {
        void setIndexOffset(String string);
    }
}
