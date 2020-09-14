package com.example.test_waiting_activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

public class LauncherActivity extends Activity {

    private Handler mHandler;
    private ServiceWaitThread mThread;
    private long beginningTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.launcher);

        beginningTime = System.currentTimeMillis();

        mHandler = new Handler();
        if(MainService.isReady()) {
            onServiceReady();
        } else {
            startService(new Intent(this,  MainService.class));
            mThread = new ServiceWaitThread();
            mThread.start();
        }
    }

    protected void onServiceReady() {
//        final Class<? extends Activity> classToStart;
        MainService.instance().setActivityToLaunchOnIncomingReceived(MainActivity.class);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent().setClass(LauncherActivity.this, MainActivity.class).setData(getIntent().getData()));
                finish();
            }
        }, 1000);
    }

    public class ServiceWaitThread extends Thread {

        public void run() {
            while (!MainService.isReady()) {
                try {
                    sleep(30);
                    Log.d("essai", "on attend");
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() failed");
                }
            }
            mHandler.post(() -> {
                onServiceReady();
            });
            mThread = null;
        }


    }
}
