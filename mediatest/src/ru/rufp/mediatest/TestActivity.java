package ru.rufp.mediatest;

import android.app.Activity;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;

public class TestActivity extends Activity {

    private MediaSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new MediaSession(this, "mediatest");
        session.setCallback(new TestCallback(session));
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build());
        session.setActive(true);
    }
}
