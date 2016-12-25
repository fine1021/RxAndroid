package com.hikvision.android.rxjava2.ftp;

import java.io.File;

/**
 * Created by yexiaokang on 2016/7/26.
 */
public interface FtpUploadListener {

    /**
     * FTP服务器连接成功
     */
    void onConnectSuccess();

    /**
     * FTP服务器连接失败
     *
     * @param errorCode 错误代码
     */
    void onConnectFail(int errorCode);

    /**
     * 上传单个文件时的进度回调
     *
     * @param current 已上传的文件大小
     * @param total   文件总共大小
     * @param file    当前上传的文件
     */
    void onUploadProgress(int current, int total, File file);

    /**
     * 上传多个文件时的进度回调
     *
     * @param current 当前已上传的个数
     * @param total   文件的总个数
     * @param file    当前上传的文件
     */
    void onUploadCount(int current, int total, File file);

    /**
     * 本次上传任务完成，当遇到某个文件上传失败或者上传取消了是不会调用该方法的
     */
    void onUploadComplete();

    /**
     * 本次上传任务失败，当遇到某个文件上传失败或者上传取消了时调用该方法的
     */
    void onUploadFail();

    /**
     * 上传单个文件成功
     *
     * @param file 当前上传的文件
     */
    void onUploadSuccess(File file);

    /**
     * 上传单个文件失败
     *
     * @param file 当前上传的文件
     */
    void onUploadFail(File file);

    /**
     * 取消上传文件
     *
     * @param file 当前上传的文件
     */
    void onUploadCancel(File file);
}
