package kr.ac.kaist.mobilecs;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;


public class ImageNode {


    private String TAG = "ImageNode";

    /**
     * Factory method for this ImageNode class. Constructs an ImageNode using a search keyword.
     */

    public static ImageNode create(float x, float y, String keyword, int width, int height) {
        ImageNode im = new ImageNode(x, y, "", width, height);
        im.searchImgur(keyword);
        return im;
    }

    private float mPosX;
    private float mPosY;
    private float mPosXOrig;
    private float mPosYOrig;
    private int mWidth;
    private int mHeight;
    //private String mText;
    //public String mKeyword;
    private String mURL;
    final Bitmap[] bitmap = new Bitmap[1];

    public ImageNode() {

    }

    public ImageNode(float x, float y, String url, int width, int height) {
        mPosX = x;
        mPosY = y;
        mPosXOrig = x;
        mPosYOrig = y;
        mURL = url;
        mWidth = width;
        mHeight = height;
    }

    public ImageNode(float x, float y, Bitmap b, int width, int height) {
        mPosX = x;
        mPosY = y;
        mPosXOrig = x;
        mPosYOrig = y;
        bitmap[0] = b;
        mWidth = width;
        mHeight = height;
    }

    public float getX() {
        return mPosX;
    }

    public float getY() {
        return mPosY;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean withinConfines(float x, float y) {
        return (mPosX < x) && (x < (mPosX + mWidth)) && (mPosY < y) && (y < (mPosY + mHeight));
    }

    public void moveRelativeToOrigin(float x, float y) {
        mPosX = mPosXOrig + x;
        mPosY = mPosYOrig + y;
    }

    public void setOriginToCurr() {
        mPosXOrig = mPosX;
        mPosYOrig = mPosY;
    }

    public void searchImgur(String keyword) {
        Log.d("ImageNode", "Running searchImgur");
        Log.d("ImageNodeKeyword", keyword);
        //bitmap[0] = ImageLoader.getInstance().loadImageSync("http://kaist.egg.ovh/img/imFeelingLucky?keyword="+keyword);

        ImageLoader.getInstance().loadImage("http://kaist.egg.ovh/img/imFeelingLucky?keyword=" + keyword, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

                Log.d("searchImgur", "Loading started");
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                Log.d("searchImgur", "Loading failed!!!");
                failReason.getCause().printStackTrace();
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                bitmap[0] = loadedImage;
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        });

        /*HttpManager.imFeelingLucky(mURL, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                Log.d("ImageNode", "searchImgur Succeeded!!!!!!!!!");
                bitmap[0] = BitmapFactory.decodeByteArray(binaryData, 0, binaryData.length);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                Log.d("ImageNode", "imFeelingLucky failed!!!");
                error.printStackTrace();
            }
        });*/


    }

    public Bitmap getBitmap() {

        if (bitmap[0] == null) {
            return null;
        }
        return bitmap[0];
    }
}
