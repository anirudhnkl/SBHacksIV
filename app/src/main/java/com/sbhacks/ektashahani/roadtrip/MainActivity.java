package com.sbhacks.ektashahani.roadtrip;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import com.sbhacks.ektashahani.roadtrip.googlecloud.IResults;
import com.sbhacks.ektashahani.roadtrip.googlecloud.MicrophoneStreamRecognizeClient;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback, IResults {

    private static final String CLIENT_ID = "fd8cdcd290f64bb28d37a246758a4f5f";
    private static final String REDIRECT_URI = "http://google.com";

    private Player mPlayer;
    private static final int REQUEST_CODE = 1337;

    private Button mSkipBtn;

    SpotifyApi api = new SpotifyApi();
    List<PlaylistTrack> saved;
    AudioFeaturesTracks allTrackFeatures;
    List<List<AudioFeaturesTrack>> allMoods = new ArrayList<List<AudioFeaturesTrack>>();
    List<AudioFeaturesTrack> tired = new ArrayList<>();
    List<AudioFeaturesTrack> energetic = new ArrayList<>();
    List<AudioFeaturesTrack> chill = new ArrayList<>();
    List<AudioFeaturesTrack> sad = new ArrayList<>();


    private static final String[] MOODS = new String[] {
            "Happy", "Sad", "Tired", "Energetic", "Chill"};

    private String currentMood = "";
    private String unfilteredMood = "";
    List<AudioFeaturesTrack> currentPlaylist;
    private int currentPos;
    private int playlistIndex = 0;
    //int currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int currentTime = Calendar.getInstance().get(Calendar.MINUTE);

    TextToSpeech t1;

    private IResults Self = this;

    private Thread runner = new Thread() {

        public void run(){

            try {
                Log.d("Main Activity", "Start");
                MicrophoneStreamRecognizeClient client;
                client = new MicrophoneStreamRecognizeClient(getResources().openRawResource(R.raw.credential), Self);
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        //Authentication
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "user-library-read"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        //Create mood playlists
        allMoods.add(tired);
        allMoods.add(energetic);
        allMoods.add(chill);
        allMoods.add(sad);

        //Create Text to Speech Object
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                    t1.setPitch(1.3f);
                    t1.setSpeechRate(1.1f);
                }
            }
        });

         mSkipBtn = (Button)findViewById(R.id.btn_skip);
         mSkipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.skipToNext(null);
            }
        });

        //Grab mood from UI
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, MOODS);
        final AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.moodEnterView);
        textView.setAdapter(adapter);
        textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(arg1.getApplicationWindowToken(), 0);
                currentMood = (String) arg0.getItemAtPosition(arg2);

                if(currentMood.equals("Tired")) { currentPos = 0; }
                else if(currentMood.equals("Energetic")) { currentPos = 1; }
                else if(currentMood.equals("Chill")) { currentPos = 2; }
                else { currentPos = 3; }

                currentPlaylist = allMoods.get(currentPos);
                mPlayer.playUri(null, currentPlaylist.get(0).uri, 0, 0);
            }
        });

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            int RECORD_AUDIO = 666;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO);
        }

        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            int ACCESS_NETWORK_STATE = 333;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                    ACCESS_NETWORK_STATE);
        }
//
//        System.out.print("hi");
//
//
//        final Button startButton = (Button) findViewById(R.id.button);
//        startButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                runner.start();
//            }
//        });
//
//        final Button stop = (Button) findViewById(R.id.stopStreaming);
//        stop.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                try {
//                    Log.d("Main Activity", "Stop");
//                    runner.join();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        // The next 19 lines of the code are what you need to copy & paste! :)
        if (requestCode == REQUEST_CODE) {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                api.setAccessToken(response.getAccessToken());

                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        playlistIndex++;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        if(min != currentTime) {
            mPlayer.pause(null);
            currentTime = min;
        }

        if(hour > 17) {
            currentMood = "Tired";
            currentPlaylist = tired;
            playlistIndex = 0;
        }

        if(playlistIndex == currentPlaylist.size()) {
            if(currentPos != 3) {
                currentPos++;
            } else {
                currentPos = 0;
            }

            currentPlaylist = allMoods.get(currentPos);
            playlistIndex = 0;
        }

        mPlayer.queue(null, currentPlaylist.get(playlistIndex).uri);

        switch (playerEvent) {
            case kSpPlaybackNotifyPause : {
                String toSpeak = "How are you feeling?";
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                startTranscription(); //rucha
                //SWITCH PLAYLIST BASED ON RESPONSE AND RESUME PLAYER
            }
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    private void startTranscription()
    {
        runner.start();
        System.out.println("\n\nhi");
        try
        {
            Log.d("Main Activity", "Stop");
            runner.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        System.out.println("\n\nhi");

        for (String word : unfilteredMood.split("\\s+"))
        {
            for (String moods : MOODS)
            {
                if (word.equals(moods))
                {
                    currentMood = word;
                    return;
                }
            }
        }

        System.out.println("\n\n"+currentMood);
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");

        // This is the line that plays a song.
//        mPlayer.playUri(null, "spotify:track:4bJzIbze97tZwCxxMw4hx9", 0, 0);

        final SpotifyService spotify = api.getService();

        //03LHtXxWrdOdS7UQQy2E6Y

        spotify.getPlaylistTracks("1252333561", "03LHtXxWrdOdS7UQQy2E6Y", new Callback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                saved = playlistTrackPager.items;

                String allTrackURIs = "";

                for(int i = 0; i < saved.size(); i++) {
                    allTrackURIs += saved.get(i).track.uri.substring(14);
                    if(i != saved.size() - 1) { allTrackURIs += ","; }
                }

                System.out.println(allTrackURIs);

                spotify.getTracksAudioFeatures(allTrackURIs, new Callback<AudioFeaturesTracks>() {
                    @Override
                    public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                        System.out.println("playlists fetched");
                        allTrackFeatures = audioFeaturesTracks;
                        sortPlaylist(allTrackFeatures);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        System.out.println(error.getMessage());
                    }
                });

            }

            @Override
            public void failure(RetrofitError error) {
                System.out.println(error.getMessage());
            }
        });
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error var1) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mPlayer != null) {
            mPlayer.destroy();
        }
    }

    public void sortPlaylist(AudioFeaturesTracks featureObj)
    {
        List<AudioFeaturesTrack> features = featureObj.audio_features;

        for (int i = 0; i < features.size(); i++)
        {
            AudioFeaturesTrack curr = features.get(i);
            if(curr.danceability > .70) { tired.add(curr); }
            if(curr.energy > .70) { energetic.add(curr); }
            if(curr.acousticness > .50 && curr.valence < .29) { sad.add(curr); }
            if(curr.loudness < -6 && (curr.energy > .35 && curr.energy < .60)) { chill.add(curr); }
        }
    }


    @Override
    public void onPartial(String text)
    {

        //setText("Partial: "+text+"\n");
    }

    @Override
    public void onFinal(String text)
    {
        unfilteredMood = text;
        //setText("Final: "+text+"\n");
    }
}
