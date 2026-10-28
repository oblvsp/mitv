package ru.rufp.mediatest;

import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.util.Log;

class TestCallback extends MediaSession.Callback {

    private final MediaSession session;

    TestCallback(MediaSession session) {
        this.session = session;
    }

    private void setState(int state) {
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                        | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, 0, 1f)
                .build());
    }

    @Override
    public void onPlay() {
        Log.i("mediatest", "PLAY");
        setState(PlaybackState.STATE_PLAYING);
    }

    @Override
    public void onPause() {
        Log.i("mediatest", "PAUSE");
        setState(PlaybackState.STATE_PAUSED);
    }

    @Override
    public void onSkipToNext() {
        Log.i("mediatest", "NEXT");
        setState(PlaybackState.STATE_PLAYING);
    }

    @Override
    public void onSkipToPrevious() {
        Log.i("mediatest", "PREV");
        setState(PlaybackState.STATE_PLAYING);
    }
}
