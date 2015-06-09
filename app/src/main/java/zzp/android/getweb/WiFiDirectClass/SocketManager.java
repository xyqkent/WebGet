package zzp.android.getweb.WiFiDirectClass;

/**
 * Created by yingqiang on 2015/5/27.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.ListData;

public class SocketManager {
    private ServerSocket server, serverCheck;
    private Handler handler = null;
    int port = 8801;//文件传输端口
    int portChcek = 8802;//ip地址传输端口
    int MaxRun = 5, RunCount = 0;//接受文件最大执行次数
    Boolean sendFlag = true;
    Socket s, data;

    public SocketManager(Handler handler) {
        this.handler = handler;
        try {
            this.server = new ServerSocket(port);
            this.serverCheck = new ServerSocket(portChcek);

        } catch (Exception e) {
            Log.e("SocketManager", e.toString());
        }
        sendFlag = true;
    }


    void SendMessage(int what, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }

    public void OpenServerSocket() {
        try {
            this.server = new ServerSocket(port);
            this.serverCheck = new ServerSocket(portChcek);

        } catch (Exception e) {
            Log.e("SocketManager", e.toString());
        }
        sendFlag = true;
    }

    public void CloseServerSocket() {
        try {
            server.close();
            serverCheck.close();
            s.close();
            data.close();
        } catch (Exception e) {
            Log.e("CloseServerSocket", e.toString());
        }
        sendFlag = false;
    }

    public String CheckClient() {
        BufferedReader in;
        try {
            SendMessage(4, "正在检查是否有传入连接");
            Socket check = serverCheck.accept();
            in = new BufferedReader(new InputStreamReader(check.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(check.getOutputStream());

            String str;
            str = in.readLine();//接受连接端ip地址
            out.println("已成功连接");
            out.flush();

            in.close();
            out.close();
            check.close();
            SendMessage(4, str);
            return str;
        } catch (Exception e) {
            SendMessage(4, "握手失败" + e.toString());
            return null;
        }
    }

    public boolean CheckServer(String HostIP, String ClientIP) {
        try {
            SendMessage(4, "发送连接标示");
            Socket check = new Socket(HostIP, 8802);
            BufferedReader in = new BufferedReader(new InputStreamReader(check.getInputStream()));
            PrintWriter out = new PrintWriter(check.getOutputStream());
            out.println(ClientIP);//发送连接端ip地址
            out.flush();
            SendMessage(4, in.readLine());
            out.close();
            in.close();
            check.close();
            return true;
        } catch (Exception e) {
            SendMessage(4, "握手失败" + e.toString());
            return false;
        }
    }

    //接收文件
    public boolean ReceiveFile(String savePath) {
        try {
            String filePath;
            Log.i("rec", "ReceiveFileTop");
            //接收文件名
            Socket s = server.accept();
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            ListData receive = (ListData) ois.readObject();
            ois.close();
            s.close();
            SendMessage(4, "正在接收:" + receive.getName());
            //接收文件数据
            Socket data = server.accept();
            InputStream dataStream = data.getInputStream();
            filePath = savePath + File.separator + receive.getName() + ".zip";
            receive.setPath(filePath);//接收文件时重新修改写入路径
            FileOutputStream file = new FileOutputStream(filePath, false);
            byte[] buffer = new byte[1024];
            int size = -1;
            while ((size = dataStream.read(buffer)) != -1) {
                file.write(buffer, 0, size);
            }
            file.close();
            dataStream.close();
            data.close();
            SendMessage(4, receive.getName() + " 接收完成");
            Message.obtain(ReceiveListFragment.mHandler, 1, receive).sendToTarget();
            RunCount = 0;
        } catch (Exception e) {
            Log.e("ReceiveFile", "接收错误:\n" + e.toString());
            if (RunCount > MaxRun) {
                SendMessage(3, null);
                return false;
            }
            RunCount++;
        }
        return true;
    }

    public void SendFile(List<ListData> sends, String ipAddress) {

        try {
            //TODO 超时跳出
            for (ListData send : sends) {
                if (!sendFlag) break;
                Log.i("sendfiles", send.getName());
                s = new Socket(ipAddress, port);
                s.setSoTimeout(5 * 1000);
                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                oos.writeObject(send);//写文件对象
                oos.close();
                s.close();
                SendMessage(4, "正在发送" + send.getName());
                Log.i("sendfiles", "正在发送" + send.getName());

                data = new Socket(ipAddress, port);
                data.setSoTimeout(5 * 1000);
                OutputStream outputData = data.getOutputStream();
                FileInputStream fileInput = new FileInputStream(send.getPath());
                int size = -1;
                byte[] buffer = new byte[1024];
                while ((size = fileInput.read(buffer, 0, 1024)) != -1) {
                    outputData.write(buffer, 0, size);
                }
                outputData.close();
                fileInput.close();
                data.close();
                SendMessage(4, send.getName() + " 发送完成");
                Message.obtain(SendListFragment.mHandler, 1, send).sendToTarget();//通知SendListFragment
                Log.i("sendfiles", send.getName() + " 发送完成");
            }
            if (sendFlag) {
                SendMessage(4, sends.size() + "个所有文件发送完成");
                Log.i("sendfiles", sends.size() + "个所有文件发送完成");
            } else {
                SendMessage(4, "发送文件错误");
                Log.i("sendfiles", "发送文件错误");
            }
        } catch (Exception e) {
            Log.i("sendfiles", "发送错误:\n" + e.getMessage());
        }
    }
}
