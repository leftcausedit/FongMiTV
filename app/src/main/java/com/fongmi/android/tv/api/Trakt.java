package com.fongmi.android.tv.api;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.dialog.TraktDialog;
import com.github.catvod.utils.Prefers;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

public class Trakt {
    private static String accessToken = "14665d13b002ff58b4add6ed1daded0350f5679a783f0f7950f4f22c28116b4d";
    private static final String clientId = "8111469842a563dd678e4210ff597eb9265f7e7ed6357eef22fe3373b4a71ac9";
    private static final String clientSerect = "c4e73c927f10ce4a9fedd8ac99eef33338666819d0511f9e793b02108a58387e";
    private static String deviceCode;
    private static String refreshToken = "95b7b7fc6ba6cc171a0e24e0d119c125ff089ed97229348a80ae06fdf8e705";
    public static final String apiUrl = "https://api.trakt.tv";
    private final Context context;
    private static final Handler handler = new Handler();
    private static final int POLLING_INTERVAL = 1000;
    private static final int TOTAL_POLLING_DURATION = 600000;
    private static boolean isCodeActivated = false;

    public Trakt(Context context) {
        this.context = context;
        init();
    }

    public static Trakt create(Context context) {
        return new Trakt(context);
    }
    public void init() {
        accessToken = getAccessToken();
        refreshToken = getRefreshToken();
        if (accessToken.isEmpty()) {
            initCode();
        }
        testToken();
    }
    private static String getAccessToken() {
        return Prefers.getString("trakt_access_token", "");
    }
    private static void putAccessToken(String token) {
        Prefers.put("trakt_access_token", token);
    }
    private  static String getRefreshToken() {
        return Prefers.getString("refresh_token", "");
    }
    private static void putRefreshToken(String token) {
        Prefers.put("refresh_token", token);
    }

