package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogTraktBinding;

import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TraktDialog {

    public static void show(Activity activity, String url, String code, DialogInterface.OnClickListener listener) {
        new TraktDialog().create(activity, url, code, listener);
    }

    public void create(Activity activity, String url, String code, DialogInterface.OnClickListener listener) {
        DialogTraktBinding binding = DialogTraktBinding.inflate(LayoutInflater.from(activity));
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot())
                .setPositiveButton(R.string.dialog_positive, listener)
                .setNegativeButton(R.string.dialog_negative, (ndialog, which) -> ndialog.dismiss()).create();
        dialog.getWindow().setDimAmount(0.3f);
        binding.QRCode.setImageBitmap(QRCode.getBitmap(url, 250, 1));
        binding.userCode.setText(code);
        binding.QRCode.setOnClickListener(v -> openUrl(activity, url));
        binding.QRCode.setOnLongClickListener(v -> {
            Util.copy(url);
            Notify.show("URL Copied");
            return true;
        });
        binding.userCode.setOnClickListener(v -> {
            Util.copy(code);
            Notify.show("Code Copied");
        });
        dialog.show();
    }

    private void openUrl(Activity activity, String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public interface Callback {
        void onPositive();
        default void onNegative() {}
    }

}
