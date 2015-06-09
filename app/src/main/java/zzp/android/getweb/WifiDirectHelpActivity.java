package zzp.android.getweb;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Xiang on 2015/6/9.
 */
public class WifiDirectHelpActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct_help);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
