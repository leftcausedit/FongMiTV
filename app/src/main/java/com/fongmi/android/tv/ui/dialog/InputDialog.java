package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogInputBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class InputDialog {

    DialogInputBinding mBinding;
    Activity mActivity;
    String title;
    String detail = "";
    String preInput;

    public InputDialog(Activity activity) {
        mActivity = activity;
        mBinding = DialogInputBinding.inflate(LayoutInflater.from(activity));
    }

    public static InputDialog create(Activity activity) {
        return new InputDialog(activity);
    }

    public static InputDialog create(Activity activity, String title, String detail, String preInput) {
        return new InputDialog(activity).title(title).detail(detail).preInput(preInput);
    }

    public InputDialog title(String title) {
        this.title = title;
        return this;
    }

    public InputDialog detail(String detail) {
        this.detail = detail;
        return this;
    }

    public InputDialog preInput(String preInput) {
        this.preInput = preInput;
        return this;
    }

    public void show() {
        initView();
        initDialog();
    }

    protected void initView() {
        mBinding.detail.setText(detail);
        if (detail == null || detail.isEmpty()) mBinding.detail.setVisibility(View.GONE);
        mBinding.input.setHint(preInput);
    }

    private void initDialog() {
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(
                        mBinding.getRoot().getContext())
                        .setTitle(title)
                        .setView(mBinding.getRoot())
                        .setPositiveButton(R.string.dialog_positive, this::onPositive)
                        .setNegativeButton(R.string.dialog_negative, this::onNegative)
                        .create();
        dialog.getWindow().setDimAmount(0.5f);
        dialog.show();

    }

    public void onPositive(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

    public void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }


}
