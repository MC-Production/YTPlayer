package com.kpstv.youtube;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kpstv.youtube.adapters.SongAdapter;
import com.kpstv.youtube.models.DiscoverModel;
import com.kpstv.youtube.utils.HttpHandler;
import com.kpstv.youtube.utils.SpotifyTrack;
import com.kpstv.youtube.utils.YTMeta;
import com.kpstv.youtube.utils.YTSearch;
import com.kpstv.youtube.utils.YTutils;

import java.util.ArrayList;

import at.huber.youtubeExtractor.YouTubeUriExtractor;

public class SearchActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    CardView recyclerCard;
    TextView trendingText;
    EditText searchEdit;
    ProgressBar progressBar;
    static String SongList;
    private SongAdapter adapter;
    private ArrayList<DiscoverModel> discoverModels;
    private Activity activity; boolean showTrend;
    private AsyncTask<Void, Void, Void> task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        SongList = intent.getStringExtra("data_csv");

        activity = this;
        discoverModels = new ArrayList<>();
        progressBar = findViewById(R.id.progressBar);
        trendingText = findViewById(R.id.trendingNow);
        searchEdit = findViewById(R.id.searchEditText);
        recyclerCard = findViewById(R.id.recyclerCard);
        recyclerView = findViewById(R.id.my_recycler_view);
        mLayoutManager = new LinearLayoutManager(SearchActivity.this);
        recyclerView.setLayoutManager(mLayoutManager);

        searchEdit.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String textToSearch = searchEdit.getText().toString();
                    if (textToSearch.isEmpty())
                        return false;
                    discoverModels.clear();
                    if (textToSearch.contains("open.spotify.com")) {
                        // Spotify url here
                        if (textToSearch.contains("/track/"))
                        { 
                            String id = YTutils.getSpotifyID(textToSearch);
                            if (id!=null)
                                new spotifySearch(textToSearch).execute(); 
                            else Toast.makeText(SearchActivity.this, "Could not extract track ID!", Toast.LENGTH_SHORT).show();
                        }
                        else if (textToSearch.contains("/playlists/")) {
                            showAlert("Note","Current spotify link seems to be a playlist.\n\n" +
                                    "It is recommend to go to the playlist menu from the app where you can manage this url!",true);
                        }else {
                            Toast.makeText(SearchActivity.this, "Seems to invalid spotify url", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        // Normal searching goes here
                        new normalSearch(textToSearch).execute();
                    }
                    return true;
                }
                return false;
            }
        });

        task = new getVirals();
        task.execute();
    }

    class spotifySearch extends AsyncTask<Void,Void,Void> {

        SpotifyTrack track;
        String textToSearch;
        public spotifySearch(String textToSearch) {
            this.textToSearch = textToSearch;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            trendingText.setText("SEARCH RESULTS");
            if (track.getTitle()==null)
                trendingText.setText("NO RESULTS FOUND");
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            adapter = new SongAdapter(discoverModels,activity);
            recyclerView.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String id = YTutils.getSpotifyID(textToSearch);
            if (id!=null) {
                track = new SpotifyTrack(id);
                if (track.getTitle()!=null) {
                    discoverModels.add(new DiscoverModel(
                            track.getTitle(),
                            track.getAuthor(),
                            track.getImageUrl(),
                            track.getYtUrl()
                    ));
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            super.onPreExecute();
        }
    }

    class normalSearch extends AsyncTask<Void,Void,Void> {
        String textToSearch;
        YTSearch ytSearch;
        public normalSearch(String textToSearch) {
            this.textToSearch = textToSearch;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            trendingText.setText("SEARCH RESULTS");
            if (ytSearch.getVideoIDs().size()<=0)
                trendingText.setText("NO RESULTS FOUND");
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            adapter = new SongAdapter(discoverModels,activity);
            recyclerView.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ytSearch = new YTSearch(textToSearch);
            if (ytSearch.getVideoIDs().size()<=0) return null;
            for (String videoID: ytSearch.getVideoIDs()) {
                YTMeta ytMeta = new YTMeta(videoID);
                discoverModels.add(new DiscoverModel(
                        ytMeta.getVideMeta().getTitle(),
                        ytMeta.getVideMeta().getAuthor(),
                        ytMeta.getVideMeta().getImgUrl(),
                        "https://www.youtube.com/watch?v="+videoID
                ));
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            super.onPreExecute();
        }
    }

    class getVirals extends AsyncTask<Void,Void,Void> {

        @Override
        protected void onPostExecute(Void aVoid) {
            if (showTrend)
                trendingText.setText("TOP HIT ON SPOTIFY");
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            adapter = new SongAdapter(discoverModels,activity);
            recyclerView.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);
            trendingText.setVisibility(View.VISIBLE);
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            SharedPreferences preferences = getSharedPreferences("history",MODE_PRIVATE);
            String list = preferences.getString("urls",null);
            if (list!=null) {
                String[] songList = list.split(",");
                if (songList.length>5) {
                    MakeList(songList,5);
                }else if (songList.length<=0) {
                    MakeSpotifyList();
                }else MakeList(songList,songList.length);
                return null;
            }else showTrend=true;

            MakeSpotifyList();
            return null;
        }
    }

    void MakeSpotifyList() {
        if (SongList ==null) {
            HttpHandler handler = new HttpHandler();
            SongList = handler.makeServiceCall("https://spotifycharts.com/viral/global/daily/latest/download");
        }

        String[] csvlines = SongList.split("\r|\n");
        for(int i=1;i<3;i++) {
            String line = csvlines[i];
            String title = line.split(",")[1].replace("\"","");
            String author = line.split(",")[2].replace("\"","");

            SpotifyTrack track = new SpotifyTrack(title,author);
            discoverModels.add(new DiscoverModel(
                    title,author,track.getImageUrl(),track.getYtUrl()
            ));
        }
    }

    void MakeList(String[] songList, int length) {
        for (int i=0;i<length;i++) {
            String ytUrl = songList[i].split("\\|")[0];
            YTMeta ytMeta = new YTMeta(YTutils.getVideoID(ytUrl));
            if (ytMeta.getVideMeta()!=null) {
                discoverModels.add(new DiscoverModel(
                        ytMeta.getVideMeta().getTitle(),
                        ytMeta.getVideMeta().getAuthor(),
                        ytMeta.getVideMeta().getImgUrl(),
                        ytUrl
                ));
            }
        }
    }

    void showAlert(String title, String message, boolean isalert) {
        int icon = android.R.drawable.ic_dialog_info;
        if (isalert) icon = android.R.drawable.ic_dialog_alert;
        new AlertDialog.Builder(SearchActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(icon)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
