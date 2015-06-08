package zzp.android.getweb.WiFiDirectClass;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.LVAdapter;
import zzp.android.getweb.OwnClass.ListData;
import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.OwnClass.ZipUtils;
import zzp.android.getweb.R;
import zzp.android.getweb.WebViewActivity;

/**
 * Created by Xiang on 2015/6/1.
 */
public class SendListFragment extends ListFragment {
    View mContentView = null;
    LinearLayout clear_all;
    ListView sendList;
    ArrayList<ListData> s_list = new ArrayList<ListData>();
    Context mContext;
    LVAdapter mAdapter;
    DBManger dbM = new DBManger();
    Boolean cbShow = false;
    public static Handler mHandler;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        sendList = (ListView) mContentView.findViewById(android.R.id.list);
        clear_all = (LinearLayout) mContentView.findViewById(R.id.clear_all);

        sendList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getActivity().onTouchEvent(event);
                return false;
            }
        });
        clear_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (s_list.size() == 0) Toast.makeText(getActivity(), "列表中无任何文件", Toast.LENGTH_SHORT).show();
                else
                    new AlertDialog.Builder(getActivity())
                            .setTitle("询问")
                            .setMessage("是否清空发送列表？")
                            .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Message.obtain(mHandler, 2, null).sendToTarget();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
            }
        });
        mAdapter = new LVAdapter(mContext, s_list, false);
        sendList.setAdapter(mAdapter);
        sendList.setOnItemClickListener(sendList_ItemClickListener);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //更新数据
                        ListData tmpLD = (ListData) msg.obj;
                        s_list.add(tmpLD);
                        sendList.setAdapter(new LVAdapter(mContext, s_list, false));
                        break;
                    case 2://清空数据
                        s_list.clear();
                        sendList.setAdapter(new LVAdapter(mContext, s_list, false));
                        break;

                }
            }
        };
    }

    private AdapterView.OnItemClickListener sendList_ItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final int pis = position;
            final String savePath = dbM.getSavePath(getActivity());
            final UserFunction uf = new UserFunction();
            Thread thrd = new Thread(new Runnable() {
                @Override
                public void run() {
                    final Context context = mContext;
                    int position = pis;
                    String path = s_list.get(position).getPath();
                    final String id = s_list.get(position).getID();
                    File file = new File(path);
                    // 文件存在并可读
                    if (file.exists() && file.canRead()) {
                        String viewPath = savePath + File.separator + "view";
                        File viewDir = new File(viewPath);
                        uf.delete(viewDir);
                        try {
                            ZipUtils.unzip(file.getAbsolutePath(), viewPath);
                        } catch (Exception e) {
                            new AlertDialog.Builder(context).setTitle("错误")
                                    .setMessage("尝试打开文件失败\n" + e.toString())
                                    .setPositiveButton("确定", null).show();
                            return;
                        }
                        File index = new File(savePath + File.separator + "view/index.html");
                        Intent intent = new Intent();
                        intent.setClass(context, WebViewActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("ListData", s_list.get(position));
                        intent.putExtras(bundle);
                        intent.putExtra("SFFlag", false);
                        intent.setDataAndType(Uri.fromFile(index), "html");
                        startActivity(intent);
                    }
                }
            });
            if (cbShow) {
                LVAdapter.ViewHolder holder = (LVAdapter.ViewHolder) view.getTag();
                // 改变CheckBox的状态
                holder.cb.toggle();
                // 将CheckBox的选中状况记录下来
                mAdapter.map.put(position, holder.cb.isChecked());
            } else {
                Toast.makeText(getActivity(), "正在解压缩文件，请稍后", Toast.LENGTH_LONG).show();
                thrd.start();
            }

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.wifi_direct_send_list_frag, null);
        return mContentView;
    }
}
