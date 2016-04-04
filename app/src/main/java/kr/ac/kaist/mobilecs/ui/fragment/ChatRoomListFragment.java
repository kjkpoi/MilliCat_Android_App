package kr.ac.kaist.mobilecs.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.ac.kaist.mobilecs.HttpManager;
import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.model.ChatRoom;
import kr.ac.kaist.mobilecs.ui.adapter.ChatRoomListAdapter;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link ChatRoomListFragment.OnChatRoomListFragmentInteractionListener}
 * interface.
 */
public class ChatRoomListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String TAG = "ChatRoomListFragment";
    // Reference to HomeActivity. Responsible for loading the chat rooms.
    private OnChatRoomListFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChatRoomListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Handle empty chat room list
        // TODO: Load Adapter with saved data
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chatroomlist, container, false);

        // Find list view
        mListView = (AbsListView) view.findViewById(android.R.id.list);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnChatRoomListFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        HttpManager.getRooms(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                try {
                    JSONArray rooms = response.getJSONArray("rooms");
                    Log.d(TAG, rooms.toString());

                    Gson gson = new Gson();
                    ChatRoom[] chatRooms = gson.fromJson(rooms.toString(), ChatRoom[].class);

                    // Set the adapter
                    List<ChatRoom> chatRoomList = new ArrayList<ChatRoom>();
                    chatRoomList.addAll(Arrays.asList(chatRooms));
                    mAdapter = new ChatRoomListAdapter(getActivity(), chatRoomList);
                    ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            ChatRoom chatRoom = (ChatRoom) mAdapter.getItem(position);
            mListener.onChatRoomListFragmentInteraction(chatRoom._id);
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

    public void addRoom(ChatRoom chatRoom) {
        ((ChatRoomListAdapter) mAdapter).add(chatRoom);
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
    public interface OnChatRoomListFragmentInteractionListener {
        public void onChatRoomListFragmentInteraction(String id);
    }

}