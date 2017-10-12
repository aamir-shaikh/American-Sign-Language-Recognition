/*
 * Copyright (C) 2014 Thalmic Labs Inc.
 * Distributed under the Myo SDK license agreement. See LICENSE.txt for details.
 */

package com.thalmic.android.sample.glass;

import android.app.Instrumentation;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.XDirection;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//
// This sample demonstrates how to connect to a Myo and use it to trigger touch pad motion
// events that Google Glass recognizes as taps and swipes.
//
// Due to Android security restrictions, the motion events dispatched by this service will only be
// sent to activities in the same application.
//
public class MyoGlassService extends Service {
    private static final String TAG = "MyoGlassService";

    private static final String PREF_MAC_ADDRESS = "PREF_MAC_ADDRESS";

    private Hub mHub;
    private SharedPreferences mPrefs;
    private boolean mActivityActive;
    private MyoListener mListener = new MyoListener();

    // Return an interface to use to communicate with the service.
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new MBinder();

    // The Binder class clients will use to communicate with this service. We know clients in this
    // sample will always run in the same process as the service, so we don't need to deal with IPC.
    public class MBinder extends Binder {
        public MyoGlassService getService() {
            return MyoGlassService.this;
        }
    }

    // Set the active state of the activity.
    public void setActivityActive(boolean active) {
        mActivityActive = active;
    }

    // Detach from the currently attached Myo, if any, and attach to a new one.
    public void attachToNewMyo() {
        // Detach from the previously attached Myo, if it exists.
        mHub.detach(mPrefs.getString(PREF_MAC_ADDRESS, ""));

        // Clear the saved Myo mac address.
        mPrefs.edit().putString(PREF_MAC_ADDRESS, "").apply();

        // Begin looking for an adjacent Myo to attach to.
        mHub.attachToAdjacentMyo();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // First, we initialize the Hub singleton with an application identifier.
        mHub = Hub.getInstance();
        if (!mHub.init(this, getPackageName())) {
            Log.e(TAG, "Could not initialize the Hub.");
            stopSelf();
            return;
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Register for DeviceListener callbacks.
        mHub.addListener(mListener);

        // If there is no connected Myo, try to attach to one.
        if (mHub.getConnectedDevices().isEmpty()) {
            String myoAddress = mPrefs.getString(PREF_MAC_ADDRESS, "");

            // If we have a saved Myo MAC address then connect to it, otherwise look for one nearby.
            if (TextUtils.isEmpty(myoAddress)) {
                mHub.attachToAdjacentMyo();
            } else {
                mHub.attachByMacAddress(myoAddress);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Release any resources held by the Hub and MyoListener.
        mHub.shutdown();
        mListener.shutdown();
    }

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private class MyoListener extends AbstractDeviceListener {
        private static final long LAUNCH_HOLD_DURATION = 1000;

        private Handler mHandler = new Handler();
        private Instrumentation mInstrumentation = new Instrumentation();
        private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

        private Runnable mLaunchRunnable = new Runnable() {
            @Override
            public void run() {
                // Start the immersion activity. FLAG_ACTIVITY_NEW_TASK is needed to start an
                // Activity from a Service.
                Intent intent = new Intent(MyoGlassService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        };

        public void shutdown() {
            mExecutor.shutdown();
        }

        // onAttach() is called whenever a Myo has been attached.
        @Override
        public void onAttach(Myo myo, long timestamp) {
            // Store the MAC address of the attached Myo so we can automatically attach to it
            // the next time the app starts.
            mPrefs.edit().putString(PREF_MAC_ADDRESS, myo.getMacAddress()).apply();
        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            Log.i(TAG, "pose: " + pose);

            if (!mActivityActive) {
                // Post a delayed runnable when the FINGERS_SPREAD pose is detected, and remove it
                // if the pose is released before the delay ends. This allows triggering actions
                // only if a pose is held for a certain time.
                if (pose == Pose.FINGERS_SPREAD) {
                    mHandler.postDelayed(mLaunchRunnable, LAUNCH_HOLD_DURATION);

                    // Tell the Myo that an event has resulted in an action. The Myo will vibrate.
                    myo.notifyUserAction();

                    // Tell the Myo to stay unlocked until told otherwise. We do so here so you can
                    // hold the pose without the Myo becoming locked.
                    myo.unlock(Myo.UnlockType.HOLD);
                } else {
                    mHandler.removeCallbacks(mLaunchRunnable);

                    // Tell the Myo that an event has resulted in an action. The Myo will vibrate.
                    myo.notifyUserAction();

                    // Tell the Myo to unlock, but lock after a short period.
                    myo.unlock(Myo.UnlockType.TIMED);
                }
            } else {
                // Swap wave poses if the Myo is on the left arm. Allows user to "wave" right or left
                // regardless of the Myo arm and have the swipes be in the appropriate direction.
                if (myo.getArm() == Arm.LEFT) {
                    if (pose == Pose.WAVE_IN) {
                        pose = Pose.WAVE_OUT;
                    } else if (pose == Pose.WAVE_OUT) {
                        pose = Pose.WAVE_IN;
                    }
                }

                // Dispatch touch pad events for the standard navigation controls based on the
                // current pose.
                switch (pose) {
                    case FIST:
                        sendEvents(myo, MotionEventGenerator.getTapEvents());
                        break;
                    case FINGERS_SPREAD:
                        sendEvents(myo, MotionEventGenerator.getSwipeDownEvents());
                        break;
                    case WAVE_IN:
                        sendEvents(myo, MotionEventGenerator.getSwipeRightEvents());
                        break;
                    case WAVE_OUT:
                        sendEvents(myo, MotionEventGenerator.getSwipeLeftEvents());
                        break;
                }
            }
        }

        // Dispatch a list of events using Instrumentation. Due to Android security restrictions,
        // the events will only be sent to activities in the same application.
        private void sendEvents(Myo myo, final List<MotionEvent> events) {
            if (mExecutor.isShutdown()) {
                Log.w(TAG, "Executor shutdown. Can't send event.");
                return;
            }

            // Tell the Myo that an event has resulted in an action. The Myo will vibrate.
            myo.notifyUserAction();

            // Tell the Myo to unlock for a short period. Since it is already unlocked, this will
            // extend the time before it gets locked again.
            myo.unlock(Myo.UnlockType.TIMED);

            for (final MotionEvent event : events) {
                // Post the event dispatch to a background thread, as sendPointerSync can not be
                // called from the main thread.
                mExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mInstrumentation.sendPointerSync(event);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed sending motion event." , e);
                        }
                    }
                });
            }
        }
    }
}
