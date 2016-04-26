package com.yxkang.rxandroid.sample;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.yxkang.rxandroid.RxEventBus;

import rx.Subscription;
import rx.functions.Action1;

public class MainService extends Service {

    private static final String TAG = MainService.class.getSimpleName();

    public static void start(Context context) {
        Intent intent = new Intent(context, MainService.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, MainService.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ok");
        register();
    }

    private void register() {
        Subscription subscription = RxEventBus.getInstance().ofType(TestEvent.class).subscribe(new Action1<TestEvent>() {
            @Override
            public void call(TestEvent testEvent) {
                Log.i(TAG, "call: Thread = " + Thread.currentThread().getName() + " value =  " + testEvent.getValue());
            }
        });
        RxEventBus.getInstance().subscribe(this, subscription);
        subscription = RxEventBus.getInstance().ofType(MessageEvent.class).subscribe(new Action1<MessageEvent>() {
            @Override
            public void call(MessageEvent messageEvent) {
                Log.i(TAG, "call: Thread = " + Thread.currentThread().getName() + " msg =  " + messageEvent.getMessage());
            }
        });
        RxEventBus.getInstance().subscribe(this, subscription);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: isUnsubscribed = " + RxEventBus.getInstance().isUnsubscribed(this));
        RxEventBus.getInstance().unsubscribe(this);
        Log.i(TAG, "onDestroy: ok");
    }
}
