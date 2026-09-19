package com.fast.radiofinal5;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.activity.ComponentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;

public class MainActivity extends ComponentActivity {
    private EditText nameEdit, urlEdit;
    private TextView status;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<String> urls = new ArrayList<>();
    private final String[] icons = {"📻 Radio", "🎵 Music", "🎙 Talk", "📡 Live"};
    private Spinner iconSpinner;
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameEdit = findViewById(R.id.stationName);
        urlEdit = findViewById(R.id.streamUrl);
        status = findViewById(R.id.status);
        iconSpinner = findViewById(R.id.stationIcon);
        ListView list = findViewById(R.id.stationList);

        ArrayAdapter<String> iconAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, icons);
        iconAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        iconSpinner.setAdapter(iconAdapter);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        list.setAdapter(adapter);

        controllerFuture = new MediaController.Builder(this,
                new SessionToken(this, new android.content.ComponentName(this, RadioPlaybackService.class))).buildAsync();
        controllerFuture.addListener(() -> {
            try { controller = controllerFuture.get(); } catch (Exception e) { status.setText("Controller error: " + e.getMessage()); }
        }, getMainExecutor());

        findViewById(R.id.addButton).setOnClickListener(v -> addStation());
        findViewById(R.id.playButton).setOnClickListener(v -> playCurrent());
        findViewById(R.id.stopButton).setOnClickListener(v -> { if (controller != null) controller.stop(); status.setText("Stopped"); });
        list.setOnItemClickListener((p,v,pos,id) -> playStation(pos));
    }

    private void addStation() {
        String name = nameEdit.getText().toString().trim();
        String url = urlEdit.getText().toString().trim();
        if (url.isEmpty()) { status.setText("Enter a stream URL"); return; }
        if (name.isEmpty()) name = url;
        String icon = icons[iconSpinner.getSelectedItemPosition()].substring(0, 2).trim();
        names.add(icon + " " + name);
        urls.add(url);
        adapter.notifyDataSetChanged();
        status.setText("Added: " + icon + " " + name);
    }

    private void playCurrent() { String url = urlEdit.getText().toString().trim(); if (!url.isEmpty()) play(url, nameEdit.getText().toString().trim()); }
    private void playStation(int pos) { play(urls.get(pos), names.get(pos)); }

    private void play(String url, String title) {
        if (controller == null) { status.setText("Controller is not ready"); return; }
        String actual = TranscoderUrl.build(url, "mp3", 32);
        controller.setMediaItem(MediaItem.fromUri(actual));
        controller.prepare();
        controller.play();
        status.setText("Playing: " + (title == null || title.isEmpty() ? url : title));
    }

    @Override protected void onDestroy() {
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        super.onDestroy();
    }
}
