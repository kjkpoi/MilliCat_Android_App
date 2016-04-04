package kr.ac.kaist.mobilecs.ui.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.ListIterator;

import kr.ac.kaist.mobilecs.HttpManager;
import kr.ac.kaist.mobilecs.ImageNode;
import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.TextNode;
import kr.ac.kaist.mobilecs.ui.activity.DrawingActivity;
import kr.ac.kaist.mobilecs.ui.adapter.ImageAdapter;

/**
 * Created by Alvin on 5/7/2015.
 */
public class DrawingView extends View {

    private static final String TAG = "DrawingView";

    //private final View mGridView;
    private GridView mGridView;
    private ImageAdapter mSuggestionListViewAdapter;


    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int mPaintColor = 0xFF660000;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    // paints for text
    private Paint textFillPaint = new Paint();
    private Paint textBorderPaint = new Paint();

    // all the created text nodes
    private ArrayList<TextNode> textNodes = new ArrayList<TextNode>();
    private ArrayList<ImageNode> imageNodes = new ArrayList<ImageNode>();

    // gesture detector
    private GestureDetectorCompat mDetector;

    //last x,y touch coordinates
    private float mTouchX;
    private float mTouchY;
    private boolean mErase;
    private float mImageDragStartX;
    private float mImageDragStartY;
    private boolean mImageDragOn = false;
    private ImageNode mDragImageNode;

    // gesture detector class
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";
        final Context myContext;
        final View myView;

        public MyGestureListener(Context context, View view) {
            myContext = context;
            myView = view;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG, "onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());

            if (mMode == DrawingActivity.TEXT) {
                mTouchX = event.getX();
                mTouchY = event.getY();

                AlertDialog.Builder alert = new AlertDialog.Builder(myContext);

                alert.setTitle("Text Input");
                alert.setMessage("Say something!");

                // Set an EditText view to get user input
                final EditText input = new EditText(myContext);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        Log.i("DrawingView", value);
                        Log.i("DrawingView", textNodes.toString());
                        textNodes.add(new TextNode(mTouchX, mTouchY, value));
                        invalidate();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            } else if (mMode == DrawingActivity.IMAGE) {
                mTouchX = event.getX();
                mTouchY = event.getY();

                final EditText input = (EditText) mDialogView.findViewById(R.id.image_search_text);
                final LinearLayout suggestions = (LinearLayout) mDialogView.findViewById(R.id.suggestions);

                if (mAlert == null) {

                    AlertDialog.Builder alert_builder = new AlertDialog.Builder(myContext);

                    alert_builder = alert_builder.setTitle("Place an image!");
                    alert_builder = alert_builder.setMessage("Search for: ");

                    alert_builder = alert_builder.setView(mDialogView);
                    Button search_btn = (Button) mDialogView.findViewById(R.id.image_search_btn);

                    // load images into suggestions list view
                    search_btn.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mSuggestionListViewAdapter.clear();
                            String keyword = input.getText().toString();
                            loadImages(keyword, 10);
                        }
                    });
                    /*
                    alert_builder = alert_builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, int whichButton) {
                            mSuggestionListViewAdapter.clear();
                            mGridView.setVisibility(View.GONE);
                        }
                    });
                    */

