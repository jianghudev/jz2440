package com.htc.client.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;


import com.htc.client.controllerscanner.Logger;
import com.htc.client.controllerscanner.ScannerService;

import java.lang.ref.SoftReference;


public class AnimationsContainer {
    public int FPS = 58;
    private int resId = 0;
    private Context mContext =null;

    private static AnimationsContainer mInstance;

    public AnimationsContainer(){
    }

    public static AnimationsContainer getInstance(Context context, int resId, int fps) {
        if (mInstance == null)
            mInstance = new AnimationsContainer();
        mInstance.setResId(context, resId, fps);
        return mInstance;
    }

    public void setResId(Context context, int resId, int fps){
        this.mContext = context;
        this.resId = resId;
        this.FPS = fps;
    }

    public FramesSequenceAnimation createAnim(ImageView imageView) {
        return new FramesSequenceAnimation(imageView, getData(resId), FPS);
    }


    public class FramesSequenceAnimation {
        private static final String TAG = "FramesSequenceAnimation";
        private int[] mFrames;
        private int mIndex;
        private boolean mShouldRun;
        private boolean mIsRunning;
        private SoftReference<ImageView> mSoftReferenceImageView;
        private Handler mHandler;
        private int mDelayMillis;
        private long mLastTimestamp;
        private OnAnimationStoppedListener mOnAnimationStoppedListener;

        private Bitmap mBitmap = null;
        private BitmapFactory.Options mBitmapOptions;

        public FramesSequenceAnimation(ImageView imageView, int[] frames, int fps) {

            mHandler = new Handler(Looper.getMainLooper());
            mFrames = frames;
            mIndex = -1;
            mSoftReferenceImageView = new SoftReference<ImageView>(imageView);
            mShouldRun = false;
            mIsRunning = false;
            mDelayMillis = 1000 / fps;
            mLastTimestamp = 0;

            imageView.setImageResource(mFrames[0]);

            if (Build.VERSION.SDK_INT >= 11) {
                Bitmap bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                int width = bmp.getWidth();
                int height = bmp.getHeight();
                Bitmap.Config config = bmp.getConfig();
                mBitmap = Bitmap.createBitmap(width, height, config);
                mBitmapOptions = new BitmapFactory.Options();

                mBitmapOptions.inBitmap = mBitmap;
                mBitmapOptions.inMutable = true;
                mBitmapOptions.inSampleSize = 1;
            }
        }

        private int getNext() {
            mIndex++;
            if (mIndex >= mFrames.length)
                mIndex = 0;
            return mFrames[mIndex];
        }

        public synchronized void start() {
            mShouldRun = true;
            if (mIsRunning)
                return;

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    ImageView imageView = mSoftReferenceImageView.get();
                    if (!mShouldRun || !ScannerService.mIsScreenON || imageView == null) {
                        Logger.d(TAG, "[run] stopped, mShouldRun: " + mShouldRun + ", mIsScreenON: " + ScannerService.mIsScreenON);
                        mIsRunning = false;
                        if (mOnAnimationStoppedListener != null) {
                            mOnAnimationStoppedListener.AnimationStopped();
                        }
                        return;
                    }

                    mIsRunning = true;

                    //int duration = 0;

                    //if(mLastTimestamp != 0){
                    //    duration = (int)(System.currentTimeMillis() - mLastTimestamp);
                    //}

                    //Logger.d(TAG, "[run] index: " +mIndex+ ", duration: " + duration + ", frame-by-frame: " + mDelayMillis);

                    mHandler.postDelayed(this, mDelayMillis);

                    if (imageView.isShown()) {
                        int imageRes = getNext();
                        if (mBitmap != null) {
                            Bitmap bitmap = null;
                            try {
                                bitmap = BitmapFactory.decodeResource(imageView.getResources(), imageRes, mBitmapOptions);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                            } else {
                                imageView.setImageResource(imageRes);
                                mBitmap.recycle();
                                mBitmap = null;
                            }
                        } else {
                            imageView.setImageResource(imageRes);
                        }
                    }

                    //mLastTimestamp = System.currentTimeMillis();

                }
            };

            mHandler.post(runnable);
        }

        public synchronized void stop() {
            mShouldRun = false;
        }

        public void setOnAnimStopListener(OnAnimationStoppedListener listener){
            this.mOnAnimationStoppedListener = listener;
        }
    }

    private int[] getData(int resId){
        TypedArray array = mContext.getResources().obtainTypedArray(resId);

        int len = array.length();
        int[] intArray = new int[array.length()];

        for(int i = 0; i < len; i++){
            intArray[i] = array.getResourceId(i, 0);
        }
        array.recycle();
        return intArray;
    }

    public interface OnAnimationStoppedListener{
        void AnimationStopped();
    }
}