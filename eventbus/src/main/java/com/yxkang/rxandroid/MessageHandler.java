package com.yxkang.rxandroid;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * <h1>MessageHandler</h1>
 * a subclass of {@link Handler}, using in {@link RxEventBus} for posting message to main thread
 */
class MessageHandler extends Handler {

    public MessageHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
    }
}
