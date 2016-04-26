package com.yxkang.rxandroid.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.yxkang.rxandroid.RxEventBus;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

@SuppressWarnings("unchecked")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView mTextView;
    private TextView mTextViewSingle;
    private Subscriber<String> mSubscriber;
    private SingleSubscriber<String> mSingleSubscriber;

    /**
     * {@link PublishSubject}貌似是不会把{@link PublishSubject#onNext(Object)}回调到
     * {@link PublishSubject#observeOn(Scheduler)}设置的调度线程中执行，
     * 好像是在哪个线程中抛出事件就在哪个线程中执行
     */
    private PublishSubject<String> mPublishSubject;
    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        mTextView = (TextView) findViewById(R.id.text);
        mTextViewSingle = (TextView) findViewById(R.id.text_single);
        MainService.start(this);

        setSubscriber();
        setObservable();

        setSingleSubscriber();
        setSingle();

        setPublishSubject();
    }

    private void setSubscriber() {
        mSubscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted: Subscriber");
                mPublishSubject.onNext("PublishSubject Post");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Subscriber.onError", e);
            }

            @Override
            public void onNext(String s) {
                Log.i(TAG, "onNext: Thread = " + Thread.currentThread().getName() + " msg =  " + s);
                updateTextView(s);
            }
        };
    }

    private void setSingleSubscriber() {
        mSingleSubscriber = new SingleSubscriber<String>() {
            @Override
            public void onSuccess(String value) {
                updateSingleTextView(value);
            }

            @Override
            public void onError(Throwable error) {

            }
        };
    }

    private void setPublishSubject() {
        mPublishSubject = PublishSubject.create();
        mSubscription = mPublishSubject.subscribe(new Observer<String>() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted: Observer");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Observer.onError", e);
            }

            @Override
            public void onNext(String s) {
                Log.i(TAG, "onNext: Thread = " + Thread.currentThread().getName() + " msg =  " + s);
                updateTextView(s);
                testRxEventBus();
            }
        });
        Subscription subscription = RxEventBus.getInstance().ofType(TestEvent.class)
                .subscribe(new Action1<TestEvent>() {
                    @Override
                    public void call(TestEvent testEvent) {
                        Log.i(TAG, "call: Thread = " + Thread.currentThread().getName() + " value =  " + testEvent.getValue());
                        updateTextView(testEvent.getValue());
                    }
                });
        RxEventBus.getInstance().subscribe(this, subscription);
    }

    private void testRxEventBus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                RxEventBus.getInstance().post(new TestEvent("TestEvent"));
                RxEventBus.getInstance().post(new MessageEvent("MessageEvent"));
            }
        }).start();
    }

    private void setObservable() {
        Observable<String> observable = Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                Log.i(TAG, "Observable.call: Thread = " + Thread.currentThread().getName());
                int loop = 1;
                while (loop < 4) {
                    subscriber.onNext(String.valueOf(loop));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    loop++;
                }
                subscriber.onNext("GO");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /**
                 * if invoke {@link Subscriber#onCompleted()}, subscriber will unsubscribe automatically
                 */
                subscriber.onCompleted();
            }
        });
        Subscription subscription = observable.delay(1, TimeUnit.SECONDS)   // the source Observable shifted forward one second
                .delaySubscription(1, TimeUnit.SECONDS)         // delays the subscription one second to the source Observable
                .subscribeOn(Schedulers.newThread())            // 在一个新线程中调度
                .observeOn(AndroidSchedulers.mainThread())      // 在Android主线程中观察，处理结果
                .subscribe(mSubscriber);                        // 开始订阅
        mSubscriber.add(subscription);                          // 把当前订阅加入到订阅者的订阅列表中
    }

    private void setSingle() {
        Single single = Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "Single.call: Thread = " + Thread.currentThread().getName());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "Success";
            }
        });
        Subscription subscription = single.delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())            // 在一个新线程中调度
                .observeOn(AndroidSchedulers.mainThread())      // 在Android主线程中观察，处理结果
                .subscribe(mSingleSubscriber);
        mSingleSubscriber.add(subscription);
    }

    private void updateTextView(String text) {
        mTextView.setText(text);
    }

    private void updateSingleTextView(String text) {
        mTextViewSingle.setText(text);
    }

    @Override
    protected void onDestroy() {
        /**
         * 检查订阅者所有的订阅中是否有未取消订阅的事件，防止内存泄露
         */
        if (!mSubscriber.isUnsubscribed()) {
            mSubscriber.unsubscribe();                          // 取消所有的订阅
            Log.i(TAG, "onDestroy: mSubscriber.unsubscribe()");
        }
        /**
         * {@link SingleSubscriber}只有{@link SingleSubscriber#onSuccess(Object)}和{@link SingleSubscriber#onError(Throwable)}，
         * 需要手动取消订阅，不然会有内存泄漏的隐患
         */
        if (!mSingleSubscriber.isUnsubscribed()) {
            mSingleSubscriber.unsubscribe();                    // 取消所有的订阅
            Log.i(TAG, "onDestroy: mSingleSubscriber.unsubscribe()");
        }
        /**
         * 如果订阅没有取消，则手动取消，规避内存泄漏的问题，如果调用了onCompleted方法会自动取消订阅的
         */
        if (!mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
            Log.i(TAG, "onDestroy: mSubscription.unsubscribe()");
        }
        MainService.stop(this);
        Log.i(TAG, "onDestroy: isUnsubscribed = " + RxEventBus.getInstance().isUnsubscribed(this));
        RxEventBus.getInstance().unsubscribe(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
