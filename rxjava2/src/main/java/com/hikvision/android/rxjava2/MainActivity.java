package com.hikvision.android.rxjava2;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.hikvision.android.rxjava2.ftp.Ftp4jManager;
import com.hikvision.android.rxjava2.ftp.FtpClientManager;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FtpClientManager ftpClientManager;
    private static final String VID_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
            + File.separator + "Camera" + File.separator + "VID_20161207_1647511.mp4";
    private static final String REMOTE_PATH = "/area0/20161221_03/";
    private Ftp4jManager ftp4jManager = new Ftp4jManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        ftpClientManager = new FtpClientManager(this);
    }

    @OnClick(R.id.button)
    void submit() {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> e) throws Exception {
                e.onNext("------------------------------------------------------");
                ftp4jManager.upload(REMOTE_PATH, new File(VID_PATH), new FTPDataTransferListener() {
                    @Override
                    public void started() {
                        e.onNext("started: ");
                    }

                    @Override
                    public void transferred(int i) {
                        e.onNext("transferred: " + i);
                    }

                    @Override
                    public void completed() {
                        e.onNext("completed: ");
                        e.onComplete();
                    }

                    @Override
                    public void aborted() {
                        e.onNext("aborted: ");
                        e.onComplete();
                    }

                    @Override
                    public void failed() {
                        e.onNext("failed: ");
                        e.onComplete();
                    }
                });
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String s) {
                        Log.i(TAG, "onNext: " + s);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "onError: ", t);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: ");
                    }
                });
        /*Flowable.create(new FlowableOnSubscribe<String>() {
            @Override
            public void subscribe(final FlowableEmitter<String> e) throws Exception {
                e.onNext("------------------------------------------------------");
                ftpClientManager.uploadFile(REMOTE_PATH, new File(VID_PATH), new FtpUploadAdapter() {
                    @Override
                    public void onUploadProgress(int current, int total, File file) {
                        super.onUploadProgress(current, total, file);
                        e.onNext(current + "/" + total);
                        //Log.d(TAG, "onUploadProgress: " + current + "/" + total + "/" + file.getName());
                    }

                    @Override
                    public void onUploadFail(File file) {
                        super.onUploadFail(file);
                        e.onNext("onUploadFail: " + file.getName());
                        e.onComplete();
                        Log.d(TAG, "onUploadFail: " + file.getName());
                    }

                    @Override
                    public void onUploadSuccess(File file) {
                        super.onUploadSuccess(file);
                        e.onNext("onUploadSuccess: " + file.getName());
                        e.onComplete();
                        Log.d(TAG, "onUploadSuccess: " + file.getName());
                    }

                    @Override
                    public void onUploadFail() {
                        super.onUploadFail();
                        e.onNext("onUploadFail: ");
                        e.onComplete();
                        Log.d(TAG, "onUploadFail: ");
                    }

                    @Override
                    public void onConnectFail(int errorCode) {
                        super.onConnectFail(errorCode);
                        e.onNext("onConnectFail: errorCode = " + errorCode);
                        e.onComplete();
                        Log.d(TAG, "onConnectFail: errorCode = " + errorCode);
                    }

                    @Override
                    public void onUploadComplete() {
                        super.onUploadComplete();
                        e.onComplete();
                        Log.i(TAG, "onUploadComplete: ");
                    }
                });
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        Log.d(TAG, "onSubscribe: ");
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.i(TAG, "onNext: " + s);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "onError: ", t);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: ");
                    }
                });*/
    }

}
