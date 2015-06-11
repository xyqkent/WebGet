package zzp.android.getweb.WiFiDirectClass;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.LVAdapter;
import zzp.android.getweb.OwnClass.ListData;
import zzp.android.getweb.OwnClass.PublicVariable;
import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.OwnClass.ZipUtils;
import zzp.android.getweb.R;
import zzp.android.getweb.SaveViewActivity;
import zzp.android.getweb.WebViewActivity;

/**
 * Created by Xiang on 2015/6/1.
 */
public class ReceiveListFragment extends ListFragment {
    View mContentView = null;
    ListView receiveList;
    LinearLayout putin, oc_mselect;
    TextView putin_text, oc_mselect_text;
    ArrayList<ListData> r_list = new ArrayList<ListData>();
    Context mContext;
    LVAdapter mAdapter;
    DBManger dbM = new DBManger();
    Boolean cbShow = false;
    public static Handler mHandler;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        receiveList = (ListView) mContentView.findViewById(android.R.id.list);
        putin = (LinearLayout) mContentView.findViewById(R.id.putin);
        oc_mselect = (LinearLayout) mContentView.findViewById(R.id.oc_mselect);
        putin_text = (TextView) mContentView.findViewById(R.id.putin_text);
        oc_mselect_text = (TextView) mContentView.findViewById(R.id.oc_mselect_text);

        receiveList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getActivity().onTouchEvent(event);
                return false;
            }
        });
        putin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.get_setCKFlg()) {
                    if (r_list.size() == 0) {
                        Toast.makeText(getActivity(), "未选中任何文件", Toast.LENGTH_SHORT).show();
                    } else
                        new AlertDialog.Builder(getActivity())
                                .setTitle("询问")
                                .setMessage("是否导入选中文件？")
                                .setPositiveButton("导入", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        putinDB(false);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                } else {
                    if (r_list.size() == 0) {
                        Toast.makeText(getActivity(), "无可导入文件", Toast.LENGTH_SHORT).show();
                    } else
                        new AlertDialog.Builder(getActivity())
                                .setTitle("询问")
                                .setMessage("是否导入全部文件？")
                                .setPositiveButton("导入", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        putinDB(true);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                }
            }
        });
        oc_mselect.setOnClickListener(o_mselect);

        mAdapter = new LVAdapter(mContext, r_list, false);

        receiveList.setAdapter(mAdapter);
        receiveList.setOnItemClickListener(receiveList_ItemClickListener);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //更新数据
                        UserFunction uf = new UserFunction();
                        Toast.makeText(getActivity(), "接收文件中\n请查看接收列表", Toast.LENGTH_LONG).show();
                        ListData tmpLD = (ListData) msg.obj;
                        //检查是否存在重复文件，若重复则不加入接收列表
                        Boolean checked = false;
                        for (ListData check : r_list)
                            if (check.getName().equals(tmpLD.getName()))
                                checked = true;
                        if (checked) break;
                        r_list.add(tmpLD);
                        mAdapter.notifyDataSetChanged();
                        break;

                }
            }
        };
    }

    private void putinDB(Boolean allFlag) {
        int[] removes = new int[r_list.size()];
        ArrayList<ListData> DBList = PublicVariable.getInstance().get_DBList();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = df.format(new Date());
        DBManger dbM = new DBManger();
        SQLiteDatabase db = dbM.getDB(getActivity());
        Cursor cur = db.rawQuery("select * from list order by id desc", null);
        int id = 0;
        if (cur.moveToFirst()) {
            id = cur.getInt(0) + 1;//获取id
        }
        cur.close();
        for (int i = r_list.size() - 1; i >= 0; i--) {
            if (mAdapter.map.get(i) || allFlag) {
                ListData each = r_list.get(i);
                Boolean checked = false;
                String autoName;
                for (ListData check : DBList) {
                    if (each.getName().equals(check.getName())) {
                        checked = true;
                        break;//可以break标签
                    }
                }
                if (checked) autoName = "(副本)" + each.getName();
                else autoName = each.getName();
                ContentValues cv = new ContentValues();
                cv.put("id", id);
                cv.put("name", autoName);
                cv.put("c_time", now);
                cv.put("g_id", "1");
                cv.put("link", each.getLink());
                db.insert("list", null, cv);
                id++;
                String movepath = dbM.getSavePath(getActivity()) + File.separator + autoName + ".zip";
                File receiveFile = new File(each.getPath());
                receiveFile.renameTo(new File(movepath));
                r_list.remove(i);//移除项目
                mAdapter.map.delete(i);//移除check记录

            }
        }
        db.close();

        mAdapter.notifyDataSetChanged();
        //通知SaveViewActivity刷新数据
        Message.obtain(SaveViewActivity.mHandler, 1).sendToTarget();
        Toast.makeText(getActivity(), "导入文件成功", Toast.LENGTH_LONG).show();
    }

    private View.OnClickListener o_mselect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            oc_mselect.setOnClickListener(c_mselect);
            oc_mselect_text.setText(R.string.receive_list_btn_c_mselect);
            putin_text.setText(R.string.receive_list_btn_putin_select);
            //刷新ListView
            for (int i = 0; i < r_list.size(); i++)
                mAdapter.map.put(i, true);
            cbShow = true;
            mAdapter.set_setCKFlg(true);
            mAdapter.notifyDataSetChanged();
        }
    };

    private View.OnClickListener c_mselect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            oc_mselect.setOnClickListener(o_mselect);
            oc_mselect_text.setText(R.string.receive_list_btn_o_mselect);
            putin_text.setText(R.string.receive_list_btn_putin_all);
            //刷新ListView
            for (int i = 0; i < r_list.size(); i++)
                mAdapter.map.put(i, true);
            cbShow = false;
            mAdapter.set_setCKFlg(false);
            mAdapter.notifyDataSetChanged();
        }
    };

    private AdapterView.OnItemClickListener receiveList_ItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (cbShow) {
                LVAdapter.ViewHolder holder = (LVAdapter.ViewHolder) view.getTag();
                // 改变CheckBox的状态
                holder.cb.toggle();
                // 将CheckBox的选中状况记录下来
                mAdapter.map.put(position, holder.cb.isChecked());
            } else {
                final int pis = position;
                final String savePath = dbM.getSavePath(getActivity());
                final UserFunction uf = new UserFunction();

                String path = r_list.get(position).getPath();
                final File file = new File(path);
                // 文件存在并可读
                if (file.exists() && file.canRead()) {
                    Thread thrd = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String viewPath = savePath + File.separator + "view";
                            File viewDir = new File(viewPath);
                            uf.delete(viewDir);
                            try {
                                ZipUtils.unzip(file.getAbsolutePath(), viewPath);
                            } catch (Exception e) {
                                Toast.makeText(mContext,"尝试打开文件失败\n" + e.toString(),Toast.LENGTH_LONG).show();
                                return;
                            }
                            File index = new File(savePath + File.separator + "view/index.html");
                            Intent intent = new Intent();
                            intent.setClass(mContext, WebViewActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("ListData", r_list.get(pis));
                            intent.putExtras(bundle);
                            intent.putExtra("SFFlag", false);
                            intent.setDataAndType(Uri.fromFile(index), "html");
                            startActivity(intent);
                        }
                    });

                    thrd.start();
                }else
                    Toast.makeText(getActivity(), "文件不存在，请检查文件有效性", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.wifi_direct_receive_list_frag, null);
        return mContentView;
    }
}
