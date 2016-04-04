package kr.ac.kaist.mobilecs.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

import kr.ac.kaist.mobilecs.R;

/**
 * Created by william on 30/05/2015.
 */
public class ImageAdapter extends ArrayAdapter<String> {
    private LayoutInflater inflater;
    private DisplayImageOptions options;
    private LruCache<String, Bitmap> mMemoryCache;

    public ImageAdapter(Context context, ArrayList<String> mSuggestionImageUrls) {
        super(context, R.layout.chatroom_list_item, R.id.tv_text, mSuggestionImageUrls);
        inflater = LayoutInflater.from(context);

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.ic_error)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public void setMemoryCache(LruCache<String, Bitmap> mMemoryCache) {
        this.mMemoryCache = mMemoryCache;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        convertView = inflater.inflate(R.layout.suggested_image, parent, false);
        holder = new ViewHolder();
        holder.imageView = (ImageView) convertView.findViewById(R.id.image);
        holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progress);
        holder.url = getItem(position);
        convertView.setTag(holder);
        Bitmap tmp = getBitmapFromMemCache(holder.url);
        if(tmp != null) {
            holder.imageView.setImageBitmap(tmp);
            Log.d("MemoryCache", "MemoryCacheSize: " + mMemoryCache.size() + ", Cache hit: " + mMemoryCache.hitCount() + ", Cache miss: " + mMemoryCache.missCount());
        } else {
            ImageLoader.getInstance()
                    .displayImage(holder.url, holder.imageView, options, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            holder.progressBar.setProgress(0);
                            holder.progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            holder.progressBar.setVisibility(View.GONE);
                            remove(imageUri);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            holder.progressBar.setVisibility(View.GONE);
                            addBitmapToMemoryCache(holder.url, loadedImage);
                            Log.d("MemoryCache", "MemoryCacheSize: " + mMemoryCache.size() + ", Cache hit: " + mMemoryCache.hitCount() + ", Cache miss: " + mMemoryCache.missCount());
                        }
                    }, new ImageLoadingProgressListener() {
                        @Override
                        public void onProgressUpdate(String imageUri, View view, int current, int total) {
                            //holder.progressBar.setProgress(Math.round(100.0f * current / total));
                        }
                    });
        }
        holder.progressBar.setVisibility(View.GONE);
        return convertView;
    }

    public static class ViewHolder {
        public ImageView imageView;
        ProgressBar progressBar;
        public String url;
    }
}
