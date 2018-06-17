package com.htc.miac.controllerutility.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.htc.miac.controllerutility.R;
import com.htc.miac.controllerutility.controllerscanner.Logger;
import com.htc.miac.controllerutility.widget.MiddleIconSpan;

/**
 * Created by chihhang_chuang on 2017/11/8.
 */

public class Utils {
    /** check if the network is connected */
    public static boolean isNetworkConnected(final String TAG, Context context) {
        if (context == null) {
            Log.w(TAG, "context is null");
            return false;
        }

        boolean isConnected = false;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info != null) {
            isConnected = info.isConnected();
            Log.i(TAG, "isNetworkConnected: " + isConnected + ", type = " + info.getTypeName());
        }
        return isConnected;
    }

    public static AnimationsContainer.FramesSequenceAnimation renderPressHomeBtnAnimation(Context context, RelativeLayout layout,@IdRes int imageViewResID){
        try {
            ImageView controllerImageView = (ImageView) layout.findViewById(imageViewResID);

            return AnimationsContainer.getInstance(context, R.array.animation_press_home_btn, 30)
                    .createAnim(controllerImageView);

        }catch (Exception e){
            Logger.w("Utils", "[renderPressHomeBtnAnimation] failed: "+e.getMessage());
        }

        return null;
    }

    /** for Update Press Home Button UI **/
    public static void renderPressHomeBtnUIText(Context context, RelativeLayout layout,@IdRes int textResID){
        try {
            TextView descriptionTextView = (TextView) layout.findViewById(textResID);

            String text = context.getString(R.string.pair_controller_description_new_render);
            String showText = text.replace("{", "").replace("}", "");

            SpannableString spannableString = new SpannableString(showText);

            // For Highlight Text
            int highlightStartIndex = text.indexOf("{");
            int highlightEndIndex = text.indexOf("}") - 1;

            spannableString.setSpan(
                    new ForegroundColorSpan(context.getColor(R.color.miac_new_description_highlight_color)),
                    highlightStartIndex, highlightEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // For Icon
            int iconStartIndex = showText.indexOf("@");
            int iconEndIndex = iconStartIndex+1;

            int iconSize = Utils.dip2px(context, 12);
            Drawable iconHomeBtn = ContextCompat.getDrawable(context,R.drawable.miac_icon_home_button);
            iconHomeBtn.setBounds(0, 0, iconSize, iconSize);

            spannableString.setSpan(
                    new MiddleIconSpan(iconHomeBtn),
                    iconStartIndex, iconEndIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            );


            descriptionTextView.setText(spannableString);

        }catch (Exception e){
            Logger.w("Utils", "[renderWelcomeUIText] failed: "+e.getMessage());
        }
    }

    public static int dip2px(Context context, float dpValue) {
        try {
            final float scale = context.getResources().getDisplayMetrics().density;

            return (int) (dpValue * scale + 0.5f);
        }catch (Exception e){
            return (int) dpValue * 4;
        }
    }
}
