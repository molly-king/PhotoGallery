package com.txrd.photogallery.app;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mollyrand on 7/9/14.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    Handler mHandler;
    Handler mResponseHandler;
    Listener<Token> mListener;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());

    public interface Listener<Token>{
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener(Listener<Token> listener){
        mListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    //Android Lint will warn about subclassing Handler. Everything is tied to HandlerThread though, so no danger of memory leak.
    @Override
    protected void onLooperPrepared(){
        //called before looper checks the queue for the first time
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };
    }

    public void queueThumbnail(Token token, String url){
        Log.i(TAG, "Got a URL: " + url);
        requestMap.put(token, url);
        //store and retrieve urls based on the particular token

        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token)
                .sendToTarget();
        //send off to be put on message queue
    }

    private void handleRequest(final Token token){
        //where the actual downloading happens
        try {
            final String url = requestMap.get(token);
            if (url == null)
               return;

            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "bitmap created");

            //associated with main thread's looper, will run on main thread (update UI)
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token) != url)
                        //necessary to double-check because the gridview recycles views. by the time we finish downloading the view may have been recycled and requested a different URL
                        return;

                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "error downloading image" , ioe);
        }
    }

    public void clearQueue(){
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}
