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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.WiFiDirectClass.DeviceDetailFragment;
import zzp.android.getweb.WiFiDirectClass.DeviceListFragment;
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
    LinearLayout[] LL = new LinearLayout[3];
    int screenH, screenW;

    /**
     * 定义手势检测实例
     */
    public static GestureDetector detector;
    /**
     * 做标签，记录当前是哪个fragment
     */
    public int MARK = 0;
    /**
     * 定义手势两点之间的最小距离
     */
    final int DISTANT = 100;//dip单位

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
        detector = gesturedetector;

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        LL[0] = (LinearLayout) findViewById(R.id.LL0);
        LL[1] = (LinearLayout) findViewById(R.id.LL1);
        LL[2] = (LinearLayout) findViewById(R.id.LL2);

        screenW = this.getWindowManager().getDefaultDisplay().getWidth();

        LL[2].setTranslationX(-screenW);
        LL[0].setTranslationX(0);
        LL[1].setTranslationX(screenW);

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
//                fragment.getView().setVisibility(View.GONE);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        //将该Activity上触碰事件交给GestureDetector处理
        return detector.onTouchEvent(event);
    }

    private GestureDetector gesturedetector = new GestureDetector(new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            LL[2].setTranslationX(-screenW);
            LL[0].setTranslationX(0);
            LL[1].setTranslationX(screenW);
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            Log.i("onScroll distanceX", String.valueOf(distanceX));
//            Log.i("onScroll distanceX", String.valueOf(distanceY));
//            flipper.getCurrentView().setTranslationX(e2.getX());
            LL[0].setTranslationX(LL[0].getTranslationX() - (int)distanceX);
            LL[1].setTranslationX(LL[1].getTranslationX() - (int)distanceX);
            LL[2].setTranslationX(LL[2].getTranslationX() - (int)distanceX);
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {


        }

        //滑动时
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() > e2.getX())
                MoveView(30);
            else
                MoveView(-30);
//            flipper.showNext();
//            if (Math.abs(e1.getX() - e2.getX()) > uf.dip2px(mContext, DISTANT)) {
//                //画面向左移动
//                if (e1.getX() - e2.getX() > 0) {
//                    flipper.setInAnimation(mContext, R.anim.fragment_slide_left_enter);
//                    flipper.setOutAnimation(mContext, R.anim.fragment_slide_left_exit);
//                    flipper.showNext();
//                }
//                //画面向右移动
//                if (e1.getX() - e2.getX() <= 0) {
//                    flipper.setInAnimation(mContext, R.anim.fragment_slide_right_enter);
//                    flipper.setOutAnimation(mContext, R.anim.fragment_slide_right_exit);
//                    flipper.showPrevious();
//                }
//                String title = "未知";
//                switch (flipper.getDisplayedChild()) {
//                    case 0:
//                        title = "无线传输";
//                        break;
//                    case 1:
//                        title = "已发送文件";
//                        break;
//                    case 2:
//                        title = "已接收文件";
//                        break;
//                }
//                //Handler,arg1,arg2,obj
//                Message.obtain(MainTabActivity.mHandler, 1, 2, -1, title).sendToTarget();
//
//            }
            return false;
        }
    });

    private void MoveView(int speed) {
        new ScrollTask().execute(speed);
    }

    class ScrollTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... speed) {

            int offectX = (int) LL[0].getTranslationX();
            Log.i("ScrollTask", String.valueOf(offectX));
            // 根据传入的速度来滚动界面，当滚动到达左边界或右边界时，跳出循环。
            while (true) {
                if (speed[0] > 0) {
                    offectX += speed[0];
                    if (offectX > 0) {
                        offectX -= speed[0];
                        break;
                    }
                }
                if (speed[0] < 0) {
                    offectX += speed[0];
                    if (offectX < 0) {
                        offectX -= speed[0];
                        break;
                    }
                }
                publishProgress(speed[0]);
                // 为了要有滚动效果产生，每次循环使线程睡眠20毫秒，这样肉眼才能够看到滚动动画。
                sleep(20);
            }
            return offectX;
        }

        @Override
        protected void onProgressUpdate(Integer... offectX) {
            MoveLL(offectX[0]);
        }

        @Override
        protected void onPostExecute(Integer offectX) {
            MoveLL(offectX);
            Log.i("onPostExecute",String.valueOf(LL[0].getTranslationX()));
        }

        private void MoveLL(int offectX) {
            LL[0].setTranslationX(LL[0].getTranslationX() + offectX);
            LL[1].setTranslationX(LL[1].getTranslationX() + offectX);
            LL[2].setTranslationX(LL[2].getTranslationX() + offectX);
        }
    }

    /**
     * 使当前线程睡眠指定的毫秒数。
     *
     * @param millis 指定当前线程睡眠多久，以毫秒为单位
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
