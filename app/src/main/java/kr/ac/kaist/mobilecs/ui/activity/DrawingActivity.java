package kr.ac.kaist.mobilecs.ui.activity;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.UUID;

import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.ui.view.DrawingView;


public class DrawingActivity extends ActionBarActivity {

    static final LightingColorFilter BACKGROUND_WHEN_SELECTED = new LightingColorFilter(0xAAAAAAAA, 0x00000000);

    private DrawingView drawView;
    private ImageButton currMode;
    private ImageButton currPaint;

    // Activity Modes
    final public static int DRAW = 0;
    final public static int TEXT = 1;
    final public static int IMAGE = 2;
    //final public static int ERASE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // turn off the async check
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.drawing_main);

        drawView = (DrawingView) findViewById((R.id.drawing));

        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paint_colors);
        currPaint = (ImageButton) paintLayout.getChildAt(0);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));

        // set currMode to paint
        currMode = (ImageButton) findViewById(R.id.draw_btn);
        currMode.setPressed(true);
        currMode.getBackground().setColorFilter(BACKGROUND_WHEN_SELECTED);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void modeSelected(View view) {

        if (view != currMode) {
            currMode.setPressed(false);
            currMode.getBackground().clearColorFilter();
            view.setPressed(true);
            view.getBackground().setColorFilter(BACKGROUND_WHEN_SELECTED);

            ImageButton imgView = (ImageButton) view;
            int mode = -1;

            drawView.setErase(false);
            int viewId = view.getId();
            if (viewId == R.id.insert_text_btn) {
                mode = TEXT;

            } else if (viewId == R.id.insert_image_btn) {
                mode = IMAGE;

            } else if (viewId == R.id.draw_btn) {
                mode = DRAW;
            } else if (viewId == R.id.erase_btn) {
                mode = DRAW;
                drawView.setErase(true);
            } else if (viewId == R.id.send_drawing_btn) {
                final Intent resultIntent = new Intent();
                // Send drawing by saving it to the SD card and passing the URI back to
                // the ChatRoomFragment
                AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
                saveDialog.setTitle(R.string.send);
                saveDialog.setMessage(R.string.send_drawing_confirm_messege);
                saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // attempt to save drawing
                        drawView.setDrawingCacheEnabled(true);
                        String randomTag = UUID.randomUUID().toString().replace("-", "");
                        String savedImageURL = MediaStore.Images.Media.insertImage(
                                getContentResolver(),
                                drawView.getDrawingCache(),
                                randomTag + ".png",
                                "drawing");
                        drawView.destroyDrawingCache();

                        // return result to ChatRoomFragment
                        resultIntent.putExtra("random_tag", randomTag);
                        resultIntent.putExtra("saved_image_url", savedImageURL);

                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    }
                });
                saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                saveDialog.show();
            }

            drawView.setMode(mode);

            currMode = imgView;
        }

    }

    public void paintClicked(View view) {
        // use chosen color
        if (view != currPaint) {
            // update color
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            drawView.setPaintColor(color);

            // Set Previous button back to normal
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            // update to reflect new chosen paint
            currPaint = (ImageButton) view;
        }
    }
}

