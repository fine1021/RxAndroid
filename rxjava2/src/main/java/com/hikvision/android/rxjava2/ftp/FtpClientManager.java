package com.hikvision.android.rxjava2.ftp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class FtpClientManager {

    private static final String TAG = "FtpClientManager";

    private static final Logger logger = LoggerFactory.getLogger(TAG);

    public static final int FTP_CONNECT_SERVER_INVALID = 2;
    public static final int FTP_CONNECT_USER_PWD_INVALID = 3;
    public static final int FTP_UPLOAD_FAIL = 4;
    public static final int FTP_CONNECT_FAIL = -1;

    private static final int BUFFER_SIZE = 2048;      // 2k

    private FTPClient ftpClient;

    private final int RET_OK = 1;
    private final int RET_SERVER_INVALID = 2;
    private final int RET_USER_PWD_INVALID = 3;
    private final int RET_ERROR = -1;

    private SharedPreferences preferences;
    private final AtomicBoolean mUpload = new AtomicBoolean(true);

    /**
     * FTP 文件筛选器
     */
    private final FTPFileFilter ftpFileFilter = new FTPFileFilter() {
        @Override
        public boolean accept(FTPFile ftpFile) {
            return !ftpFile.getName().startsWith(".");
        }
    };

    public FtpClientManager(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        getFtpParam();
        mUpload.set(true);
    }

    public void setUpload(boolean enable) {
        mUpload.set(enable);
    }

    public boolean isUpload() {
        return mUpload.get();
    }

    /**
     * 创建连接，登录FTP
     *
     * @throws IOException
     */
    public int openConnect(String remoteDir) throws IOException {
        int ret = RET_ERROR;
        ftpClient = new FTPClient();
        // 中文转码
        //ftpClient.setControlEncoding("UTF-8");
        int reply;    // 服务器响应值
        // 连接至服务器

        logger.debug("ftpIp = {}, ftpPort = {}", ftpIp, ftpPort);

        if (TextUtils.isEmpty(ftpIp) || ftpPort > 65535 || ftpPort < 0) {
            ret = RET_SERVER_INVALID;
            return ret;
        }

        // 登录到服务器
        logger.debug("ftpName = {}, ftpPwd = {}", ftpName, ftpPwd);

        if (TextUtils.isEmpty(ftpName) || TextUtils.isEmpty(ftpPwd)) {
            ret = RET_USER_PWD_INVALID;
            return ret;
        }
        ftpClient.setConnectTimeout(10 * 1000);
        ftpClient.setDefaultTimeout(10 * 1000);
        ftpClient.setAutodetectUTF8(true);

        ftpClient.connect(ftpIp, ftpPort);
        // 获取响应值
        reply = ftpClient.getReplyCode();
        logger.info("connect ReplyCode = {}, ReplyString = {}", reply, ftpClient.getReplyString());
        if (!FTPReply.isPositiveCompletion(reply)) {
            // 断开连接
            logger.error("connect failed !");
            ftpClient.disconnect();
            return ret;
        }

        ftpClient.login(ftpName, ftpPwd);
        // 获取响应值
        reply = ftpClient.getReplyCode();
        logger.info("login ReplyCode = {}, ReplyString = {}", reply, ftpClient.getReplyString());
        if (!FTPReply.isPositiveCompletion(reply)) {
            // 断开连接
            logger.error("login failed !");
            ftpClient.disconnect();
        } else {
            // 获取登录信息
            String systemType = ftpClient.getSystemType();
            logger.debug("systemType = {}", systemType);
            ftpClient.setControlKeepAliveTimeout(10);
            ftpClient.setControlKeepAliveReplyTimeout(10 * 1000);
            // 使用被动模式设为默认
            ftpClient.enterLocalPassiveMode();
            // 二进制文件支持
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            // 设置模式
            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);

            if (TextUtils.isEmpty(remoteDir)) {
                // 获取之前保存的路径
                String savePath = getSavePath();
                handleDirectory(savePath);
                logger.info("savePath = {}", savePath);
            } else {
                handleDirectory(remoteDir);
                logger.info("remoteDir = {}", remoteDir);
            }
            ret = RET_OK;
            logger.info("now everything is ok");
        }
        return ret;
    }

    /**
     * 跳转到指定的目录，注意：FTP不能一次创建多级目录，所以对于不存在的目录只能一级一级的创建
     *
     * @param remoteDir FTP远程目录
     * @throws IOException 连接时的异常
     */
    private void handleDirectory(String remoteDir) throws IOException {
        int index = 1;
        boolean directoryExisted = false;
        String[] parts = remoteDir.split("/");
        int length = parts.length;
        Log.v(TAG, "handleDirectory: length = " + length);
        String rootDir = "/";
        String directoryName;
        while (index < length) {
            Log.v(TAG, "handleDirectory: rootDir = " + rootDir);
            FTPFile[] ftpFiles = ftpClient.listFiles(rootDir, ftpFileFilter);
            if (ftpFiles != null && ftpFiles.length > 0) {
                for (FTPFile ftpFile : ftpFiles) {
                    Log.d(TAG, "handleDirectory: ftpFile = " + ftpFile.getName());
                    if (ftpFile.getName().equals(parts[index])) {
                        directoryExisted = true;
                        break;
                    }
                }
            }
            directoryName = parts[index];
            rootDir = rootDir.concat(directoryName).concat("/");

            if (directoryExisted) {
                boolean result = ftpClient.changeWorkingDirectory(directoryName);
                Log.i(TAG, "handleDirectory: changeWorkingDirectory " + directoryName + " = " + result);
                directoryExisted = false;
                index++;
            } else {
                boolean result = ftpClient.makeDirectory(directoryName);
                Log.i(TAG, "handleDirectory: makeDirectory " + directoryName + " = " + result);
                result = ftpClient.changeWorkingDirectory(directoryName);
                Log.i(TAG, "handleDirectory: changeWorkingDirectory " + directoryName + " = " + result);
                index++;
            }

            /*if (directoryExisted) {
                int result = ftpClient.sendCommand("CWD", directoryName);
                Log.i(TAG, "handleDirectory: sendCommand CWD " + directoryName + " = " + result);
                directoryExisted = false;
                index++;
            } else {
                int result = ftpClient.sendCommand("MKD", directoryName);
                Log.i(TAG, "handleDirectory: sendCommand MKD " + directoryName + " = " + result);
                result = ftpClient.sendCommand("CWD", directoryName);
                Log.i(TAG, "handleDirectory: sendCommand CWD " + directoryName + " = " + result);
                index++;
            }*/
        }
        Log.v(TAG, "handleDirectory: final rootDir = " + rootDir);

        String pwd = ftpClient.printWorkingDirectory();
        logger.debug("handleDirectory: pwd = {}", pwd);
    }

    /***
     * 关闭连接
     *
     * @throws IOException
     */
    public void closeConnect() throws IOException {
        logger.info("closeConnect");
        if (ftpClient != null) {
            if (ftpClient.isConnected()) {
                Log.v(TAG, "closeConnect: logout : " + ftpClient.logout());
            }
            ftpClient.disconnect();
        }
    }

    /**
     * 上传文件之前初始化相关参数
     *
     * @param remoteDir FTP远程目录
     * @param listener  监听器
     */
    private int uploadBeforeOperate(String remoteDir, FtpUploadListener listener) {
        int ret = RET_ERROR;
        if (!checkFtpParamSame() || ftpClient == null || !ftpClient.isConnected()) {
            // 打开FTP服务
            try {
                ret = this.openConnect(remoteDir);
                if (ret == RET_OK) {
                    listener.onConnectSuccess();
                }
            } catch (IOException e1) {
                Log.e(TAG, "", e1);
                return ret;
            }
        } else {
            ret = RET_OK;
        }

        if (ret != RET_OK) {
            logger.error("openConnect fail : {}", ret);
            return ret;
        }
        return ret;
    }

    /**
     * 上传单个文件
     *
     * @param remoteDir FTP远程目录
     * @param localFile 本地文件
     * @param listener  监听器
     */
    public void uploadFile(String remoteDir, File localFile, FtpUploadListener listener) {
        try {
            int ret = this.uploadBeforeOperate(remoteDir, listener);
            Log.d(TAG, "uploadBeforeOperate ret : " + ret);
            if (ret == RET_OK) {
                //logger.info("delete existed file : {}", ftpClient.deleteFile(localFile.getName()));
                this.uploadingSingle(localFile, listener);
                if (mUpload.get()) {
                    listener.onUploadComplete();
                }
            } else if (ret == RET_SERVER_INVALID) {
                listener.onConnectFail(FTP_CONNECT_SERVER_INVALID);
            } else if (ret == RET_USER_PWD_INVALID) {
                listener.onConnectFail(FTP_CONNECT_USER_PWD_INVALID);
            } else {
                listener.onConnectFail(FTP_CONNECT_FAIL);
            }
        } catch (Exception e) {
            logger.error("uploadFile", e);
            listener.onConnectFail(FTP_UPLOAD_FAIL);
        } finally {
            try {
                this.closeConnect();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            } finally {
                ftpClient = null;
            }
        }
    }


    /**
     * 上传多个文件.
     *
     * @param fileList  本地文件
     * @param remoteDir FTP远程目录
     * @param listener  监听器
     */
    public void uploadMultiFile(LinkedList<File> fileList, String remoteDir, FtpUploadListener listener) {

        int total = fileList.size();
        int current = 0;
        boolean success = false;

        try {
            int ret = this.uploadBeforeOperate(remoteDir, listener);
            Log.d(TAG, "uploadBeforeOperate ret : " + ret);
            if (ret == RET_OK) {
                for (File singleFile : fileList) {
                    listener.onUploadCount(current, total, singleFile);
//                    logger.info("delete existed file : {}", ftpClient.deleteFile(singleFile.getName()));
                    /*String remoteFile = remoteDir + File.separator + singleFile.getName();
                    Log.v(TAG, "uploadMultiFile: remoteFile = " + remoteFile);
                    int reply = ftpClient.dele(remoteFile);
                    logger.info("delete existed file : {}", reply);
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        remoteFile = singleFile.getName();
                        Log.v(TAG, "uploadMultiFile: remoteFile = " + remoteFile);
                        logger.info("delete existed file2 : {}", ftpClient.dele(remoteFile));
                    }*/
                    success = this.uploadingSingle(singleFile, listener);
                    current++;
                    if (!success) {
                        break;
                    }
                }
                if (mUpload.get() && success) {
                    listener.onUploadComplete();
                } else {
                    listener.onUploadFail();
                }
            } else if (ret == RET_SERVER_INVALID) {
                listener.onConnectFail(FTP_CONNECT_SERVER_INVALID);
            } else if (ret == RET_USER_PWD_INVALID) {
                listener.onConnectFail(FTP_CONNECT_USER_PWD_INVALID);
            } else {
                listener.onConnectFail(FTP_CONNECT_FAIL);
            }
        } catch (Exception e) {
            logger.error("uploadMultiFile", e);
            listener.onConnectFail(FTP_UPLOAD_FAIL);
        } finally {
            try {
                this.closeConnect();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            } finally {
                ftpClient = null;
            }
        }
    }


    /**
     * 上传单个文件.
     *
     * @param localFile 本地文件
     * @return true上传成功, false上传失败
     * @throws IOException
     */
    private boolean uploadingSingle(File localFile, FtpUploadListener listener) throws IOException {

        logger.debug("uploadingSingle start");

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localFile));
        OutputStream out = ftpClient.storeFileStream(localFile.getName());

        if (out == null) {
            logger.error("out stream is null, upload failed");
            listener.onUploadFail(localFile);
            return false;
        }

        BufferedOutputStream bos = new BufferedOutputStream(out);

        byte[] buffer = new byte[BUFFER_SIZE];
        int total = bis.available();
        int current = 0;
        long last = 0;
        long interval = 1024 * 10;            // 10k
        int count;
        while (mUpload.get() && ((count = bis.read(buffer)) != -1)) {
            bos.write(buffer, 0, count);
            bos.flush();
            current += count;
            if ((current - last) > interval) {
                listener.onUploadProgress(current, total, localFile);
                last = current;
            }
        }
        bis.close();
        bos.close();
        out.close();

        boolean flag = ftpClient.completePendingCommand();
        if (mUpload.get()) {
            if (flag) {
                listener.onUploadSuccess(localFile);
            } else {
                listener.onUploadFail(localFile);
            }
        } else {
            listener.onUploadCancel(localFile);
            //logger.info("delete canceled file : {}", ftpClient.deleteFile(localFile.getName()));
            flag = false;
        }

        logger.debug("uploadingSingle end");

        return flag;
    }

    private String ftpIp = "";
    private int ftpPort = FTP.DEFAULT_PORT;
    private String ftpName = "";
    private String ftpPwd = "";

    private void getFtpParam() {
        ftpIp = preferences.getString(FtpConstant.FTP_IP, FtpConstant.DF_IP);
        Log.d(TAG, "ftpIp = " + ftpIp);
        if (ftpIp == null || ftpIp.isEmpty()) {
            ftpIp = "";
        }
        try {
            String _ftpPort = preferences.getString(FtpConstant.FTP_PORT, FtpConstant.DF_PORT);
            ftpPort = Integer.parseInt(_ftpPort);
        } catch (Exception ignored) {
        }
        Log.d(TAG, "ftpPort = " + ftpPort);
        if (ftpPort <= 0 || ftpPort > 65535) {
            ftpPort = 21;
        }
        ftpName = preferences.getString(FtpConstant.FTP_USER_NAME, FtpConstant.DF_USER_NAME);
        Log.d(TAG, "ftpName = " + ftpName);
        ftpPwd = preferences.getString(FtpConstant.FTP_PASSWORD, FtpConstant.DF_PASSWORD);
        Log.d(TAG, "ftpPwd = " + ftpPwd);
        if (TextUtils.isEmpty(ftpName)) {
            ftpName = "";
        }
        if (TextUtils.isEmpty(ftpPwd)) {
            ftpPwd = "";
        }
    }

    private String getSavePath() {
        String savePath = preferences.getString(FtpConstant.FTP_SAVE_PATH, "");
        Log.d(TAG, "savePath = " + savePath);
        if (TextUtils.isEmpty(savePath)) {
            savePath = "/";
        }
        return savePath;
    }

    /**
     * 检查FTP参数是否相同
     *
     * @return 相同与否
     */
    private boolean checkFtpParamSame() {
        boolean ret = true;
        String newFtpIp = preferences.getString(FtpConstant.FTP_IP, FtpConstant.DF_IP);
        int newFtpPort = 21;
        try {
            String _ftpPort = preferences.getString(FtpConstant.FTP_PORT, FtpConstant.DF_PORT);
            newFtpPort = Integer.parseInt(_ftpPort);
        } catch (Exception ignored) {
        }
        String newFtpName = preferences.getString(FtpConstant.FTP_USER_NAME, FtpConstant.DF_USER_NAME);
        String newFtpPwd = preferences.getString(FtpConstant.FTP_PASSWORD, FtpConstant.DF_PASSWORD);
        Log.d(TAG, "newFtpIp = " + newFtpIp + ", newFtpPort = " + newFtpPort + ", newFtpName = " + newFtpName
                + ", newFtpPwd = " + newFtpPwd);
        if ((!newFtpIp.isEmpty() && !newFtpIp.equals(ftpIp))
                || (newFtpPort > 0 && newFtpPort < 65535 && newFtpPort != ftpPort)
                || (!newFtpName.isEmpty() && !newFtpName.equals(ftpName))
                || (!newFtpPwd.isEmpty() && !newFtpPwd.equals(ftpPwd))) {

            logger.debug("ftp params have changed, use the new params");

            ftpIp = newFtpIp;
            ftpPort = newFtpPort;
            ftpName = newFtpName;
            ftpPwd = newFtpPwd;

            logger.info("ftpIp = {}, ftpPort = {}, ftpName = {}, ftpPwd = {}", ftpIp, ftpPort, ftpName, ftpPwd);

            ret = false;
        }
        return ret;
    }

}

