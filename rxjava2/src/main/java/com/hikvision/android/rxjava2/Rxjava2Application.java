package com.hikvision.android.rxjava2;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import org.apache.log4j.Level;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * Created by yexiaokang on 2016/12/21.
 */

public class Rxjava2Application extends Application {

    private static final String TAG = "Rxjava2Application";

    @Override
    public void onCreate() {
        super.onCreate();
        log4jConfigure(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
    }

    /**
     * %m 输出代码中指定的消息<br/>
     * %p 输出优先级，即DEBUG，INFO，WARN，ERROR，FATAL；5表示宽度为5（即输出结果以5个字符宽度对齐），负号表示左对齐<br/>
     * %r 输出自应用启动到输出该log信息耗费的毫秒数<br/>
     * %c 输出所属的类目，通常就是所在类的全名<br/>
     * %t 输出产生该日志事件的线程名<br/>
     * %n 输出一个回车换行符，Windows平台为“rn”，Unix平台为“n”<br/>
     * %d 输出日志时间点的日期或时间，默认格式为ISO8601，也可以在其后指定格式，比如：%d{yyyy MMM dd HH:mm:ss,SSS}，输出类似：2002年10月18日 22：10：28，921<br/>
     * %l 输出日志事件的发生位置，包括类目名、发生的线程，以及在代码中的行数。<br/>
     *
     * @param sdcardExist sdcardExist
     */
    public synchronized void log4jConfigure(boolean sdcardExist) {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                "RxAndroid" + File.separator + "log" + File.separator + "rxjava2.log";
        try {
            LogConfigurator logConfigurator = new LogConfigurator();
            logConfigurator.setResetConfiguration(true);
            logConfigurator.setFileName(fileName);
            logConfigurator.setRootLevel(Level.DEBUG);
//            logConfigurator.setLevel("org.apache", Level.ERROR);
//            logConfigurator.setFilePattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%t] [%c{2}]-[%L] %m%n");
            logConfigurator.setFilePattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c{2}]-[%L] %m%n");
            logConfigurator.setMaxFileSize(1024 * 1024 * 5);
            logConfigurator.setUseFileAppender(sdcardExist);
            logConfigurator.setUseLogCatAppender(true);
            logConfigurator.setImmediateFlush(true);
            logConfigurator.configure();
        } catch (Exception e) {
            Log.e(TAG, "log4jConfigure", e);
        }
    }
}
