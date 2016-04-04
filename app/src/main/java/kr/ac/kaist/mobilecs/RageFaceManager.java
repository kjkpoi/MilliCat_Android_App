package kr.ac.kaist.mobilecs;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Alvin on 5/25/2015.
 */
public class RageFaceManager {
    private static Map<String, ArrayList<String>> tagToRageFaceMap = null;

    public static Bitmap getRageFaceByName(String name) {
        return ImageLoader.getInstance().loadImageSync("assets://rage-faces-width120/" + name + ".png");
    }

    public static ArrayList<String> getRageFaceURIsByTag(String tag, Context context) {
        // URIs are for ImageLoader

        if (tagToRageFaceMap == null) {
            try {
                tagToRageFaceMap = loadTagToRageFaceMap(context);
                Log.d("RageFaceManager", tagToRageFaceMap.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!tagToRageFaceMap.containsKey(tag)) {
            return new ArrayList<>();
        }

        return tagToRageFaceMap.get(tag);
    }

    private static Map<String, ArrayList<String>> loadTagToRageFaceMap(Context context) throws JSONException, IOException {

        //InputStream inputStream = context.getAssets().open("rage-face-tags.json");
        StringBuilder buf = new StringBuilder();
        InputStream json = context.getAssets().open("rage-face-tags.json");
        BufferedReader in =
                new BufferedReader(new InputStreamReader(json, "UTF-8"));
        String str;

        while ((str = in.readLine()) != null) {
            buf.append(str);
        }

        in.close();

        JSONObject rageFaceTags = new JSONObject(buf.toString());

        Map<String, ArrayList<String>> tagToRageMap = new HashMap<>();

        for (Iterator<String> iter = rageFaceTags.keys(); iter.hasNext(); ) {
            String filename = iter.next();
            JSONArray tags = rageFaceTags.getJSONArray(filename);
            String fullFilename = "assets://rage-faces-width120/" + filename;
            for (int i = 0; i < tags.length(); i++) {
                String tag = tags.getString(i);

                if (!tagToRageMap.containsKey(tag)) {
                    tagToRageMap.put(tag, new ArrayList<String>());
                }
                tagToRageMap.get(tag).add(fullFilename);
            }
        }

        return tagToRageMap;
    }

}