package com.yxkang.rxandroid;

import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

/**
 * <h1>RxEventBus</h1>
 * {@code RxEventBus} is an EventBus based on {@code RxJava}, the usage is very easy.
 * <h1>Usage</h1>
 * <p>Here is an example of usage:</p>
 * <pre class="prettyprint">
 * Subscription subscription = RxEventBus.getInstance().ofType(MessageEvent.class).subscribe(new Action1&lt;MessageEvent&gt;() {
 * &nbsp;&nbsp;<code>@Override</code>
 * &nbsp;&nbsp;public void call(MessageEvent messageEvent) {
 * &nbsp;&nbsp;&nbsp;&nbsp;// do something on the messageEvent
 * &nbsp;&nbsp;}
 * });
 * // don't forget to subscribe the subscription for current class
 * RxEventBus.getInstance().subscribe(this, subscription);
 * <p/>
 * RxEventBus.getInstance().post(new MessageEvent("MessageEvent"));
 * </pre>
 * <p>Avoid memory leak, you should add the follow statement when you don't want to receive any events:</p>
 * <pre class="prettyprint">
 * RxEventBus.getInstance().unsubscribe(this);
 * </pre>
 * <h1>Post Event</h1>
 * <p>The event will always be post in android main thread, no matter where you called {@link RxEventBus#post(Object)}</p>
 */
@SuppressWarnings({"unchecked", "unused"})
public class RxEventBus {

    private static final String TAG = RxEventBus.class.getSimpleName();
    private static volatile RxEventBus sRxEventBus;

    private final Map<Object, CompositeSubscription> mMap = new HashMap<>();
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
     * post an event to event bus, the event will always be posted in main Thread
     *
     * @param o the event
     */
    public final void post(final Object o) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mSubject.onNext(o);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mSubject.onNext(o);
                }
            });
        }
    }

    /**
     * transform the <tt>RxEventBus</tt> to <tt>Observable</tt>. call {@link Observable#subscribe()} method to subscribe event.
     * but you should check the event type manually, by using <code>instanceof</code>
     *
     * @return the <tt>Observable</tt>
     */
    public Observable<Object> toObservable() {
        return mSubject;
    }

    /**
     * filter the event type, you should call this method before subscribe an event
     *
     * @param eventType eventType
     * @param <T>       data model
     * @return {@link Observable}
     */
    public <T> Observable<T> ofType(Class<T> eventType) {
        return mSubject.ofType(eventType);
    }

    /**
     * keep the subscription for subscriber after called {@link Observable#subscribe()},
     * this method can be called many times as soon as you subscribe an event,
     * but don't forget to call {@link #unsubscribe(Object)} once you don't want to receive any events
     *
     * @param subscriber   subscriber
     * @param subscription subscription
     * @see #unsubscribe(Object)
     */
    public final void subscribe(Object subscriber, Subscription subscription) {
        mReentrantLock.lock();
        try {
            CompositeSubscription list = mMap.get(subscriber);
            if (list == null) {
                list = new CompositeSubscription(subscription);
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
    public final void unsubscribe(Object subscriber) {
        mReentrantLock.lock();
        try {
            CompositeSubscription list = mMap.get(subscriber);
            if (list != null) {
                list.unsubscribe();
                mMap.remove(subscriber);
                Log.i(TAG, "unsubscribe: " + subscriber.getClass());
            }
        } finally {
            mReentrantLock.unlock();
        }
    }

    /**
     * indicates whether this Subscriber has unsubscribed from its list of subscriptions.
     *
     * @param subscriber subscriber
     * @return {@code true} if this Subscriber has unsubscribed from its subscriptions, {@code false} otherwise
     */
    public final boolean isUnsubscribed(Object subscriber) {
        mReentrantLock.lock();
        try {
            CompositeSubscription list = mMap.get(subscriber);
            return list == null || list.isUnsubscribed();
        } finally {
            mReentrantLock.unlock();
        }
    }
}
