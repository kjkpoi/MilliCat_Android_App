package kr.ac.kaist.mobilecs.evaluation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.util.LruCache;
import android.widget.AbsListView;
import android.widget.ListAdapter;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import kr.ac.kaist.mobilecs.HttpManager;
import kr.ac.kaist.mobilecs.Markup;
import kr.ac.kaist.mobilecs.SocketIOClient;

/**
 * Created by kjk on 2015-10-12.
 */
public class Simulator {
    private static final int SAMPLE_NUMBER = 20;
    private static final String TEST_FILE = "happy.txt";
    private static final String IGNORE_LIST = "english-stop-word-list.txt";
    private static final String DIC_LIST = "en_US.txt";
    private static final String TAG = "Simulator";

    private String roomId;
    private String mSender;

    int totalCount = 0;
    long totalImageTime = 0;
    int totalKeywordRequest = 0;
    int totalLineNumber = 0;
    int totalChar = 0;
    int totalWord = 0;
    Context context;
    HashSet<String> keywordSet;
    HashSet<String> ignoreWordList;
    HashSet<String> dicList;
    Vector<Double> latencyList;

    private LruCache<String, Bitmap> mMemoryCache;
    private int cacheTotalCount = 0;
    private int cacheHitCount = 0;


    public Simulator(Context context, final String roomId, final String mSender, AbsListView mListView, final ListAdapter mAdapter) {

        this.context = context;
        System.setProperty("http.keepAlive", "false");

        this.roomId = roomId;
        this.mSender = mSender;

        latencyList = new Vector<>();
        keywordSet = new HashSet<>();
        ignoreWordList = new HashSet<>();
        dicList = new HashSet<>();

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(IGNORE_LIST)));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                ignoreWordList.add(mLine);
            }
            reader.close();

            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(DIC_LIST)));
            while ((mLine = reader.readLine()) != null) {
                dicList.add(mLine);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageLoading task = new ImageLoading();
        task.execute();
    }


    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }


    private String getLastWord(String sentence) {
        if (sentence.length() == 0) {
            return null;
        }

        String[] words = Markup.removeMarkup(sentence).split("\\s+");
        String[] words_ = sentence.split("\\s+");

        if (words.length == 0) {
            return null;
        }

        String word = words[words.length - 1];
        String word_ = words_[words_.length - 1];

        if (!word.equals(word_)) {
            return null;
        }

        // ignore punctuation
        if (sentence.length() == 0) {
            return null;
        }

        String[] words_no_punct = sentence.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
        int last = words_no_punct.length - 1;
        if(last < 0)
            return null;
        return words_no_punct[last];
    }

    public class ImageLoading extends AsyncTask<Void, Void, Void> {

        private long time;

        public ImageLoading(){
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            time = System.currentTimeMillis();
        }

        private void imageDownload(ArrayList<String> urlList, String keyword) {
            InputStream input = null;
            HttpURLConnection connection = null;

            int cnt;
            Log.d(TAG, "MemoryCacheSize: " + mMemoryCache.size() + ", Cache hit: " + mMemoryCache.hitCount() + ", Cache miss: " + mMemoryCache.missCount());
            Log.d(TAG, "MemoryCacheSize: " + mMemoryCache.size() + ", Cache hit: " + cacheHitCount + ", Cache miss: " + cacheTotalCount);
            for (String url : urlList) {
                cnt = 0;
                try {
                    URL httpUrl = new URL(url.replace(" ", "%20"));
                    cacheTotalCount++;
                    if(getBitmapFromMemCache(httpUrl.toString()) != null) {
                        Log.d(TAG, "Cache Hit!: "+ httpUrl.toString());
                        cacheHitCount++;
                        continue;
                    }
                    ByteArrayOutputStream imageByteArray = new ByteArrayOutputStream();
                    Bitmap bitmapImage;
                    while (cnt < 3) {
                            try {
                            connection = (HttpURLConnection) httpUrl.openConnection();
                            connection.setConnectTimeout(20000);
                            connection.setReadTimeout(20000);
                            connection.connect();


                            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                                Log.e(TAG, url + " -> HTTP Response Error");
                                throw new Exception("HTTP response Error");
                            }

                            // download the file
                            input = connection.getInputStream();

                            byte data[] = new byte[4096];
                            long total = 0;
                            int count;
                            while ((count = input.read(data)) != -1) {
                                if (isCancelled()) {
                                    input.close();
                                    throw new Exception("HTTP read Error");
                                }
                                imageByteArray.write(data);
                                total += count;
                            }

                            totalCount++;
                            Log.d(TAG, url + " loading complete, current image byte -> " + total);
                            bitmapImage = BitmapFactory.decodeByteArray(imageByteArray.toByteArray(), 0, imageByteArray.toByteArray().length);
                            addBitmapToMemoryCache(httpUrl.toString(), bitmapImage);
                        } catch (SocketTimeoutException | EOFException e) {
                            Log.e(TAG, url + ", EOFException -> " + e.getStackTrace());
                            cnt++;
                            SystemClock.sleep(200);
                            continue;
                        }
                        break;
                    }
                } catch (Exception e) {
                    Log.d(TAG, url + " ->" + e.toString());
                } finally {
                    try {
                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }

                    if (connection != null)
                        connection.disconnect();
                }
            }
        }

        private double getVariance()
        {
            double mean = 0;
            for(double a : latencyList)
                mean += a;
            mean = mean / latencyList.size();

            double temp = 0;
            for(double a : latencyList)
                temp += (mean-a)*(mean-a);
            return temp / (double)totalCount;
        }

        private void imageUrlDownload(final String keyword) {
            if (keyword == null) {
                Log.e(TAG, "Keyword Error");
                return;
            }

            try {
                HttpManager.imageSearchSync2(Uri.encode(keyword), new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            JSONArray images = response.getJSONArray("images");
                            int size = Math.min(images.length(), 3);

                            ArrayList<String> urlList = new ArrayList<>();
                            String url;
                            for (int i = 0; i < size; i++) {
                                url = "http://14.63.216.150:4567/" + images.getString(i);
                                //url = HttpManager.BASE_HTTP + "/img/" + images.getString(i);
                                urlList.add(url);
                            }

                            totalCount += urlList.size();
                            imageDownload(urlList, keyword);
                            totalKeywordRequest++;

                            long timeGap = 0;
                            if(urlList.size() > 0) {
                                timeGap = System.currentTimeMillis() - time;
                                totalImageTime += timeGap;
                                latencyList.add(timeGap / (double) urlList.size());
                            }

                            Log.d(TAG, "Avg latency -> " +  totalImageTime / (double)totalCount + ", Variance -> " + getVariance() + ", Std -> " + Math.sqrt(getVariance()) + ", totalCount -> " + totalCount + ", timeGap -> " + timeGap);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.d(TAG, keyword + " get fail -> " + responseString + ", " + statusCode);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "image search sync - > " + e.toString());
            }
        }


        @Override
        protected Void doInBackground(Void... Void) {
            String str;
            BufferedReader reader = null;

            for (int i = 1; i <= SAMPLE_NUMBER; i++) {
                Log.e(TAG, "Current text -> " + i);
                try {
                    //String fileName = TEST_FILE;
                    String fileName = "test_messages/" + i + ".txt";
                    reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));

                    // do reading, usually loop until end of file reading
                    String mLine;
                    String pureWord;
                    while ((mLine = reader.readLine()) != null) {
                        totalLineNumber++;
                        str = "";
                        Log.d(TAG, mLine);

                        SocketIOClient.postMessageToRoom(roomId, mSender, mLine);
                        for (char ch : (mLine + " ").toCharArray()) {

                            //evaluation for each word
                            if ((ch == ' ') && str.length() > 0) {
                                totalChar += str.length();
                                totalWord++;
                                pureWord = getLastWord(str);
                                // && !ignoreWordList.contains(pureWord) && dicList.contains(pureWord)
                                if (pureWord != null && pureWord.length() > 2) {
                                    Log.d(TAG, pureWord);
                                    SystemClock.sleep(100);
                                    time = System.currentTimeMillis();
                                    imageUrlDownload(pureWord);
                                }
                                str = "";
                                continue;
                            }

                            //evaluation for each char
                            /*pureWord = getLastWord(str);

                            if (pureWord != null && pureWord.length() > 2) {
                                Log.d(TAG, pureWord);
                                time = System.currentTimeMillis();
                                imageUrlDownload(pureWord);
                                SystemClock.sleep(300);
                            }

                            if(ch == ' ' && str.length() > 0){
                                str = "";
                            }
*/
                            str += ch;
                        }
                    }
                    Log.d(TAG, "totalLine -> " + totalLineNumber);
                    Log.d(TAG, "totalWord -> " + totalWord);
                    Log.d(TAG, "totalChar -> " + totalChar);
                }catch(IOException e){
                    Log.e(TAG, "File read -> " + e.toString());
                }finally{
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Close Error ->" + e.toString());
                        }
                    }
                }
                SystemClock.sleep(1000);
            }


            return null;
        }
    }

}
