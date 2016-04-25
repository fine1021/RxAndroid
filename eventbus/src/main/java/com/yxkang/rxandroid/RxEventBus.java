package com.yxkang.rxandroid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * Created by yexiaokang on 2016/4/25.
 */
@SuppressWarnings({"unchecked", "unused"})
public class RxEventBus {

    private static final String TAG = RxEventBus.class.getSimpleName();
    private static volatile RxEventBus sRxEventBus;

    private final Map<Object, List<Subscription>> mMap = new HashMap<>();
    private final ReentrantLock mReentrantLock = new ReentrantLock(true);
    private final MessageHandler mHandler;
    private final Subject mSubject;

    private RxEventBus() {
        mSubject = new SerializedSubject<>(PublishSubject.create());
        mHandler = new MessageHandler(Looper.getMainLooper());
    }

    public static RxEventBus getInstance() {
        if (sRxEventBus == null) {
            synchronized (RxEventBus.class) {
                if (sRxEventBus == null) {
                    sRxEventBus = new RxEventBus();
                }
            }
        }
        return sRxEventBus;
    }

    /**
     * post an event to event bus, the event will always be received in UI Thread
     *
     * @param o the event
     */
    public void post(final Object o) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mSubject.onNext(o);
        } else {
            Log.i(TAG, "current Thread is not UI Thread, post the event to UI Thread");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mSubject.onNext(o);
                }
            });
        }
    }

    /**
     * define the event type, you should call this method before subscribe an event
     *
     * @param eventType eventType
     * @param <T>       data model
     * @return {@link Observable}
     */
    public <T> Observable<T> ofType(Class<T> eventType) {
        return mSubject.ofType(eventType);
    }

    /**
     * keep the subscription for subscriber after subscribe, call {@link #unsubscribe(Object)} when don't want to receive the event
     *
     * @param subscriber   subscriber
     * @param subscription subscription
     * @see #unsubscribe(Object)
     */
    public void subscribe(Object subscriber, Subscription subscription) {
        mReentrantLock.lock();
        try {
            List<Subscription> list = mMap.get(subscriber);
            if (list == null) {
                list = new ArrayList<>();
                list.add(subscription);
                mMap.put(subscriber, list);
                Log.i(TAG, "subscribe: " + subscriber.getClass());
            } else {
                list.add(subscription);
            }
        } finally {
            mReentrantLock.unlock();
        }
    }

    /**
     * unsubscribe all events, avoiding memory leak
     *
     * @param subscriber subscriber
     * @see #subscribe(Object, Subscription)
     */
    public void unsubscribe(Object subscriber) {
        mReentrantLock.lock();
        try {
            List<Subscription> list = mMap.get(subscriber);
            if (list != null) {
                Log.i(TAG, "unsubscribe: " + subscriber.getClass());
                for (Subscription subscription : list) {
                    if (!subscription.isUnsubscribed()) {
                        subscription.unsubscribe();
                    }
                }
                mMap.remove(subscriber);
            }
        } finally {
            mReentrantLock.unlock();
        }
    }

    private class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

        }
    }
}
