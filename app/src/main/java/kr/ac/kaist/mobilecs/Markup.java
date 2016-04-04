package kr.ac.kaist.mobilecs;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by william on 31/05/2015.
 */
public class Markup {
    public static final Pattern PATTERN = Pattern.compile("\\[([^\\[]+?) ([^\\[]+?)\\]");

    private String word;
    private String url;
    private Editable editable;

    /**
     * See {@link #startIndex()}
     */
    private int startIndex = -1;

    public Markup(String word, String url, Editable editable) {
        this.word = word;
        this.url = url;
        this.editable = editable;
    }

    /**
     * Given a string with markups, strip them off and leave only the word.
     *
     * @param markupString Markup string.
     * @return String without markup.
     */
    public static String removeMarkup(String markupString) {
        boolean matched = false;
        Matcher matcher = PATTERN.matcher(markupString);

        // Replace each markup by the word
        StringBuffer s = new StringBuffer();
        while (matcher.find()) {
            matched = true;
            matcher.appendReplacement(s, matcher.group(1));
        }
        matcher.appendTail(s);

        return matched ? s.toString() : markupString;
    }

    /**
     * Given a markup string, inflate markups into images and apply the result on the {@code textView}.
     *
     * @param resources    Android Resources object.
     * @param textView     Where to apply the result.
     * @param markupString Markup string.
     * @return The {@code SpannableString} applied to the {@code textView}.
     */
    public static SpannableString inflate(final Resources resources, final TextView textView, String markupString) {
        boolean matched = false;
        final Matcher matcher = PATTERN.matcher(markupString);
        final SpannableString spannableString = new SpannableString(markupString);

        // Use this form because asynchronous
        int[] counter = {0};
        // Replace each markup by image
        while (matcher.find()) {
            matched = true;
            counter[0]++;
            loadImage(resources, matcher.group(2), matcher.start(), matcher.end(), counter, spannableString, textView);
        }

        if (!matched) {
            textView.setText(spannableString);
        }

        return spannableString;
    }

    /**
     * Given a URL, Asynchronously load the image, apply it to the {@code spannableString} between
     * {@code start} and {@code end}.
     * At the end, the last call is responsible to update the {@code textView}.
     *
     * @param resources       Android Resources object.
     * @param url             Image URL.
     * @param start           Starting position of the {@code spannableString}.
     * @param end             Ending position of the {@code spannableString}.
     * @param counter         Keep track of the last call of {@code loadImage}.
     * @param spannableString Containing the chat message.
     * @param textView        Where to apply the result.
     */
    private static void loadImage(final Resources resources, final String url, final int start, final int end, final int[] counter, final SpannableString spannableString, final TextView textView) {

        // by design, loadImage cancels older loadImage tasks when a new one uses the same URI
        // so we use the officially recommended NonViewAware workaround
        ImageSize imageSize = new ImageSize(0, 0);
        NonViewAware imageAware = new NonViewAware(imageSize, ViewScaleType.CROP);
        ImageLoader.getInstance().displayImage(url, imageAware, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);

                final Drawable d = new BitmapDrawable(resources, loadedImage);
                int height = textView.getLineHeight() * 2;
                int width = d.getIntrinsicWidth() * height / d.getIntrinsicHeight();
                d.setBounds(0, 0, width, height);
                final ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);

                spannableString.setSpan(span, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        // Show a bigger image on click
                        Drawable dd = d.mutate();
                        int height = textView.getLineHeight() * 8;
                        int width = dd.getIntrinsicWidth() * height / dd.getIntrinsicHeight();
                        dd.setBounds(0, 0, width, height);
                        ImageSpan imageSpan = new ImageSpan(dd, ImageSpan.ALIGN_BASELINE);

                        SpannableString ss = new SpannableString(" ");
                        ss.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

                        final Toast toast = Toast.makeText(widget.getContext(), ss, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Finally update view
                counter[0]--;
                if (counter[0] == 0) {
                    textView.setText(spannableString);
                }
            }
        });
    }

    /**
     * Replace the word by the markup.
     *
     * @return Markup string.
     */
    public Editable apply() {
        startIndex = startIndex();
        int endIndex = startIndex + word.length();

        // Apply markup to input string
        return editable.replace(startIndex, endIndex, toString());
    }

    /**
     * Compute the starting index of the markup.
     *
     * @return Starting index.
     */
    public int startIndex() {
        /**
         * Prevent this index to move after we performed {@link #apply()}
         */
        if (startIndex == -1) {
            startIndex = editable.toString().lastIndexOf(word);
        }
        return startIndex;
    }

    /**
     * Compute the ending index of the markup.
     *
     * @return Ending index.
     */
    public int endIndex() {
        return startIndex() + toString().length();
    }

    @Override
    public String toString() {
        return String.format("[%s %s]", word, url);
    }

}
