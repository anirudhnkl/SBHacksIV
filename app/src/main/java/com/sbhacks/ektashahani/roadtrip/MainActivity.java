package com.sbhacks.ektashahani.roadtrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

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

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/*public class MainActivity extends AppCompatActivity {

    final String clientId = "fd8cdcd290f64bb28d37a246758a4f5f";
    final String clientSecret = "";
    final String redirectURI = "moodQ://callback";

    final Api api = Api.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectURI(redirectURI)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}*/


public class MainActivity extends Activity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "fd8cdcd290f64bb28d37a246758a4f5f";
    private static final String REDIRECT_URI = "http://google.com";
    private static final String CLIENT_SECRET = "2ccdca9a119f4525bbd49ed4fd88594a";

    private Player mPlayer;
    private static final int REQUEST_CODE = 1337;


    SpotifyApi api = new SpotifyApi();
    List<PlaylistTrack> saved;
    AudioFeaturesTracks allTrackFeatures;
    List<AudioFeaturesTrack> tired = new ArrayList<>();
    List<AudioFeaturesTrack> energetic = new ArrayList<>();
    List<AudioFeaturesTrack> chill = new ArrayList<>();
    List<AudioFeaturesTrack> sad = new ArrayList<>();


    private static final String[] MOODS = new String[] {
            "Happy", "Sad", "Tired", "Energetic", "Chill"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "user-library-read"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, MOODS);
        AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.moodEnterView);
        textView.setAdapter(adapter);
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
        switch (playerEvent) {
            // Handle event type as necessary
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
                        allTrackFeatures = audioFeaturesTracks;
                        System.out.println("test: " + audioFeaturesTracks.audio_features.get(0).acousticness);
                        System.out.println("test2: " + audioFeaturesTracks.audio_features.get(1).acousticness);
                        System.out.println("size: " + audioFeaturesTracks.audio_features.size());

                        sortPlaylist(allTrackFeatures);
                        System.out.println("tired: " + tired.size());
                        System.out.println("sad: "  + sad.size());
                        System.out.println("chill: " + chill.size());
                        System.out.println("energetic: " + energetic.size());
                        mPlayer.playUri(null, chill.get(0).uri, 0, 0);
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


//
//        spotify.getTrackAudioFeatures("0bZ52QzCCKfrfOqs7za6lI", new Callback<AudioFeaturesTrack>() {
//            @Override
//            public void success(AudioFeaturesTrack audioFeaturesTrack, Response response) {
//                System.out.println(audioFeaturesTrack.acousticness);
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                System.out.println("fail");
//            }
//        });
//
//
//        spotify.getAlbum("2dIGnmEIy1WZIcZCFSj6i8", new Callback<Album>() {
//            @Override
//            public void success(Album album, Response response) {
//                System.out.println("Album success");
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                Log.d("Album failure", error.toString());
//            }
//        });
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

//    @Override
//    public void onPause() {
//        mPlayer.destroy();
//    }

    public void sortPlaylist(AudioFeaturesTracks featureObj) {
        List<AudioFeaturesTrack> features = featureObj.audio_features;

        for(int i = 0; i < features.size(); i++) {
            AudioFeaturesTrack curr = features.get(i);
            if(curr.danceability > .70) { tired.add(curr); }
            if(curr.energy > .70) { energetic.add(curr); }
            if(curr.acousticness > .50 && curr.valence < .29) { sad.add(curr); }
            if(curr.loudness < -6 && (curr.tempo < 125 && curr.tempo > 90)) { chill.add(curr); }
        }
    }
}
