/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zzp.android.getweb;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import android.view.KeyEvent;

import android.view.View;

import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.WiFiDirectClass.DeviceDetailFragment;
import zzp.android.getweb.WiFiDirectClass.DeviceListFragment;
import zzp.android.getweb.WiFiDirectClass.ReceiveListFragment;
import zzp.android.getweb.WiFiDirectClass.SendListFragment;
import zzp.android.getweb.WiFiDirectClass.WiFiDirectBroadcastReceiver;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceListFragment.DeviceActionListener {

    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private Context mContext;
    public static Handler mHandler;
    UserFunction uf = new UserFunction();

    private ViewPager viewPager;
    private List<View> lists = new ArrayList<View>();
    private ViewPagerAdapter adapter;

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct);
        mContext = this;

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        lists.add(getLayoutInflater().inflate(R.layout.wifi_direct_tab_device, null));
        lists.add(getLayoutInflater().inflate(R.layout.wifi_direct_tab_send, null));
        lists.add(getLayoutInflater().inflate(R.layout.wifi_direct_tab_receive, null));

        adapter = new ViewPagerAdapter(lists);
        viewPager = (ViewPager) findViewById(R.id.vPager);
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (!isWifiP2pEnabled) {
                            Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                        fragment.onInitiateDiscovery();
                        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Toast.makeText(WiFiDirectActivity.this, "搜索开始", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                Toast.makeText(WiFiDirectActivity.this, "搜索失败，错误代码 : " + reasonCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case 2:
                        disconnect();//关闭WLANDirect连接
                        break;
                }
            }
        };
    }

    /**
     * register the BroadcastReceiver with the intent values to be matched
     */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        unregisterReceiver(receiver);
//    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
        Message.obtain(DeviceDetailFragment.mHandler, 3, null).sendToTarget();//关闭所有连接端口
        Message.obtain(SaveViewActivity.mHandler, 4, false).sendToTarget();//通知saveview隐藏发送按钮
        Message.obtain(SendListFragment.mHandler, 2, null).sendToTarget();//通知SendListFragment清空数据
    }

    @Override
    public void actionDialog(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.actionDialog(device);
    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {

                Message.obtain(DeviceDetailFragment.mHandler, 2, 2).sendToTarget();
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "连接失败，请重试.", Toast.LENGTH_SHORT).show();
                //向DeviceDetailFragment的dialogFlag标记写入1
                Message.obtain(DeviceDetailFragment.mHandler, 2, 1).sendToTarget();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Message.obtain(DeviceDetailFragment.mHandler, 2, 2).sendToTarget();
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                Message.obtain(DeviceDetailFragment.mHandler, 2, 1).sendToTarget();
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "通道丢失，正在重新初始化", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this, "错误，专用通道或被占用，请重启WLAN直连功能", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "终止连接成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "终止连接中断，错误代码: " + reasonCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private long exitTime;


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) // System.currentTimeMillis()无论何时调用，肯定大于2000
            {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_LONG).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public class ViewPagerAdapter extends PagerAdapter {

        List<View> viewLists;

        public ViewPagerAdapter(List<View> lists) {
            viewLists = lists;
        }

        @Override
        public int getCount() {                                                                 //获得size
            // TODO Auto-generated method stub
            return viewLists.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            // TODO Auto-generated method stub
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(View view, int position, Object object)                       //销毁Item
        {
            ((ViewPager) view).removeView(viewLists.get(position));
        }

        @Override
        public Object instantiateItem(View view, int position)                                //实例化Item
        {
            ((ViewPager) view).addView(viewLists.get(position), 0);

            return viewLists.get(position);
        }

    }
}
