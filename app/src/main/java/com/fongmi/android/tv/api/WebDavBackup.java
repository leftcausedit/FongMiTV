package com.fongmi.android.tv.api;

import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.utils.Path;
import com.permissionx.guolindev.PermissionX;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WebDavBackup {
    private String url;
    private String username;
    private String password;
    private Sardine sardine;
    public static class Loader {
        public static volatile WebDavBackup INSTANCE = new WebDavBackup();
    }

    private WebDavBackup() {
        init();
    }

    public static WebDavBackup getInstance() {
        return Loader.INSTANCE;
    }

    public void init() {
        try {
            String urlWithUserInfo = getWebDavUrlWithUserInfo();
            Uri uri = Uri.parse(urlWithUserInfo);

            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            if (path != null && path.endsWith("/")) path = path.substring(0, path.length() - 2);
            String userInfo = uri.getUserInfo();
            this.username = userInfo.split(":")[0];
            this.password = userInfo.split(":")[1];
            this.sardine = new OkHttpSardine();
            this.sardine.setCredentials(this.username, this.password);
            this.url = scheme + "://" + host + ":" + port + path;
            PermissionX.init((FragmentActivity) App.activity()).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getWebDavUrlWithUserInfo() {
        return Setting.getWebDavBackupUrl();
    }

    public void onBackup() {
        if (Setting.isWebDavBackupSwitch()) {
            AppDatabase.backup(new Callback() {
                @Override
                public void success() {
                    App.execute(() -> backup());
                }
            });
        }
    }

    public void backup() {
        try {
            File folder = Path.tv();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                // 递归压缩文件夹中的文件
                addFilesToZip(folder, folder.getName(), zipOutputStream);
            }
            byte[] zipBytes = byteArrayOutputStream.toByteArray();
            sardine.put(this.url + "/backup.zip", zipBytes, "application/zip");
            sardine.put(this.url + "/backup_" + getCurrentDate() + ".zip", zipBytes, "application/zip");
            sardine.put(this.url + "/backup_" + getCurrentDate() + "_" + Build.MODEL + ".zip", zipBytes, "application/zip");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCurrentDate() {
        // 获取当前日期
        Date date = new Date();

        // 将日期格式化为字符串
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return dateFormat.format(date);
    }

    private static void addFilesToZip(File folder, String parentFolder, ZipOutputStream zipOutputStream) throws Exception {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                addFilesToZip(file, parentFolder + "/" + file.getName(), zipOutputStream);
                continue;
            }

            FileInputStream fileInputStream = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
            zipOutputStream.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }

            fileInputStream.close();
        }
    }

    public void onRestore() {
        App.execute(this::restore);
    }

    public void onRestoreHistory() {
        App.execute(this::restoreHistory);
    }

    public void onRestoreWithoutPrefer() {
        App.execute(this::restoreWithoutPrefer);
    }

    public void restore() {
        try {
            InputStream inputStream = sardine.get(this.url + "/backup.zip");
            unzipAndSaveFiles(inputStream);
            restoreToDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreWithoutPrefer() {
        try {
            InputStream inputStream = sardine.get(this.url + "/backup.zip");
            unzipAndSaveFiles(inputStream);
            restoreToDatabaseWithoutPrefer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreHistory() {
        try {
            InputStream inputStream = sardine.get(this.url + "/backup.zip");
            unzipAndSaveFiles(inputStream);
            AppDatabase.restoreHistory(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreToDatabase() {
        PermissionX.init((FragmentActivity) App.activity()).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.restore(new Callback() {
            @Override
            public void success() {
                if (allGranted) {
                    WallConfig.get().init();
                    LiveConfig.get().init().load();
                    VodConfig.get().init().load(new Callback() {
                        @Override
                        public void success() {
                            RefreshEvent.config();
                            RefreshEvent.video();
                        }

                        @Override
                        public void error(String msg) {
                            if (TextUtils.isEmpty(msg) && AppDatabase.getBackup().exists()) restoreToDatabase();
                            else RefreshEvent.empty();
                            RefreshEvent.config();
                            Notify.show(msg);
                        }
                    });
                }
                else RefreshEvent.empty();
            }
        }));
    }

    private void restoreToDatabaseWithoutPrefer() {
        PermissionX.init((FragmentActivity) App.activity()).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.restoreWithoutPrefer(new Callback() {
            @Override
            public void success() {
                if (allGranted) {
                    WallConfig.get().init();
                    LiveConfig.get().init().load();
                    VodConfig.get().init().load(new Callback() {
                        @Override
                        public void success() {
                            RefreshEvent.config();
                            RefreshEvent.video();
                        }

                        @Override
                        public void error(String msg) {
                            if (TextUtils.isEmpty(msg) && AppDatabase.getBackup().exists()) restoreToDatabaseWithoutPrefer();
                            else RefreshEvent.empty();
                            RefreshEvent.config();
                            Notify.show(msg);
                        }
                    });
                }
                else RefreshEvent.empty();
            }
        }));
    }

    private static void unzipAndSaveFiles(InputStream inputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                // 获取文件名
                String filename = zipEntry.getName().substring(3);
                File file = new File(Path.tv(), filename);
                if (!zipEntry.isDirectory()) {
                    // 读取文件内容到字节数组
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zipInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, length);
                        if (zipInputStream.available() == 0) {
                            break;
                        }
                    }
                    byte[] fileBytes = byteArrayOutputStream.toByteArray();

                    // 调用自定义的Path.write方法保存文件
                    Path.write(file, fileBytes);
                } else {
                    file.mkdirs();
                }
                zipInputStream.closeEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
