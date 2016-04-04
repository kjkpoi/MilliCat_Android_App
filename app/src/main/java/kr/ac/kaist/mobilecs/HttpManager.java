package kr.ac.kaist.mobilecs;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.InputStream;

/**
 * Created by william on 17/03/15.
 */
public class HttpManager {

    //public static final String BASE_HTTP = "http://kaist.egg.ovh";
    public static final String BASE_HTTP = "http://14.63.216.150:15000";
    public static final String BASE_LOCAL = "http://14.63.216.150:4567";
    //public static final String BASE_LOCAL = "http://143.248.56.39:4567";
    //public static final String BASE_HTTP = "http://kaist2.egg.ovh:9000";
    //public static final String BASE_HTTP = "http://192.168.56.102:9000";

    private static AsyncHttpClient client = new AsyncHttpClient();
    private static SyncHttpClient clientSync = new SyncHttpClient();


    public static void getMessageFromRoom(String roomId, JsonHttpResponseHandler handler) {
        RequestParams params = new RequestParams("roomId", roomId);
        client.get(BASE_HTTP + "/message", params, handler);
    }

    public static void createNewRoom(Context context, String name, JsonHttpResponseHandler handler) {
        RequestParams params = new RequestParams("name", name);
        client.post(context, BASE_HTTP + "/room", params, handler);
    }

    public static void getRooms(JsonHttpResponseHandler handler) {
        client.get(BASE_HTTP + "/room", handler);
    }

    public static void getRoomByHandle(String handle, JsonHttpResponseHandler handler) {
        client.get(BASE_HTTP + "/room/" + handle, handler);
    }

    public static void postMessageToRoom(String roomId, String sender, CharSequence message, JsonHttpResponseHandler handler) {
        RequestParams params = new RequestParams();
        params.put("sender", sender);
        params.put("content", message);

        client.post(BASE_HTTP + "/message?roomId=" + roomId, params, handler);
    }

    public static void imFeelingLucky(String keyword, View view, SimpleImageLoadingListener listener) {
        DisplayImageOptions options;

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.ic_error)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .displayer(new RoundedBitmapDisplayer(20)).build();

        ImageLoader.getInstance().displayImage(BASE_HTTP + "/img/imFeelingLucky?keyword=" + keyword,
                (android.widget.ImageView) view,
                options,
                listener);
    }

    public static void imageSearch(String keyword, JsonHttpResponseHandler handler) {
        client.get(BASE_HTTP + "/img/search?keyword=" + Uri.encode(keyword), handler);
    }

    public static void imageSearchSync(String keyword, JsonHttpResponseHandler handler) {
        try {
            clientSync.get(BASE_HTTP + "/img/search?keyword=" + Uri.encode(keyword), handler);
        } catch (Exception e) {

        }
    }

    public static void imageSearch2(String keyword, JsonHttpResponseHandler handler) {
        client.get(BASE_LOCAL + "/image/list/" + Uri.encode(keyword), handler);
        //client.get("http://14.63.216.150:4567/image/list/" + Uri.encode(keyword), handler);
        Log.d("Simulator", BASE_LOCAL + "/image/list/" + Uri.encode(keyword));
    }


    public static void imageSearchSync2(String keyword, JsonHttpResponseHandler handler) {
        try {
            clientSync.get("http://14.63.216.150:4567/image/list/" + Uri.encode(keyword), handler);
        } catch (Exception e) {

        }
    }

    public static void getImageByUrl(String url, BinaryHttpResponseHandler handler) {
        client.get(url, handler);
    }

    public static void postImage(InputStream inputStream, String keyWord, BinaryHttpResponseHandler handler) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("image", inputStream);

        requestParams.put("keyword", keyWord);  // should be an id but .....
        client.post(BASE_HTTP + "/img/", handler);
    }

}
