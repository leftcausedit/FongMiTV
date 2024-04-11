package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogTraktBinding;

import com.fongmi.android.tv.utils.QRCode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TraktDialog {


    public static void show(Activity activity, String url, String code, DialogInterface.OnClickListener listener) {
        new TraktDialog().create(activity, url, code);
    }

    public static void show(Activity activity, String url, String code) {
        new TraktDialog().create(activity, url, code);
    }

    public void create(Activity activity, String url, String code) {
        DialogTraktBinding binding = DialogTraktBinding.inflate(LayoutInflater.from(activity));
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0.3f);
        binding.QRCode.setImageBitmap(QRCode.getBitmap(url, 250, 1));
        binding.userCode.setText(code);
        dialog.show();
    }

}
