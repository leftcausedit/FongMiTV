package com.fongmi.android.tv.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.WebDavBackup;
import com.fongmi.android.tv.databinding.FragmentSettingCustomBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SettingCustomFragment extends BaseFragment {

    private FragmentSettingCustomBinding mBinding;
    private String[] size;

    public static SettingCustomFragment newInstance() {
        return new SettingCustomFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingCustomBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
        mBinding.danmuSyncText.setText(getSwitch(Setting.isDanmuSync()));
        mBinding.speedText.setText(getSpeedText());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
        mBinding.homeChangeConfigText.setText(getSwitch(Setting.isHomeChangeConfig()));
        mBinding.webDavBackupSwitchText.setText(getSwitch(Setting.isWebDavBackupSwitch()));
        mBinding.webDavBackupUrlText.setText(Setting.getWebDavBackupUrl());
    }

    @Override
    protected void initEvent() {
        mBinding.title.setOnLongClickListener(this::onTitle);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.danmuSync.setOnClickListener(this::setDanmuSync);
        mBinding.speed.setOnClickListener(this::setSpeed);
        mBinding.speed.setOnLongClickListener(this::resetSpeed);
        mBinding.incognito.setOnClickListener(this::setIncognito);
        mBinding.aggregatedSearch.setOnClickListener(this::setAggregatedSearch);
        mBinding.homeChangeConfig.setOnClickListener(this::setHomeChangeConfig);
        mBinding.webDavBackupSwitch.setOnClickListener(this::setWebDavBackupSwitch);
        mBinding.webDavBackupSwitch.setOnLongClickListener(this::longClickWebDavBackupSwitch);
        mBinding.webDavBackupUrl.setOnClickListener(this::setWebDavBackupUrl);
        mBinding.webDavBackupUrl.setOnLongClickListener(this::webDavRestore);
    }

    private boolean onTitle(View view) {
        mBinding.danmuSync.setVisibility(View.VISIBLE);
        return true;
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Setting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Setting.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setDanmuSync(View view) {
        Setting.putDanmuSync(!Setting.isDanmuSync());
        mBinding.danmuSyncText.setText(getSwitch(Setting.isDanmuSync()));
    }

    private String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", Setting.getPlaySpeed());
    }

    private void setSpeed(View view) {
        float speed = Setting.getPlaySpeed();
        float addon = speed >= 2 ? 1.0f : 0.1f;
        speed = speed >= 5 ? 0.2f : Math.min(speed + addon, 5.0f);
        Setting.putPlaySpeed(speed);
        mBinding.speedText.setText(getSpeedText());
    }

    private boolean resetSpeed(View view) {
        Setting.putPlaySpeed(1.0f);
        mBinding.speedText.setText(getSpeedText());
        return true;
    }

    private void setIncognito(View view) {
        Setting.putIncognito(!Setting.isIncognito());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
    }

    private void setAggregatedSearch(View view) {
        Setting.putAggregatedSearch(!Setting.isAggregatedSearch());
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
    }

    private void setHomeChangeConfig(View view) {
        Setting.putHomeChangeConfig(!Setting.isHomeChangeConfig());
        mBinding.homeChangeConfigText.setText(getSwitch(Setting.isHomeChangeConfig()));
        RefreshEvent.config();
    }

    private void setWebDavBackupSwitch(View view) {
        Setting.putWebDavBackupSwitch(!Setting.isWebDavBackupSwitch());
        mBinding.webDavBackupSwitchText.setText(getSwitch(Setting.isWebDavBackupSwitch()));
    }

    private void setWebDavBackupUrl(View view) {
        // 创建一个输入框
        final EditText input = new EditText(this.requireContext());
        input.setText(Setting.getWebDavBackupUrl());

        // 创建一个对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this.requireContext());
        builder.setTitle("WebDAV 备份 URL");
        builder.setMessage("请输入 WebDAV 备份 URL:");
        builder.setView(input);

        // 设置确认按钮
        builder.setPositiveButton("确认", (dialog, which) -> {
            // 获取输入框中的值
            String url = input.getText().toString();

            // 保存 URL 到 Preferences
            Setting.putWebDavBackupUrl(url);
            mBinding.webDavBackupUrlText.setText(Setting.getWebDavBackupUrl());
            WebDavBackup.getInstance().init();
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", null);

        // 显示对话框
        builder.show();
    }

    private boolean webDavRestore(View view) {
        WebDavBackup.getInstance().onRestore();
        return true;
    }

    private boolean longClickWebDavBackupSwitch(View view) {
        new MaterialAlertDialogBuilder(this.requireContext())
                .setMessage("选择操作")
                .setNegativeButton("备份", (dialog, which) -> {
                    WebDavBackup.getInstance().onBackup();
                    dialog.dismiss();
                })
                .setPositiveButton("恢复全部（无prefer）", (dialog, which) -> {
                    WebDavBackup.getInstance().onRestoreWithoutPrefer();
                    dialog.dismiss();
                })
                .setNeutralButton("恢复历史（合并）", (dialog, which) -> {
                    WebDavBackup.getInstance().onRestoreHistory();
                    dialog.dismiss();
                })
                .show();
        return true;
    }

}
