package com.hikvision.android.rxjava2.ftp;

import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

/**
 * Created by yexiaokang on 2016/12/21.
 */

public final class Ftp4jManager implements Closeable {

    private static final String TAG = "Ftp4jManager";
    private boolean ftpConnected = false;
    private FTPClient ftpClient;


    private void openConnect(String remoteDir) throws FTPException, IOException, FTPIllegalReplyException {
        ftpClient = new FTPClient();
        printf(ftpClient.connect(FtpConstant.DF_IP, Integer.parseInt(FtpConstant.DF_PORT)));
        ftpClient.login(FtpConstant.DF_USER_NAME, FtpConstant.DF_PASSWORD);
        ftpClient.setPassive(true);
        ftpClient.setType(FTPClient.TYPE_BINARY);
        ftpClient.setAutoNoopTimeout(30000);
        Log.i(TAG, "openConnect: now we login ok");
        ftpClient.changeDirectory(remoteDir);
        String pwd = ftpClient.currentDirectory();
        Log.d(TAG, "openConnect: pwd = " + pwd);
        ftpConnected = true;
    }

    public void upload(String remoteDir, File file, FTPDataTransferListener listener) {
        try {
            if (ftpConnected) {
                closeQuietly();
            }
            if (!ftpConnected) {
                openConnect(remoteDir);
            }
            ftpClient.upload(file, listener);
        } catch (FTPException | IOException | FTPIllegalReplyException | FTPAbortedException | FTPDataTransferException e) {
            e.printStackTrace();
        } finally {
            closeQuietly();
        }
    }


    private void printf(String[] strings) {
        if (strings != null && strings.length > 0) {
            for (String s : strings) {
                Log.d(TAG, "printf: " + s);
            }
        }
    }

    public void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() throws IOException {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect(true);
            } catch (FTPIllegalReplyException | FTPException e) {
                e.printStackTrace();
            }
        }
        ftpClient = null;
        ftpConnected = false;
    }
}
