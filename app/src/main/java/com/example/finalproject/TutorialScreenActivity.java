package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * Created by arjun on 6/28/17.
 */

public class TutorialScreenActivity extends Activity {

    private Spinner chosseLetterDropDown2;
    private VideoView vidView;
    private Button getVideoButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_screen);

        chosseLetterDropDown2 = (Spinner)findViewById(R.id.tutorial_choose_letter);
        vidView = (VideoView)findViewById(R.id.letterVideo);
        getVideoButton = (Button) findViewById(R.id.getVideoButton);

        // https://stackoverflow.com/questions/13377361/how-to-create-a-drop-down-list
        String[] lettersList2 = new String[]{"A", "B", "C"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, lettersList2);
        chosseLetterDropDown2.setAdapter(adapter2);

        // For media player controls
        MediaController vidControl = new MediaController(this);
        vidControl.setAnchorView(vidView);
        vidView.setMediaController(vidControl);


        getVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // https://code.tutsplus.com/tutorials/streaming-video-in-android-apps--cms-19888
                String videoURL = "https://archive.org/download/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4";
                Uri vidUri = Uri.parse(videoURL);
                vidView.setVideoURI(vidUri);
                vidView.start();
            }
        });

    }
}
