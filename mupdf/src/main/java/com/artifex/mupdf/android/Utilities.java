package com.artifex.mupdf.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.artifex.mupdf.fitz.R;

import java.lang.reflect.Method;

public class Utilities {
    public static void passwordDialog(final Activity activity, final passwordDialogListener listener) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                LayoutInflater li = LayoutInflater.from(activity);
                View promptsView = li.inflate(R.layout.password_prompt, null);

                final EditText et = (EditText) (promptsView.findViewById(R.id.editTextDialogUserInput));

                dialog.setView(promptsView);

                dialog.setTitle("");

                dialog.setPositiveButton(activity.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null) {
                            String password = et.getText().toString();
                            listener.onOK(password);
                        }
                    }
                });

                dialog.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null)
                            listener.onCancel();
                    }
                });

                dialog.create().show();
            }
        });
    }

    public interface passwordDialogListener {
        void onOK(String password);

        void onCancel();
    }

    //  this function returns the *actual* size of the screen.
    public static Point getRealScreenSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17) {
            //  new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //  reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("sonui", "Couldn't use reflection to get the real display metrics.");
            }
        } else {
            //this may not be 100% accurate, but it's all we've got
            realWidth = display.getWidth();
            realHeight = display.getHeight();
            Log.e("sonui", "Can't get real display matrix.");
        }

        return new Point(realWidth, realHeight);
    }

    public static int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) Math.round(px);
    }
}