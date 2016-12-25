package com.hikvision.android.rxjava2.ftp;

import java.io.File;

/**
 * Created by yexiaokang on 2016/7/26.
 */
public abstract class FtpUploadAdapter implements FtpUploadListener {

    @Override
    public void onConnectSuccess() {

    }

    @Override
    public void onConnectFail(int errorCode) {

    }

    @Override
    public void onUploadProgress(int current, int total, File file) {

    }

    @Override
    public void onUploadCount(int current, int total, File file) {

    }

    @Override
    public void onUploadComplete() {

    }

    @Override
    public void onUploadFail() {

    }

    @Override
    public void onUploadSuccess(File file) {

    }

    @Override
    public void onUploadFail(File file) {

    }

    @Override
    public void onUploadCancel(File file) {

    }
}
