package kr.ac.kaist.mobilecs.ui.fragment;

import android.app.Activity;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import kr.ac.kaist.mobilecs.HttpManager;
import kr.ac.kaist.mobilecs.Markup;
import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.SharedPreferencesManager;
import kr.ac.kaist.mobilecs.SocketIOClient;
import kr.ac.kaist.mobilecs.evaluation.Simulator;
import kr.ac.kaist.mobilecs.model.ChatMessage;
import kr.ac.kaist.mobilecs.ui.adapter.ChatRoomAdapter;
import kr.ac.kaist.mobilecs.ui.adapter.ImageAdapter;


/**
 * A fragment representing a list of Items(chat rooms).
 * Activities containing this fragment MUST implement the {@link ChatRoomFragment.OnChatRoomFragmentInteractionListener}
 * interface.
 */
public class ChatRoomFragment extends Fragment implements AbsListView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = "ChatRoomFragment";
    private static final int DRAW_ACTIVITY_REQ = 0;
    private OnChatRoomFragmentInteractionListener mListener;
    private AbsListView mListView;
    private ListAdapter mAdapter;
    private String roomId;
    private GridView mSuggestionGridView;
    private EditText mInput;
    private Button mSend;
    private Button mTest;
    private String mLastWord = ""; // the current last word in the input
    private ImageAdapter mSuggestionGridViewAdapter;
    private LruCache<String, Bitmap> mMemoryCache;

    private TextWatcher mInputTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }


        @Override
        public void afterTextChanged(final Editable s) {
            final String lastWord = getLastWord(s.toString());

            if (s.length() == 0) {
                mLastWord = null;
                mSuggestionGridViewAdapter.clear();
                mSuggestionGridView.setVisibility(View.GONE);
            }

            // don't do anything until last word has changed, and it's not a stop word
            if (lastWord != null && !lastWord.equals(mLastWord)) {
                // update word
                Log.d("Last Word", lastWord);
                mLastWord = lastWord;
                mSuggestionGridViewAdapter.clear();

                //Context context = getActivity().getBaseContext();

                //ArrayList rageFaceURIs = RageFaceManager.getRageFaceURIsByTag(lastWord, context);
                //mSuggestionGridViewAdapter.addAll(rageFaceURIs);

                // Load at most 3 images from Internet, if keyword is not a stop word, and not too short

                if (mSuggestionGridViewAdapter.getCount() < 3 &&
                        s.length() >= 3) {
                    loadImages(lastWord, 3 - mSuggestionGridViewAdapter.getCount());
                }

            }
        }
    };

    private Map<String, String> mTextState = new HashMap<String, String>();
    private String mSender;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChatRoomFragment() {
    }

    private void loadImages(final String lastWord, final int maxImages) {
        HttpManager.imageSearch2(lastWord, new JsonHttpResponseHandler() {
            private String searchWord = lastWord;

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d(TAG, lastWord + ": " + response.toString());
                if (mLastWord == null || !searchWord.equals(mLastWord)) {
                    // stale search result, do nothing
                    return;
                }

                try {
                    JSONArray images = response.getJSONArray("images");
                    int size = Math.min(images.length(), maxImages);
                    if(size == 0)
                        return;

                    for (int i = 0; i < size; i++) {
                        //mSuggestionGridViewAdapter.add(HttpManager.BASE_HTTP + "/img/" + images.getString(i));
                        mSuggestionGridViewAdapter.add(HttpManager.BASE_LOCAL + "/" + images.getString(i));
                    }

                    if (size > 0) {
                        // Tell the grid view to reload views
                        mSuggestionGridView.setVisibility(View.VISIBLE);
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

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        mSender = SharedPreferencesManager.getName(getActivity());

        String roomId = SharedPreferencesManager.getSharedPreferences(getActivity()).getString("RoomId", null);
        if (roomId == null) {

        } else {
            setRoom(roomId);
        }

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



        Ion.getDefault(getActivity().getApplicationContext()).configure().setLogging("ion", Log.DEBUG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_chatroom, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Open the DrawingActivity
            case R.id.test:
                new Simulator(getActivity().getBaseContext(), this.roomId, this.mSender, this.mListView, this.mAdapter);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Get back the user created drawing and process it here.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DRAW_ACTIVITY_REQ:
                if (resultCode == Activity.RESULT_OK) {
                    final String keyWord = data.getStringExtra("random_tag");
                    String savedDrawingURL = getPathFromURI(Uri.parse(data.getStringExtra("saved_image_url")));

                    File f = new File(savedDrawingURL);
                    Future uploading = Ion.with(this)
                            .load(HttpManager.BASE_HTTP + "/img/upload")
                            .setMultipartFile("image", f)
                            .setMultipartParameter("keyword", keyWord)
                            .asString()
                            .withResponse()
                            .setCallback(new FutureCallback<Response<String>>() {
                                @Override
                                public void onCompleted(Exception e, Response<String> result) {
                                    try {
                                        // display a success toast
                                        JSONObject jobj = new JSONObject(result.getResult());
                                        Toast.makeText(getActivity().getApplicationContext(),
                                                jobj.getString("response"),
                                                Toast.LENGTH_SHORT).show();

                                        // make markup and send message with embedded image url
                                        // make markup
                                        String message = String.format("[%s %s]",
                                                keyWord,
                                                HttpManager.BASE_HTTP + "/img/" + keyWord + "/" + keyWord);
                                        // call send
                                        SocketIOClient.postMessageToRoom(roomId, mSender, message);
                                        appendMessage(new ChatMessage(roomId, message, mSender));

                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }

                                }
                            });


                }
        }
    }

    private String getPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getActivity().getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void setRoom(String roomId) {
        this.roomId = roomId;
        if (!mTextState.containsKey(this.roomId)) {
            mTextState.put(this.roomId, "");
        }

        HttpManager.getMessageFromRoom(roomId, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "getMessageFromRoom" + response.toString());

                try {
                    JSONArray messages = response.getJSONArray("messages");

                    Gson gson = new Gson();
                    ChatMessage[] chatMessages = gson.fromJson(messages.toString(), ChatMessage[].class);

                    // First sort by date
                    Arrays.sort(chatMessages, new Comparator<ChatMessage>() {
                        @Override
                        public int compare(ChatMessage lhs, ChatMessage rhs) {
                            return lhs.timestamp.compareTo(rhs.timestamp);
                        }
                    });
                    // Set the adapter
                    List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
                    chatMessageList.addAll(Arrays.asList(chatMessages));
                    mAdapter = new ChatRoomAdapter(getActivity(), chatMessageList);
                    mListView.setAdapter(mAdapter);

                    mListView.post(new Runnable() {
                        @Override
                        public void run() {
                            mListView.smoothScrollToPosition(mAdapter.getCount() - 1);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);

            }
        });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatroom, container, false);

        // Find views
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mInput = (EditText) view.findViewById(R.id.input);
        mSend = (Button) view.findViewById(R.id.send);
        mTest = (Button) view.findViewById(R.id.test);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        mSend.setOnClickListener(this);
        mTest.setOnClickListener(this);
        mTest.setVisibility(View.INVISIBLE);
        mInput.addTextChangedListener(mInputTextWatcher);

        setEmptyText("No messages.");

        // The suggestions grid
        mSuggestionGridView = (GridView) view.findViewById(R.id.grid);
        mSuggestionGridViewAdapter = new ImageAdapter(getActivity(), new ArrayList<String>());
        mSuggestionGridViewAdapter.setMemoryCache(this.mMemoryCache);
        mSuggestionGridView.setAdapter(mSuggestionGridViewAdapter);
        mSuggestionGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("mSuggestionGridView", "Item clicked");
                Editable editable = mInput.getText();

                // Ensure space at end of string
                if (editable.charAt(mInput.length() - 1) != ' ') {
                    editable.append(' ');
                }

                // Build markup
                final ImageAdapter.ViewHolder viewHolder = (ImageAdapter.ViewHolder) view.getTag();
                String url = viewHolder.url;
                Markup markup = new Markup(mLastWord, url, editable);

                // Build SpannableString
                markup.apply();

                // Build imageSpan
                ImageSpan imageSpan = buildImageSpan(viewHolder.imageView.getDrawable());

                // Apply imageSpan to SpannableString
                editable.setSpan(imageSpan, markup.startIndex(), markup.endIndex(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                mInput.setText(editable);

                // refocus to get the cursor placed correctly
                mInput.setSelection(editable.length());

                // finally hide the suggestion bar
                mSuggestionGridViewAdapter.clear();
                mSuggestionGridView.setVisibility(View.GONE);
                mLastWord = "";
            }
        });

        return view;
    }

    private ImageSpan buildImageSpan(Drawable drawable) {
        int height = mInput.getLineHeight();
        int width = drawable.getIntrinsicWidth() * height / drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);
        return new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
    }

    private String getLastWord(String sentence) {
        if (sentence.length() == 0) {
            return null;
        }

        // Unsophisticated way to detect if last "word" is actually an image, if so ignore

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

        return words_no_punct[last];
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnChatRoomFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            ChatMessage chatMessage = (ChatMessage) mAdapter.getItem(position);
            mListener.onChatRoomFragmentInteraction(chatMessage._id);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.send) {
            String content = mInput.getText().toString();
            SocketIOClient.postMessageToRoom(roomId, mSender, content);
            appendMessage(new ChatMessage(roomId, content, mSender));

            mInput.setText("");
        } else if(v.getId() == R.id.test) {
            new Simulator(getActivity().getBaseContext(), this.roomId, this.mSender, this.mListView, this.mAdapter);
        }
    }

    public void appendMessage(final ChatMessage chatMessage) {
        if (chatMessage.roomId.equals(roomId)) {
            mListView.post(new Runnable() {
                @Override
                public void run() {
                    ((ChatRoomAdapter) mAdapter).add(chatMessage);
                }
            });
        }
    }

    public void saveTextState() {
        mTextState.put(this.roomId, mInput.getText().toString());
    }

    public void restoreTextState() {
        mInput.setText(mTextState.get(this.roomId));
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnChatRoomFragmentInteractionListener {
        public void onChatRoomFragmentInteraction(String id);
    }


}
