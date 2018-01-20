package com.sbhacks.ektashahani.roadtrip;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.wrapper.spotify.Api;

import kaaes.spotify.webapi.android.SpotifyApi;

public class MainActivity extends AppCompatActivity {

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
}
