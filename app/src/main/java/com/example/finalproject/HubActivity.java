package com.example.finalproject;

/**
 * Created by Aamir on 7/4/2017.
 */

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.darken.myolib.BaseMyo;
import eu.darken.myolib.Myo;
import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;
import eu.darken.myolib.processor.emg.EmgData;
import eu.darken.myolib.processor.emg.EmgProcessor;
import eu.darken.myolib.processor.imu.ImuData;
import eu.darken.myolib.processor.imu.ImuProcessor;

public class HubActivity {

    private MyoConnector mMyoConnector;
    private ImuProcessor mImuProcessor;
    private EmgProcessor mEmgProcessor;
    public Myo CurrentMyo;
    boolean myoConnection = false, isMyoConnected = false;
    public boolean WriteMode = false;
    private Handler mHandler;
    public long lastUpdated;
    String train = "train.csv", test = "test.csv";

    String orientationData;

    Boolean isTested = false;
    int i = 0;

    private List<Integer> roll = new ArrayList<Integer>();
    private List<Integer> yaw = new ArrayList<Integer>();
    private List<Integer> pitch = new ArrayList<Integer>();

    void start_myo(Context mContext) {
        mHandler = new Handler();
        mMyoConnector = new MyoConnector(mContext);
        mMyoConnector.scan(2000, mScannerCallback);
        myoConnection = true;
    }

    public MyoConnector.ScannerCallback mScannerCallback = new MyoConnector.ScannerCallback() {
        @Override
        public void onScanFinished(final List<Myo> myos) {
            if (mHandler == null) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    for (final Myo myo : myos) {

                        CurrentMyo = myo;
                        Log.d("MYO","Myo Connected");
                        myo.connect();
                        myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
                        myo.writeSleepMode(MyoCmds.SleepMode.NEVER, null);
                        myo.writeMode(MyoCmds.EmgMode.FILTERED, MyoCmds.ImuMode.RAW,
                                MyoCmds.ClassifierMode.DISABLED, null);
                        myo.writeUnlock(MyoCmds.UnlockType.HOLD, null);
                        mImuProcessor = new ImuProcessor();
                        myo.addProcessor(mImuProcessor);
                        isMyoConnected = true;

                        mImuProcessor.addListener(new ImuProcessor.ImuDataListener() {
                            @Override
                            public void onNewImuData(ImuData imuData) {
                                if (WriteMode) {
                                    orientationData = Double.valueOf(imuData.getOrientationData()[0]).toString()
                                            + "," +
                                            Double.valueOf(imuData.getOrientationData()[1]).toString() + "," +
                                            Double.valueOf(imuData.getOrientationData()[2]).toString() + "," +
                                            Double.valueOf(imuData.getOrientationData()[3]).toString();
                                }

                            }
                        });

                        mEmgProcessor = new EmgProcessor();
                        myo.addProcessor(mEmgProcessor);
                        mEmgProcessor.addListener(new EmgProcessor.EmgDataListener() {
                            @Override
                            public void onNewEmgData(EmgData emgData) {
                                    while (i < 50 && WriteMode == true) {
                                        if (WriteMode && System.currentTimeMillis() - lastUpdated > 499){
                                        i++;
                                        if(i>=50){
                                            WriteMode = false;
                                            i = 0;
                                        }
                                        try {
                                            UploadToServer.writeEMGToFile(TrainScreenActivity.path +
                                                            (isTested == true ? test : train),
                                                    (Byte.valueOf(emgData.getData()[0])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[1])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[2])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[3])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[4])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[5])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[6])).toString() + "," +
                                                            (Byte.valueOf(emgData.getData()[7])).toString() + "," +
                                                            orientationData + "," +
                                                            (isTested == true ? null : TrainScreenActivity.gestureRecorded), !isTested);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        if (isTested)
                                            WriteMode = false;
                                        lastUpdated = System.currentTimeMillis();
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }

    };

}