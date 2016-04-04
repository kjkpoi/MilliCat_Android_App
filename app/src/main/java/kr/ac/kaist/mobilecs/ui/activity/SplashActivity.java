package kr.ac.kaist.mobilecs.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.SharedPreferencesManager;

/**
 * Splash screen
 * If this is the user's first time running the app, will prompt for username.
 */
public class SplashActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        boolean loggedIn = SharedPreferencesManager.isLoggedIn(this);
        if (loggedIn) {
            switchActivity();
        } else {
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.container, new LoginFragment())
                        .commit();
            }
        }
    }

    private void switchActivity() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
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

    public class LoginFragment extends Fragment {

        public LoginFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_login, container, false);

            final EditText et_name = (EditText) rootView.findViewById(R.id.name);
            Button confirm = (Button) rootView.findViewById(R.id.confirm);

            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = et_name.getText().toString().trim();

                    if (name.isEmpty()) {

                    } else {
                        SharedPreferencesManager.setName(getActivity(), name);
                        SharedPreferencesManager.isLoggedIn(getActivity(), true);

                        switchActivity();
                    }

                }
            });

            return rootView;
        }
    }
}
