package com.fongmi.android.tv.ui.activity;

import static com.fongmi.android.tv.utils.Util.substring;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.Trakt;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.adapter.FlagAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DetailActivity extends BaseActivity implements FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener {

    private ActivityDetailBinding mBinding;
    private EpisodeAdapter mEpisodeAdapter;
    private SiteViewModel mViewModel;
    private FlagAdapter mFlagAdapter;
    private History mHistory;
    private JSONObject tmdbItem;
    private JSONObject traktItem;
    private Future<?> futureGetTraktData;
    private Future<String> futureGetDoubanId;
    private ExecutorService executor;
    private Future<JSONObject> futureGetOMDBItem;
    private Future<String> futureGetIMDBIdFromTMDB;


    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, null, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark) {
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.putExtra("mark", mark);
        intent.putExtra("name", name);
        intent.putExtra("pic", pic);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    private String getName() {
        return getIntent().getStringExtra("name");
    }

    private String getPic() {
        return getIntent().getStringExtra("pic");
    }

    private String getMark() {
        return getIntent().getStringExtra("mark");
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private String getId() {
        return getIntent().getStringExtra("id");
    }

    private String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId()).concat(AppDatabase.SYMBOL) + VodConfig.getCid();
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private Flag getFlag() {
        return mFlagAdapter.getActivated();
    }

    private Episode getEpisode() {
        return mEpisodeAdapter.getActivated();
    }

    private boolean isFromCollect() {
        return getCallingActivity() != null && getCallingActivity().getShortClassName().contains(CollectActivity.class.getSimpleName());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        mBinding.progressLayout.showProgress();
        setRecyclerView();
        setViewModel();
        getDetail();
    }

    @Override
    protected void initEvent() {
    }

    private void setRecyclerView() {
        mBinding.flag.setHasFixedSize(true);
        mBinding.flag.setItemAnimator(null);
        mBinding.flag.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.flag.setAdapter(mFlagAdapter = new FlagAdapter(this));
        mBinding.episode.setHasFixedSize(true);
        mBinding.episode.setItemAnimator(null);
        mBinding.episode.setAdapter(mEpisodeAdapter = new EpisodeAdapter(this, ViewType.VERT));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, this::setDetail);
        mViewModel.player.observe(this, new Observer<Result>() {
            @Override
            public void onChanged(Result result) {

            }
        });
    }

    private void getDetail() {
        mViewModel.execute(mViewModel.result, () -> {
            Vod vod = new Vod();
            vod.setVodId(getId());
            vod.setVodName(getName());
            vod.setVodPic(getPic());
            return Result.vod(vod);
        });
//        mViewModel.detailContent(getKey(), getId());
    }

    private void setDetail(Result result) {
        if (result.getList().isEmpty()) setEmpty();
        else setDetail(result.getList().get(0));
        Notify.show(result.getMsg());
    }

    private void setEmpty() {
        if (isFromCollect()) {
            finish();
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        mBinding.progressLayout.showEmpty();
    }

    private void setDetail(Vod item) {

        mBinding.progressLayout.showContent();
        mBinding.name.setText(item.getVodName(getName()));
//        setText(mBinding.site, R.string.detail_site, getSite().getName());
//        setText(mBinding.content, 0, Html.fromHtml(item.getVodContent()).toString());
//        setText(mBinding.director, R.string.detail_director, Html.fromHtml(item.getVodDirector()).toString());
//        App.execute(this::getDataFromTMDB);
        initData();
        loadTMDBData();
        ImgUtil.rect(item.getVodName(), item.getVodPic(getPic()), mBinding.pic);
        mFlagAdapter.addAll(item.getVodFlags());
        checkHistory(item);
        checkFlag(item);
        checkKeepImg();
    }

    private void initData() {
        // get tmdb data
        executor = Executors.newFixedThreadPool(2);
        try {
            executor.submit(this::getDataFromTMDB).get(Constant.TIMEOUT_VOD, TimeUnit.MILLISECONDS);
            futureGetTraktData = executor.submit(this::getDataFromTrakt);
            futureGetDoubanId = executor.submit(this::getDoubanIdFromIMDBID);
            futureGetIMDBIdFromTMDB = executor.submit(this::getIMDBIdFromTMDB);
            futureGetOMDBItem = executor.submit(this::getOMDBItem);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void loadTMDBData() {
        ImgUtil.rect(isMovie() ? tmdbItem.optString("title") : tmdbItem.optString("name"), "https://image.tmdb.org/t/p/original" + tmdbItem.optString("backdrop_path"), mBinding.landscape);
        mBinding.contentText.setText(tmdbItem.optString("overview"));
        mBinding.type.setText(combineTMDBGenres(tmdbItem));
        mBinding.date.setText(isMovie() ? tmdbItem.optString("release_date") : tmdbItem.optString("first_air_date"));
        String rating = String.format(Locale.CHINA,"⭐ %.1f" ,tmdbItem.optDouble("vote_average"));
        String hit = "\uD83D\uDD25 " + tmdbItem.optInt("popularity");
        String rating_hit = rating + "  " + hit;
        mBinding.rating.setText(rating_hit);
        mBinding.originalName.setText(isMovie() ? tmdbItem.optString("original_title") : tmdbItem.optString("original_name"));

        mBinding.playButton.setOnClickListener(view -> onClickPlayButton());
        mBinding.imdbLink.setOnClickListener(view -> onClickIMDBLink());
        mBinding.tmdbLink.setOnClickListener(view -> onClickTMDBLink());
        mBinding.traktLink.setOnClickListener(view -> onClickTraktLink());
        mBinding.doubanLink.setOnClickListener(view -> onClickDoubanLink());

        App.execute(() -> {
            try {
                mBinding.imdbRating.setText(getIMDBRatingFromOMDBItem(futureGetOMDBItem.get()));
                mBinding.imdbVotes.setText(getIMDBVotesFromOMDBItem(futureGetOMDBItem.get()));
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void onClickPlayButton() {
        try {
            VideoActivity.start(getActivity(), "Douban", futureGetDoubanId.get(), getVodName(), tmdbItem.optString("backdrop_path"), getMediaType(), getId(), null, false);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getMediaType() {
        return isMovie() ? "movie" : "tv";
    }

    private String getDoubanIdFromIMDBID() {
        try {
            futureGetTraktData.get();
            String url = "https://movie.douban.com/j/subject_suggest?q=" + getIMDBIdFromTraktItemWithChild();
            JSONArray item= new JSONArray(OkHttp.string(url));
            return item.optJSONObject(0).optString("id");
        } catch (ExecutionException | InterruptedException | JSONException e) {
            return "";
        }
    }

    private void onClickDoubanLink() {
        try {
            String id = futureGetDoubanId.get();
            String url = "https://movie.douban.com/subject/" + id;
            openExternalLink(url);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void onClickIMDBLink() {
        try {
            openExternalLink(getIMDBLinkFromOMDBItem(futureGetOMDBItem.get()));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getVodName() {
        return isMovie() ? tmdbItem.optString("title") : tmdbItem.optString("name");
    }
    private void onClickTMDBLink() {
        String url = "https://www.themoviedb.org/" + (isMovie() ? "movie" : "tv") + "/" + tmdbItem.optString("id");
        openExternalLink(url);
    }

    private void onClickTraktLink() {
        try {
            futureGetTraktData.get(Constant.TIMEOUT_VOD, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        String url = "https://trakt.tv/" + (isMovie() ? "movies" : "shows") + "/" + getSlugFromTraktItemWithChild();
        openExternalLink(url);
    }

    private boolean isMovie() {
        return tmdbItem.has("title");
    }

    private String getIMDBIdFromTMDB() {
        String url = String.format("https://api.themoviedb.org/3/%s/%s/external_ids", getMediaType(), getId());
        try {
            ResponseBody body = OkHttp.newCall(url, getTMDBHeaders()).execute().body();
            String rsp = body == null ? "" : body.string();
            return new JSONObject(rsp).optString("imdb_id");
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String getIMDBRatingFromOMDBItem(JSONObject item) {
        return item.optString("imdbRating");
    }

    private String getIMDBVotesFromOMDBItem(JSONObject item) {
        return item.optString("imdbVotes");
    }

    private String getIMDBIdFromOMDBItem(JSONObject item) {
        return item.optString("imdbID");
    }

    private String getIMDBLinkFromOMDBItem(JSONObject item) {
        return "https://www.imdb.com/title/" + getIMDBIdFromOMDBItem(item);
    }

    private JSONObject getOMDBItem() {
        try {
            String url = "http://www.omdbapi.com/?apikey=53e26309&plot=full&i=" + futureGetIMDBIdFromTMDB.get();
            String rsp = OkHttp.string(url);
            return new JSONObject(rsp);
        } catch (JSONException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private void openExternalLink(String externalLink) {
        Uri uri = Uri.parse(externalLink);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        if (intent.resolveActivity(getPackageManager()) != null)
        startActivity(intent);
    }

    private void getDataFromTrakt() {
        String url = Trakt.apiUrl + "/search/tmdb/" + tmdbItem.optString("id") + "?type=" + (isMovie() ? "movie" : "show");
        try {
            ResponseBody body = OkHttp.newCall(url, Trakt.getTraktHeaders()).execute().body();
            String response = body == null ? "" : body.string();
            this.traktItem = new JSONArray(response).optJSONObject(0);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSlugFromTraktItemWithChild() {
        String type = traktItem.optString("type");
        JSONObject typeItem = traktItem.optJSONObject(type);
        JSONObject item = typeItem == null ? null : typeItem.optJSONObject("ids");
        return item != null && item.has("slug") ? item.optString("slug") : "";
    }

    private String getIMDBIdFromTraktItemWithChild() {
        String type = traktItem.optString("type");
        JSONObject item = Objects.requireNonNull(traktItem.optJSONObject(type)).optJSONObject("ids");
        return item != null && item.has("imdb") ? item.optString("imdb") : "";
    }
    private void getDataFromTMDB() {
        try {
            String url;
            if (getMark().startsWith("🎬")) url = "https://api.themoviedb.org/3/movie/" + getId() + "?language=zh-CN";
            else if (getMark().startsWith("📺")) url = "https://api.themoviedb.org/3/tv/" + getId() + "?language=zh-CN";
            else {return;}
            ResponseBody body = OkHttp.newCall(url, getTMDBHeaders()).execute().body();
            String response = body == null ? "" : body.string();
            this.tmdbItem = new JSONObject(response);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private String combineTMDBGenres (JSONObject item) {
        try {
            JSONArray genres = item.optJSONArray("genres");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < (genres == null ? 0 :genres.length()); i++) {
                String genre = genres.optJSONObject(i).optString("name");
                genre = genreTranslate(genre);
                builder.append(genre).append(" ");
            }
            return substring(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String genreTranslate(String key) {
        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("Sci-Fi & Fantasy", "科幻");

        return dictionary.get(key) == null ? key : dictionary.get(key);
    }

    private Headers getTMDBHeaders() {
        return new Headers.Builder()
                .add("accept", "application/json")
                .add("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlNjU3ZTY3ZTU3N2FkMTliM2U0NDk2YTM5YmUxMWQwNSIsInN1YiI6IjYzZDU0YzkxMTJiMTBlMDA5M2U3OGZjOCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.KPdFvId1UDpbqu9CvqYC2v4FrTodBII_9EOLlQUmTSU")
                .build();
    }

    private void setText(TextView view, int resId, String text) {
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setText(resId > 0 ? getString(resId, text) : text);
        view.setTag(text);
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        if (!TextUtils.isEmpty(getMark())) mHistory.setVodRemarks(getMark());
    }

    private History createHistory(Vod item) {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setCid(VodConfig.getCid());
        history.setVodPic(item.getVodPic());
        history.setVodName(item.getVodName());
        history.findEpisode(item.getVodFlags());
        return history;
    }

    private void checkFlag(Vod item) {
        boolean empty = item.getVodFlags().isEmpty();
        mBinding.flag.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            ErrorEvent.flag();
        } else {
            onItemClick(mHistory.getFlag());
            if (mHistory.isRevSort()) reverseEpisode(true);
        }
    }

    private void reverseEpisode(boolean scroll) {
        mFlagAdapter.reverse();
        setEpisodeAdapter(getFlag().getEpisodes());
        if (scroll) mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
    }

    private void setEpisodeAdapter(List<Episode> items) {
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        mEpisodeAdapter.addAll(items);
    }

    private void checkKeepImg() {
        //mBinding.keep.setImageResource(Keep.find(getHistoryKey()) == null ? R.drawable.ic_control_keep_off : R.drawable.ic_control_keep_on);
    }

    @Override
    public void onItemClick(Flag item) {
        if (item.isActivated()) return;
        mFlagAdapter.setActivated(item);
        mBinding.flag.scrollToPosition(mFlagAdapter.getPosition());
        setEpisodeAdapter(item.getEpisodes());
    }

    @Override
    public void onItemClick(Episode item) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
