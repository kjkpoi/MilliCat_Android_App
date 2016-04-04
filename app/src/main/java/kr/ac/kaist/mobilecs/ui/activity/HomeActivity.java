package kr.ac.kaist.mobilecs.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import kr.ac.kaist.mobilecs.HttpManager;
import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.SocketIOClient;
import kr.ac.kaist.mobilecs.model.ChatMessage;
import kr.ac.kaist.mobilecs.model.ChatRoom;
import kr.ac.kaist.mobilecs.ui.fragment.ChatRoomFragment;
import kr.ac.kaist.mobilecs.ui.fragment.ChatRoomListFragment;


public class HomeActivity extends ActionBarActivity implements ChatRoomFragment.OnChatRoomFragmentInteractionListener, ChatRoomListFragment.OnChatRoomListFragmentInteractionListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final Gson gson = new Gson();

        SocketIOClient.connect(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final JSONObject arg = (JSONObject) args[0];

                if (args.length > 0) {
                    final ChatMessage chatMessage = gson.fromJson(arg.toString(), ChatMessage.class);

                    ((ChatRoomFragment) mSectionsPagerAdapter.getItem(1)).appendMessage(chatMessage);
                }
            }
        });

        // Setup ImageLoader
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(getApplicationContext());
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setTitle(mSectionsPagerAdapter.getPageTitle(position));
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
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
        } else if (id == R.id.action_add) {
            final EditText editText = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("Create a new room")
                    .setView(editText)
                    .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String roomName = editText.getText().toString();

                            HttpManager.createNewRoom(HomeActivity.this, roomName, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    super.onSuccess(statusCode, headers, response);

                                    try {
                                        JSONObject room = response.getJSONObject("room");

                                        Gson gson = new Gson();
                                        ChatRoom chatRoom = gson.fromJson(room.toString(), ChatRoom.class);

                                        ChatRoomListFragment chatRoomListFragment = (ChatRoomListFragment) mSectionsPagerAdapter.getItem(0);
                                        chatRoomListFragment.addRoom(chatRoom);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onChatRoomListFragmentInteraction(String id) {
        // Update chat room view
        ChatRoomFragment chatRoomFragment = (ChatRoomFragment) mSectionsPagerAdapter.getItem(1);
        chatRoomFragment.setRoom(id);
        chatRoomFragment.restoreTextState();
        mViewPager.setCurrentItem(1);
    }

    @Override
    public void onChatRoomFragmentInteraction(String id) {

    }

    @Override
    public void onBackPressed() {
        // When on other page go back to first page else normal behaviour
        switch (mViewPager.getCurrentItem()) {
            case 0:
                super.onBackPressed();
                break;
            case 1:
                // Update chat room view
                ChatRoomFragment chatRoomFragment = (ChatRoomFragment) mSectionsPagerAdapter.getItem(1);
                chatRoomFragment.saveTextState();
                mViewPager.setCurrentItem(0);
                break;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Fragment[] fragments = new Fragment[2];

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            fragments[0] = new ChatRoomListFragment();
            fragments[1] = new ChatRoomFragment();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_chatlist).toUpperCase(l);
                case 1:
                    return getString(R.string.title_chatroom).toUpperCase(l);
            }
            return null;
        }
    }

}
