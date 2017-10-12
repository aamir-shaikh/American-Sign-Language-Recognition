package com.example.finalproject;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;

//import com.thalmic.myo.DeviceListener;

/**
 * Created by pawan on 7/2/17.
 */

public class TrainScreenActivity extends AppCompatActivity {
    Button cancelButton;
    Spinner chooseLetterDropDown;
    static boolean newGesture = false;
    TextView gestureClass;
    TextView mTextView;
    VideoView trainVideo;
    boolean myoConnection = false;
    static String gestureRecorded;
    static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Movies/";
    String[] letterList;
    MyoConnector mMyoConnector;
    private Button trainButton;
    HubActivity h;

    Context context;

    final Handler toastHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            Toast.makeText(TrainScreenActivity.this, "Myo Connected", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.train_screen);

        context = TrainScreenActivity.this;

        //Start Myo reading here
        h = new HubActivity();
        h.start_myo(context);
        h.myoConnection = true;

        trainButton = (Button) findViewById(R.id.trainButton);
        cancelButton = (Button) findViewById(R.id.trainCancel);
        chooseLetterDropDown = (Spinner) findViewById(R.id.train_spinner);
        gestureClass = (TextView) findViewById(R.id.trainGesture);
        trainVideo = (VideoView) findViewById(R.id.videoView);
        mTextView = (TextView) findViewById(R.id.mTextView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(h.isMyoConnected){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        toastHandler.sendEmptyMessage(0);
                        break;
                    }
                }
            }
        }).start();

        // https://stackoverflow.com/questions/13377361/how-to-create-a-drop-down-list
        letterList = new String("ABCDEFGHIJKLMNOPQRSTUVWXYZ").split("");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, letterList);
        chooseLetterDropDown.setAdapter(adapter);
        chooseLetterDropDown.setSelection(1);

        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGesture = true;
                MainMenu.availableTest.add(chooseLetterDropDown.getSelectedItem().toString());
                gestureRecorded = chooseLetterDropDown.getSelectedItem().toString().toUpperCase();

                String url = "http://192.168.43.237/uploads/vids/" + gestureRecorded + ".mp4";
                trainVideo.setVideoURI(Uri.parse(url));
                trainVideo.start();
                h.lastUpdated = System.currentTimeMillis();
                h.WriteMode = true;
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGesture = false;
                h.WriteMode = false;
            }
        });
    }

    @Override
    protected void onResume() {
        if (myoConnection) {
            mMyoConnector.scan(5000, h.mScannerCallback);

       }
        super.onResume();
    }

    @Override
    protected void onPause() {
        myoConnection = true;
        if(myoConnection) {
            h.CurrentMyo.writeSleepMode(MyoCmds.SleepMode.NORMAL, null);
            h.CurrentMyo.writeMode(MyoCmds.EmgMode.NONE, MyoCmds.ImuMode.NONE, MyoCmds.ClassifierMode.DISABLED, null);
            h.CurrentMyo.disconnect();
        }
        super.onPause();
    }
}