    private void testToken() {
        if (getAccessToken().equals("")) return;
        OkHttp.client().newCall(new Request.Builder().url(apiUrl + "/users/settings").headers(Headers.of(getAccessHeaders())).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.code() != 200) initCode();
            }
        });
    }

    private static Map<String, String> getAccessHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("trakt-api-version", "2");
        headers.put("trakt-api-key", clientId);
        return headers;
    }
    public static Headers getTraktHeaders() {
        return new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("Authorization", "Bearer " + accessToken)
                .add("trakt-api-version", "2")
                .add("trakt-api-key", clientId)
                .build();
    }

    public interface TraktCallback {
        void onSuccess(JSONObject result);
        void onError(Throwable throwable);
    }

    public static void scrobbleStart(String title, String type, String year, String tmdbId, int episodePos, float progress, Callback callback) {
        toScrobble(title, type, year, tmdbId, episodePos, progress, "start", callback);
    }
    public static void scrobblePause(String title, String type, String year, String tmdbId, int episodePos, float progress, Callback callback) {
        toScrobble(title, type, year, tmdbId, episodePos, progress, "pause", callback);
    }
    public static void scrobbleStop(String title, String type, String year, String tmdbId, int episodePos, float progress, Callback callback) {
        toScrobble(title, type, year, tmdbId, episodePos, progress, "stop", callback);
    }

    public static void toScrobble(String title, String type, String year, String tmdbId, int episodePos, float progress, String scrobbleType, Callback callback) {

        findItem(title, type, year, tmdbId, episodePos, new TraktCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                System.out.println("Trakt findItem: have item");
                callback.success(result);
                scrobble(result, progress, scrobbleType);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Trakt findItem: Result is null");
            }
        });
    }
    public static void scrobble(JSONObject src, float progress, String scrobbleType) {
        if (src == null) return;
        JSONObject item = new JSONObject();
        String srcType = src.optString("type");

        if (srcType.equals("movie")) {
            try {
                item.put(srcType, src.optJSONObject(srcType));
                item.put("progress", progress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (srcType.equals("episode")) {
            try {
                item.put(srcType, src.optJSONObject(srcType));
                item.put("progress", progress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // App.post(() -> Toast.makeText(App.get(), "Trakt " + src.optJSONObject(srcType).optString("title") + " " + (!src.optJSONObject(srcType).has("year") ? "" : src.optJSONObject(srcType).optString("year")), Toast.LENGTH_LONG).show());

        Request request = new Request.Builder()
                .url(apiUrl + "/scrobble/" + scrobbleType)
                .headers(Headers.of(getAccessHeaders()))
                .post(RequestBody.create(item.toString(), MediaType.parse("application/json")))
                .build();
        OkHttp.client().newCall(request).enqueue(new Callback()); // success code 201
    }


    private static Map.Entry<String, String> parseTitle(String showTitle) {
        String patternString = "^(.+?)(?:第(.+?)季)?$";

        Map<String, String> chineseToArabic = new HashMap<>();
        chineseToArabic.put("一", "1");
        chineseToArabic.put("二", "2");
        chineseToArabic.put("三", "3");
        chineseToArabic.put("四", "4");
        chineseToArabic.put("五", "5");
        chineseToArabic.put("六", "6");
        chineseToArabic.put("七", "7");
        chineseToArabic.put("八", "8");
        chineseToArabic.put("九", "9");
        chineseToArabic.put("十", "10");
        chineseToArabic.put("十一", "11");
        chineseToArabic.put("十二", "12");
        chineseToArabic.put("十三", "13");

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(showTitle);

        if (matcher.matches()) {
            String title = matcher.group(1);
            String season = matcher.group(2) != null ? chineseToArabic.get(matcher.group(2)) : "1";
            return new AbstractMap.SimpleEntry<>(title, season);
        } else {
            return new AbstractMap.SimpleEntry<>(showTitle, "1");
        }
    }

    public static void getTMDBResult(String title, String type, TraktCallback callback) {
        type = parseTypeTMDB(type);
//        if (type.equals("tv"))
        title = parseTitle(title).getKey();

        Request request = new Request.Builder()
                .url("https://api.themoviedb.org/3/search/" + type + "?query=" + title + "&include_adult=false&language=zh-CN&page=1")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlNjU3ZTY3ZTU3N2FkMTliM2U0NDk2YTM5YmUxMWQwNSIsInN1YiI6IjYzZDU0YzkxMTJiMTBlMDA5M2U3OGZjOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.KPdFvId1UDpbqu9CvqYC2v4FrTodBII_9EOLlQUmTSU")
                .build();

        OkHttp.cachedClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseBodyStr = response.body().string();
                    try {
                        JSONArray tmdbResultArray = new JSONObject(responseBodyStr).optJSONArray("results");
                        if (tmdbResultArray != null && tmdbResultArray.length() > 0){
                            JSONObject tmdbResult = tmdbResultArray.optJSONObject(0);
                            callback.onSuccess(tmdbResult);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private static String parseTypeTMDB(String type) {
        switch (type) {
            case "movie": case "movies":
                type = "movie"; break;
            case "series": case "show": case "shows": case "tv":
                type = "tv"; break;
            default:
                type = "multi"; break;
        }
        return type;
    }

    private static String parseTypeTrakt(String type) {
        switch (type) {
            case "movie": case "movies":
                type = "movie"; break;
            case "series": case "show": case "shows": case "tv":
                type = "show"; break;
            default:
                type = "movie,show"; break;
        }
        return type;
    }

    public static void findItem(String title, String type, String year, String tmdbId, int episodePos, TraktCallback callback) {
        if (!tmdbId.isEmpty()){
            getItemByTMDBId(tmdbId, type, title, episodePos, new TraktCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onError(Throwable throwable) {
                }
            });
        } else {
            getTMDBResult(title, type, new TraktCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    String tmdbId = Integer.toString(result.optInt("id"));
                    String type = result.optString("media_type");
                    getItemByTMDBId(tmdbId, type, title, episodePos, new TraktCallback() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            callback.onSuccess(result);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                }
            });
        }
    }
    public static void getItemByTMDBId(String tmdbId, String type, String title, int episodePos, TraktCallback callback) {
        String season;
        // keep empty string as empty
//        if (!type.isEmpty())
        type = parseTypeTrakt(type);
        String finalType = type;
        season = parseTitle(title).getValue();

        String finalSeason = season;
        OkHttp.cachedClient().newCall(new Request.Builder().url(apiUrl + "/search/tmdb/" + tmdbId + "?type=" + type).get()
                .addHeader("Content-Type", "application/json")
                .addHeader("trakt-api-version", "2")
                .addHeader("trakt-api-key", clientId).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseBodyStr = response.body().string();
                    JSONObject result;
                    try {
                        result = new JSONArray(responseBodyStr).optJSONObject(0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        callback.onError(e);
                        return;
                    }
                    if (result == null) return;
                    String finalType = result.optString("type", "");
                    if (finalType.equals("show")) {
                        String slug = result.optJSONObject(finalType).optJSONObject("ids").optString("slug");
                        getEpisodeItem(slug, finalSeason, episodePos, new TraktCallback() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                callback.onSuccess(result);
                            }
                            @Override
                            public void onError(Throwable throwable) {
                                callback.onError(throwable);
                            }
                        });
                    } else if (!finalType.isEmpty()) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(new Throwable());
                    }
                }
            }
        });
    }

//    public static void oldfindItem(String title, String type, String year, int episodePos, boolean useYear, TraktCallback callback) {
//        String url, season = "1";
//        if (type.equals("movie") || type.equals("movies")) {
//            type = "movie";
//        } else if (type.equals("series") || type.equals("show") || type.equals("shows") || type.equals("tv")) {
//            title = parseShowTitle(title).getKey();
//            season = parseShowTitle(title).getValue();
//            type = "show";
//        } else if (type.equals("")){
//            type = "movie,show";
//        }
//        else {
//            callback.onError(new IllegalArgumentException("Invalid type"));
//            return;
//        }
//
//        url = apiUrl + "/search/" + type + "?query=" + title + (useYear ? " " + year : "");
//        String finalTitle = title;
//        String finalType = type;
//        String finalSeason = season;
//        OkHttp.cachedClient().newCall(new Request.Builder().url(url).get()
//                .addHeader("Content-Type", "application/json")
//                .addHeader("trakt-api-version", "2")
//                .addHeader("trakt-api-key", clientId).build())
//                .enqueue(new Callback() {
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (response.code() == 200) {
//                    try {
//                        String responseBody = response.body().string();
//                        JSONObject findResponse = new JSONArray(responseBody).optJSONObject(0);
//                        if (findResponse != null) {
//                            if (finalType.equals("show")) {
//                                String slug = findResponse.optJSONObject(finalType).optJSONObject("ids").optString("slug");
//                                getEpisodeItem(slug, finalSeason, episodePos, new TraktCallback() {
//                                    @Override
//                                    public void onSuccess(JSONObject result) {
//                                        callback.onSuccess(result);
//                                    }
//                                    @Override
//                                    public void onError(Throwable throwable) {
//                                        callback.onError(throwable);
//                                    }
//                                });
//                            } else {
//                                callback.onSuccess(findResponse);
//                            }
//                        } else {
//                            if (useYear) findItem(finalTitle, finalType, year, episodePos, new TraktCallback() {
//                                @Override
//                                public void onSuccess(JSONObject result) {
//                                    callback.onSuccess(result);
//                                }
//
//                                @Override
//                                public void onError(Throwable throwable) {
//                                    callback.onError(throwable);
//                                }
//                            });
//                            else callback.onSuccess(null);
//                        }
//                    } catch (JSONException e) {
//                        callback.onError(e);
//                    }
//                }
//                else callback.onSuccess(null);
//            }
//        });
//    }

    private static void getEpisodeItem(String slug, String season, int episodePos, TraktCallback callback) {
        String url = apiUrl + "/shows/" + slug + "/seasons/" + season + "/episodes/" + episodePos;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("trakt-api-version", "2")
                .addHeader("trakt-api-key", clientId)
                .build();
        OkHttp.cachedClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject finalItem = new JSONObject();
                        finalItem.put("episode", new JSONObject(responseBody));
                        finalItem.put("type", "episode");
                        callback.onSuccess(finalItem);
                    } catch (JSONException e) {
                        callback.onError(e);
                    }
                } else callback.onError(new Throwable());
            }
        });
    }

    private static void initToken() {
        isCodeActivated = false;
        requestToken();
        // 启动轮询
        handler.postDelayed(new Runnable() {
            private final long startTime = System.currentTimeMillis();
            @Override
            public void run() {
                // 在轮询的开始处进行条件判断
                if (System.currentTimeMillis() - startTime >= TOTAL_POLLING_DURATION || isCodeActivated) {
                    return;
                }
                // 执行轮询的操作
                requestToken();

                // 继续下一次轮询
                handler.postDelayed(this, POLLING_INTERVAL);
            }
        }, POLLING_INTERVAL);
    }


    private static void requestToken() {
        String jsonBody = "{"
                + "\"code\": \"" + deviceCode + "\","
                + "\"client_id\": \"" + clientId + "\","
                + "\"client_secret\": \"" + clientSerect +"\""
                + "}";
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.trakt.tv/oauth/device/token")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();
        OkHttp.client().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    try {
                        JSONObject tokenResponse = new JSONObject(response.body().string());
                        accessToken = tokenResponse.optString("access_token");
                        putAccessToken(accessToken);
                        refreshToken = tokenResponse.optString("refresh_token");
                        putRefreshToken(refreshToken);

                        isCodeActivated = true;
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void initCode() {
        String jsonBody = "{"
                + "\"client_id\": \"" + clientId +"\""
                + "}";
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.trakt.tv/oauth/device/code")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();
        OkHttp.client().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.code() == 200 && response.body() != null) {
                        JSONObject codeResponse = new JSONObject(response.body().string());
                        String userCode = codeResponse.optString("user_code");
                        String veriUrl = codeResponse.optString("verification_url");
                        deviceCode = codeResponse.optString("device_code");
                        App.post(() -> {
                            // 更新 UI 的代码
                            TraktDialog.show((Activity) context, veriUrl, userCode);
                            initToken();
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