                    alert_builder = alert_builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Cancelled; retain loaded images, if any
                            //mSuggestionListViewAdapter.clear();
                            //mGridView.setVisibility(View.GONE);
                        }
                    });

                    mAlert = alert_builder.create();
                }
                mAlert.show();
            }

            return true;
        }

        @Override
        public void onShowPress(MotionEvent event) {
            Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
            //return true;
        }


        @Override
        public void onLongPress(MotionEvent event) {
            super.onLongPress(event);
            Log.d(DEBUG_TAG, "onLongPress: " + event.toString());

        }
    }

    // for the mDialog
    private View mDialogView;  // the view for the image search
    private AlertDialog mAlert;  // the image search dialog
    private int mMode;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
        mDetector = new GestureDetectorCompat(context, new MyGestureListener(context, this));
        mDetector.setIsLongpressEnabled(true);

        Activity host = (Activity) getContext();
        mDialogView = host.getLayoutInflater().inflate(R.layout.image_suggestion, null);
        mGridView = (GridView) mDialogView.findViewById(R.id.suggestions_list);
        mSuggestionListViewAdapter = new ImageAdapter(getContext(), new ArrayList<String>());
        mGridView.setAdapter(mSuggestionListViewAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Item clicked");

                // Build markup
                final ImageAdapter.ViewHolder viewHolder = (ImageAdapter.ViewHolder) view.getTag();
                String url = viewHolder.url;
                Bitmap bitmap = ((BitmapDrawable) viewHolder.imageView.getDrawable()).getBitmap();

                // TODO put this with on positive button
                addImageNode(bitmap);

                // clear the suggestions and hide the alert
                ((EditText) mDialogView.findViewById(R.id.image_search_text)).setText("");
                mSuggestionListViewAdapter.clear();
                mGridView.setVisibility(View.GONE);

                mAlert.dismiss();


                invalidate();
            }
        });
        //this.setOnTouchListener(mDetector);
    }


    private void setupDrawing() {
        // setup paint for text
        textFillPaint.setTextSize(72f);
        textBorderPaint.setTextSize(72f);

        textFillPaint.setColor(Color.WHITE);
        textBorderPaint.setStyle(Paint.Style.STROKE);
        textBorderPaint.setStrokeWidth(8);
        textBorderPaint.setColor(Color.BLACK);

        //get drawing area setup for interaction
        drawPath = new Path();
        drawPaint = new Paint();

        drawPaint.setColor(mPaintColor);

        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
        canvasPaint.setColor(Color.DKGRAY);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

        if (drawPaint != null)
            canvas.drawPath(drawPath, drawPaint);

        for (ImageNode i_n : imageNodes) {

            try {
                Rect my_rect = new Rect(
                        (int) i_n.getX(),
                        (int) i_n.getY(),
                        (int) i_n.getX() + i_n.getWidth(),
                        (int) i_n.getY() + i_n.getHeight());
                canvas.drawBitmap(i_n.getBitmap(), null, my_rect, null);
            } catch (Exception e) {
                Log.d("onDraw", "image node no bitmap");
                //e.printStackTrace();
            }
        }

        for (TextNode tn : textNodes) {
            canvas.drawText(tn.getText(), 0, tn.getText().length(), tn.getX(), tn.getY(), textBorderPaint);
            canvas.drawText(tn.getText(), 0, tn.getText().length(), tn.getX(), tn.getY(), textFillPaint);

        }
    }


    private void addImageNode(Bitmap bitmap) {
        // target width height for display
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > 600) {
            height = (int) (height * 600.0 / width);
            width = 600;
        }
        // put image in center of touch
        imageNodes.add(new ImageNode(mTouchX - 150, mTouchY - 150, bitmap, width, height));
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // check if within an image node
        // TODO clean up all this motion detection stuff
        // the only reason mDetector is in the code is for longpress which is currently no longer
        // used
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchX = event.getX();
            mTouchY = event.getY();

            for (ListIterator it = imageNodes.listIterator(imageNodes.size()); it.hasPrevious(); ) {
                ImageNode i_n = (ImageNode) it.previous();
                if (i_n.withinConfines(mTouchX, mTouchY)) {
                    Log.d(TAG, "Pressed within ImageNode!");
                    mImageDragStartX = mTouchX;
                    mImageDragStartY = mTouchY;
                    mDragImageNode = i_n;
                    mImageDragOn = true;

                    //return true;
                }
            }
        } else if (mImageDragOn && event.getAction() == MotionEvent.ACTION_UP) {
            mImageDragOn = false;
            mDragImageNode.setOriginToCurr();
            invalidate();
            return true;
        } else if (mImageDragOn && event.getAction() == MotionEvent.ACTION_MOVE) {
            mTouchX = event.getX();
            mTouchY = event.getY();

            mDragImageNode.moveRelativeToOrigin(mTouchX - mImageDragStartX, mTouchY - mImageDragStartY);
            invalidate();
            return true;
        }

        // use this if using gesture detector
        if (mMode != DrawingActivity.DRAW) {
            return this.mDetector.onTouchEvent(event); // it works this way
        }
        //return super.onTouchEvent(event); Claimed by Android documentation to be necessary, but only works if removed.
        // handling line drawing here
        else if (mMode == DrawingActivity.DRAW) {
            mTouchX = event.getX();
            mTouchY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    drawPath.moveTo(mTouchX, mTouchY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    drawPath.lineTo(mTouchX, mTouchY);
                    break;
                case MotionEvent.ACTION_UP:
                    drawPath.lineTo(mTouchX, mTouchY);
                    drawCanvas.drawPath(drawPath, drawPaint);
                    drawPath.reset();
                    break;
                default:
                    return false;
            }
            invalidate();
        }
        return true;
    }

    public void setMode(int mode) {
        // mode "DRAW", "TEXT", "IMAGE"
        mMode = mode;
    }

    public void setPaintColor(String newColor) {
        //set color
        invalidate();
        mPaintColor = Color.parseColor(newColor);
        drawPaint.setColor(mPaintColor);
    }

    //set erase true or false
    public void setErase(boolean isErase) {
        // Erasing is implemented as drawing with White
        mErase = isErase;
        if (mErase) {
            drawPaint.setColor(Color.WHITE);
        } else {
            drawPaint.setColor(mPaintColor);
        }
    }

    private void loadImages(final String keyword, final int maxImages) {
        HttpManager.imageSearch(keyword, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                    JSONArray images = response.getJSONArray("images");
                    int size = Math.min(images.length(), maxImages);

                    for (int i = 0; i < size; i++) {
                        mSuggestionListViewAdapter.add(HttpManager.BASE_HTTP + "/img/" + images.getString(i));
                    }

                    if (size > 0) {
                        // Tell the grid view to reload views
                        mGridView.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(TAG, responseString);
            }

        });
    }

    private void clearDialog() {

    }
}
