package com.example.finalproject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;


public class TestScreenActivity extends Activity{
    TextView gestureClass;
    boolean breakOut = false;
    boolean newGesture = false;
//    long lastTime = System.currentTimeMillis();
    final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Movies/";
    String fileName = "test.csv";
    String result;
    private Spinner chooseLetterDropDown;
    private Button scanButton, cancelButton;
    String gestureRecorded;
    String[] lettersList;
    Context context = this;
    boolean myoConnection = false;
    MyoConnector mMyoConnector;
    HubActivity h;

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            gestureClass = (TextView) findViewById(R.id.gestureClass);
            if(msg.what == 0 && !breakOut) {
                gestureClass.setText(result);
//                Toast.makeText(TestScreenActivity.this, "Wrote", Toast.LENGTH_SHORT).show();
            }
            else{

                gestureClass.setText("Ready");
            }
        }
    };


        final Handler toastHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                Toast.makeText(TestScreenActivity.this, "Myo Connected", Toast.LENGTH_SHORT).show();
            }
        };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.testing_screen);

        //Start Myo reading here
        h = new HubActivity();
        h.start_myo(context);
        h.isTested = false;

        scanButton = (Button) findViewById(R.id.scanButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        chooseLetterDropDown = (Spinner) findViewById(R.id.choose_letter);
        gestureClass = (TextView) findViewById(R.id.gestureClass);

        // https://stackoverflow.com/questions/13377361/how-to-create-a-drop-down-list
//        lettersList = MainMenu.availableTest.toArray(new String[MainMenu.availableTest.size()]);
        lettersList = new String("AEIOU").split("");
        Arrays.sort(lettersList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                lettersList);
        chooseLetterDropDown.setAdapter(adapter);
        chooseLetterDropDown.setSelection(1);

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

        scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//            if(lettersList.length == 0 || lettersList == null) {
//                Toast.makeText(TestScreenActivity.this, "Please run train module!", Toast.LENGTH_LONG).show();
//            }
//            else {
                //gestureRecorded = chooseLetterDropDown.getSelectedItem().toString().toUpperCase();

                breakOut = false;
//                final String text = chooseLetterDropDown.getSelectedItem().toString();
                h.lastUpdated = System.currentTimeMillis();
                h.WriteMode = true;
                h.isTested = true;


                    new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    try {
                                        UploadToServer.uploadToServer(path + "train.csv");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    while (true) {
                                        String file = path + fileName;
                                        try {
                                            Thread.sleep(2000);
                                            result = UploadToServer.uploadToServer(file);
                                            handler.sendEmptyMessage(0);
                                            if(breakOut){
                                                return;
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                            }
                        }).start();
                }
            });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                breakOut = true;
                newGesture = false;
                h.WriteMode = false;
                h.isTested = false;
                handler.sendEmptyMessage(1);
            }
        });
    }

    @Override
    protected void onResume() {
        if(myoConnection) {
            mMyoConnector.scan(5000, h.mScannerCallback);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
//        myoConnection = true;
        if(myoConnection) {
            h.CurrentMyo.writeSleepMode(MyoCmds.SleepMode.NORMAL, null);
            h.CurrentMyo.writeMode(MyoCmds.EmgMode.NONE, MyoCmds.ImuMode.NONE, MyoCmds.ClassifierMode.DISABLED, null);
            h.CurrentMyo.disconnect();
        }
        super.onPause();
    }
}