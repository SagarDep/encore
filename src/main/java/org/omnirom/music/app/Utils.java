package org.omnirom.music.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.TypedValue;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

/**
 * Utilities
 */
public class Utils {

    /**
     * Calculates and return the action bar height based on the current theme
     *
     * @param theme The active theme
     * @param res   The resources context
     * @return The height of the action bar, in pixels
     */
    public static int getActionBarHeight(Resources.Theme theme, Resources res) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight += TypedValue.complexToDimensionPixelSize(tv.data, res.getDisplayMetrics());
        }

        // As we are a "fullscreen" activity, the actionbar is also the statusbar
        actionBarHeight += getStatusBarHeight(res);

        return actionBarHeight;
    }

    /**
     * @param res The resources context
     * @return The height of the status bar, in pixels
     */
    public static int getStatusBarHeight(Resources res) {
        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return res.getDimensionPixelSize(resourceId);
        } else {
            return 0;
        }
    }

    /**
     * Blurs and dims (darken) a provided bitmap.
     * Note that this method recreates and reallocates RenderScript data, so it is not a good idea
     * to use it where performance is critical.
     *
     * @param context The application context
     * @param inBmp The input bitmap
     * @param radius The blur radius, max 25
     * @return A blurred and dimmed copy of the input bitmap
     */
    public static Bitmap blurAndDim(Context context, Bitmap inBmp, float radius) {
        if (inBmp == null) {
            throw new IllegalArgumentException("blurAndDim: The input bitmap is null!");
        }

        RenderScript renderScript = RenderScript.create(context);
        ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));

        final int scaledW = inBmp.getWidth() / 2;
        final int scaledH = inBmp.getHeight() / 2;

        Allocation input = Allocation.createFromBitmap(renderScript,
                Bitmap.createScaledBitmap(inBmp, scaledW, scaledH, false));
        Allocation output = Allocation.createTyped(renderScript, input.getType());

        intrinsicBlur.setInput(input);
        intrinsicBlur.setRadius(radius);

        intrinsicBlur.forEach(output);

        // Dim down images with a tint color
        input.destroy();
        input = Allocation.createFromBitmap(renderScript,
                Bitmap.createScaledBitmap(Bitmap.createBitmap(new int[]{0x70000000},
                                1, 1, Bitmap.Config.ARGB_8888),
                        scaledW, scaledH, false
                )
        );

        ScriptIntrinsicBlend intrinsicBlend = ScriptIntrinsicBlend.create(renderScript,
                Element.U8_4(renderScript));
        intrinsicBlend.forEachSrcOver(input, output);

        Bitmap outBmp = Bitmap.createBitmap(scaledW, scaledH, inBmp.getConfig());
        output.copyTo(outBmp);

        input.destroy();
        output.destroy();
        intrinsicBlur.destroy();
        intrinsicBlend.destroy();
        renderScript.destroy();

        return outBmp;
    }

    /**
     * Format milliseconds into an human-readable track length.
     * Examples:
     *  - 01:48:24 for 1 hour, 48 minutes, 24 seconds
     *  - 24:02 for 24 minutes, 2 seconds
     *  - 52s for 52 seconds
     * @param timeMs The time to format, in milliseconds
     * @return A formatted string
     */
    public static String formatTrackLength(int timeMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
                - TimeUnit.HOURS.toSeconds(hours)
                - TimeUnit.MINUTES.toSeconds(minutes);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%02ds", seconds);
        } else {
            return "N/A";
        }
    }

    /**
     * Calculates the RMS audio level from the provided short sample extract
     *
     * @param audioData The audio samples
     * @return The RMS level
     */
    public static int calculateRMSLevel(short[] audioData, int numframes) {
        long lSum = 0;
        int numread = 0;
        for (short s : audioData) {
            lSum = lSum + s;
            numread++;
            if (numread == numframes) break;
        }

        double dAvg = lSum / numframes;
        double sumMeanSquare = 0d;

        numread = 0;
        for (short anAudioData : audioData) {
            sumMeanSquare = sumMeanSquare + Math.pow(anAudioData - dAvg, 2d);
            numread++;
            if (numread == numframes) break;
        }

        double averageMeanSquare = sumMeanSquare / numframes;

        return (int) (Math.pow(averageMeanSquare, 0.5d) + 0.5);
    }

    /**
     * Shows a short Toast style message
     * @param context The application context
     * @param res The String resource id
     */
    public static void shortToast(Context context, int res) {
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show();
    }
}
