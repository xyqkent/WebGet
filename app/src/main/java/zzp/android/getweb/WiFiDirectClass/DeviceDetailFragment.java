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

package zzp.android.getweb.WiFiDirectClass;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.ListData;
import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.R;
import zzp.android.getweb.SaveViewActivity;
import zzp.android.getweb.WiFiDirectClass.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    public static Handler mHandler;
    String savePath;
    int dialogFlag = 1;
    SocketManager sm;
    TextView console;
    String ThisIP, otherIP;
    Thread tr1, tr2;
    Context mContext;
    UserFunction uf=new UserFunction();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        DBManger db = new DBManger();
        savePath = db.getSavePath(getActivity()) + File.separator + "Receives";//文件暂存点
        File receiveDir = new File(savePath);
        uf.delete(receiveDir);//清空文件夹内容
        if (!receiveDir.exists()) receiveDir.mkdirs();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //TODO 发送文件
                        final List<ListData> lds = (ArrayList<ListData>) msg.obj;

                        if (info.groupFormed && !info.isGroupOwner) {
                            otherIP = info.groupOwnerAddress.getHostAddress();
                        }
                        Thread tr = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                sm.SendFile(lds, otherIP);
                            }
                        });
                        if (otherIP != null) {
                            tr.start();
                        }
                        break;
                    case 2:
                        //设置对话框弹出设置
                        dialogFlag = (int) msg.obj;
                        break;
                    case 3:
                        sm.CloseServerSocket();//关闭Serversocket
                        tr1 = null;
                        tr2 = null;
                        break;
                    case 4://由SocketManager传递消息
                        ConsoleMessage(msg.obj.toString());
                        break;
                }
            }
        };

        console = (TextView) mContentView.findViewById(R.id.console);
        sm = new SocketManager(mHandler);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.wifi_direct_device_detail_frag, null);
        mContext = getActivity();
        return mContentView;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        Log.i("InfoAvailable", "top");
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        dialogFlag = 2;
        sm.OpenServerSocket();//重新绑定
        ThisIP = getThisDeviceIpAddress(info.groupOwnerAddress.getHostAddress());

        Message.obtain(SaveViewActivity.mHandler, 4, true).sendToTarget();//通知保存页面可以发送文件

        if (info.groupFormed && info.isGroupOwner) {
            if (tr1 == null) {
                tr1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        otherIP = sm.CheckClient();
                        while (sm.ReceiveFile(savePath)) {
                        }
                    }
                });
                tr1.start();
            }
        } else if (info.groupFormed) {
            if (tr2 == null) {
                tr2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sm.CheckServer(info.groupOwnerAddress.getHostAddress(), ThisIP);
                        while (sm.ReceiveFile(savePath)) {
                        }
                    }
                });
                tr2.start();
            }
        }
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void actionDialog(final WifiP2pDevice device) {
        this.device = device;
        switch (dialogFlag) {
            case 1:
                new AlertDialog.Builder(getActivity())
                        .setTitle("询问")
                        .setMessage("是否连接该设备？")
                        .setPositiveButton("连接", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                WifiP2pConfig config = new WifiP2pConfig();
                                config.deviceAddress = device.deviceAddress;
                                config.wps.setup = WpsInfo.PBC;
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                progressDialog = ProgressDialog.show(getActivity(), "按返回键取消", "连接" + device.deviceName + "中...", true, true,
                                        new DialogInterface.OnCancelListener() {

                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                ((DeviceActionListener) getActivity()).cancelDisconnect();
                                                dialogFlag = 1;
                                            }
                                        }
                                );
                                ((DeviceActionListener) getActivity()).connect(config);
                                dialogFlag = 0;
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                break;
            case 2:
                new AlertDialog.Builder(getActivity())
                        .setTitle("警告")
                        .setMessage("无法连接到此设备")
                        .setPositiveButton("确定", null)
                        .show();
                break;
        }
    }


    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        TextView view;
        view = (TextView) mContentView.findViewById(R.id.console);
        view.setText("");
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText("");
    }

    private void ConsoleMessage(String msg) {
        console.setText(console.getText() + "\n" + msg);
        Log.i("msg", msg);
    }

    public String getThisDeviceIpAddress(String HostIP) {
        String HostIPMask = HostIP.substring(0, HostIP.length() - 1);
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) en.nextElement();
                Enumeration<InetAddress> ee = ni.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress ia = (InetAddress) ee.nextElement();
//                    Log.i("MyIP Address", ia.getHostAddress());
                    if (ia.getHostAddress().startsWith(HostIPMask)) {
                        return ia.getHostAddress();
                    } else {
//                        Log.i("ClientSocket", "launching a new client thread");
                    }
                }
            }
        } catch (Exception e) {
            Log.i("transmittor", "I couldn't do it :( " + e);
        }
        return null;
    }
}
