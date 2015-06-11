package zzp.android.getweb.OwnClass;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zzp.android.getweb.SaveViewActivity;
import zzp.android.getweb.WebViewGroupActivity;
import zzp.android.getweb.WiFiDirectClass.DeviceDetailFragment;

/**
 * Created by yingqiang on 2015/5/21.
 */
public class UserFunction {
    private Context mContext;
    private List<ListData> selList;
    private Toast toast;

    public void setContextNListDate(Context context, List<ListData> list) {
        mContext = context;
        selList = list;
    }

    public void reName() {
        if (selList.size() != 1) {
            showTextToast("无法重命名多个选中或无选中的文件");
        } else {
            CheckTitle(selList.get(0).getName());
        }
    }

    public void reGroup() {
        String ids[] = new String[selList.size()];
        for (int i = 0; i < selList.size(); i++) {
            ids[i] = selList.get(i).getID();
        }
        Intent intent = new Intent();
        intent.setClass(mContext, WebViewGroupActivity.class);
        Bundle b=new Bundle();
        b.putStringArray("list_id", ids);
        intent.putExtras(b);
        mContext.startActivity(intent);
    }

    public void sendSelects(){
        Message.obtain(DeviceDetailFragment.mHandler, 1, selList).sendToTarget();
        Toast.makeText(mContext,"正在发送文件\n请到已发送列表查看状态",Toast.LENGTH_LONG).show();
    }

    public void shareFile() {
        final String share1;
        String share2 = "分享链接";
        final ArrayList<Uri> uris = new ArrayList<Uri>();
        //获得文件（总）大小
        Long size = new Long(0);
        for (int i = 0; i < selList.size(); i++) {
            File file = new File(selList.get(i).getPath());
            try {
                size += getFileSize(file);
            } catch (Exception e) {
            }
            Uri u = Uri.fromFile(file);
            uris.add(u);
        }
        if (selList.size() > 1) {
            share2 = share2 + "（无法分享多条链接）";
        } else if (selList.get(0).getLink() == null) {
            share2 = share2 + "（无分享链接）";
        }
        share1 = "分享文档（" + FormetFileSize(size) + "）";
        final String shareItem[] = new String[]{share1, share2};
        new AlertDialog.Builder(mContext)
                .setTitle("分享")
                .setItems(shareItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                boolean multiple = uris.size() > 1;
                                Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE : android.content.Intent.ACTION_SEND);
                                if (multiple) {
                                    intent.setType("*/*");
                                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                                } else {
                                    intent.setType("*/*");
                                    intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                                }
                                mContext.startActivity(Intent.createChooser(intent, "分享文档"));
                                break;
                            case 1:
                                String SendMsg = selList.get(0).getLink();
                                String SendTitle = selList.get(0).getName();
                                if (SendMsg == null) {
                                    new AlertDialog.Builder(mContext).setTitle("错误")
                                            .setMessage("数据库中无分享链接")
                                            .setPositiveButton("确定", null).show();
                                } else if (selList.size() > 1) {
                                    new AlertDialog.Builder(mContext).setTitle("错误")
                                            .setMessage("无法同时分享多条链接")
                                            .setPositiveButton("确定", null).show();
                                } else {
                                    Intent itSend = new Intent(Intent.ACTION_SEND);
                                    itSend.setType("text/plain");
                                    itSend.putExtra(Intent.EXTRA_SUBJECT, "分享链接");
                                    itSend.putExtra(Intent.EXTRA_TEXT, SendTitle+"\n"+SendMsg);
                                    itSend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(Intent.createChooser(itSend, "分享链接"));
                                }
                                break;
                        }
                    }
                })
                .show();
    }

    public void delFile() {
        String title = "确定要删除此文件吗？";
        final String paths[] = new String[selList.size()];
        final String ids[] = new String[selList.size()];
        for (int i = 0; i < selList.size(); i++) {
            paths[i] = selList.get(i).getPath();
            ids[i] = selList.get(i).getID();
        }
        if (selList.size() > 1) title = "确定删除这" + selList.size() + "个文件吗？";
        new AlertDialog.Builder(mContext).setTitle("注意!").setMessage(title)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String sqlstr = " where id in(";
                        for (int i = 0; i < selList.size(); i++) {
                            delete(new File(paths[i]));
                            sqlstr += ids[i] + ",";
                        }
                        sqlstr = sqlstr.substring(0, sqlstr.length() - 1) + ")";
                        DBManger dbM = new DBManger();
                        SQLiteDatabase db = dbM.getDB(mContext);
                        db.execSQL("delete from list " + sqlstr);
                        db.close();
                        showTextToast("删除完成");
                        Message msg1 = Message.obtain();
                        msg1.what = 1;
                        SaveViewActivity.mHandler.sendMessage(msg1);
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    private void CheckTitle(String title) {
        final EditText et = new EditText(mContext);
        et.setText(title);
        et.setSelection(title.length());
        new AlertDialog.Builder(mContext)
                .setTitle("请修改标题")
                .setView(et)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HideKeyboard(mContext, et);
                        CheckTile2(et.getText().toString());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void CheckTile2(String title) {
        //检查标题是否重复
        //写入数据库
        final String send_title = title;
        DBManger dbM = new DBManger();
        SQLiteDatabase db = dbM.getDB(mContext);
        Cursor cur = db.rawQuery("select * from list where name='" + title + "'", null);
        if (cur.getCount() != 0) {
            new AlertDialog.Builder(mContext).setTitle("警告")
                    .setMessage("发现重复标题，请问？")
                    .setCancelable(false)
                    .setPositiveButton("重新修改", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CheckTitle(send_title);
                        }
                    })
                    .setNegativeButton("放弃修改", null)
                    .show();
            cur.close();
            db.close();
            return;
        }
        cur.close();
        title = sqliteEscape(title);//删除特殊字符
        db.execSQL("update list set name='" + title + "' where id=" + selList.get(0).getID());
        String zipSave = dbM.getSavePath(mContext);
        String rezipSave = zipSave + File.separator + title + ".zip";
        zipSave += File.separator + selList.get(0).getName() + ".zip";
        File NameZip = new File(zipSave);
        File reNameZip = new File(rezipSave);
        NameZip.renameTo(reNameZip);//修改文件名
        selList.get(0).setName(title);//修改当前sellist的name数据
        selList.get(0).setPath(rezipSave);//修改当前sellist的path数据
        showTextToast("重命名成功");
        //更新SaveViewActivity
        Message msg1 = Message.obtain();
        msg1.what = 1;
        SaveViewActivity.mHandler.sendMessage(msg1);
        //修改文件名
        db.close();
    }

    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
        } else {
            toast.cancel();
            toast.setText(msg);
        }
        toast.show();
    }

    public void delete(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }
            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]);
            }
            file.delete();
        }
    }

    private static long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
        } else {
            file.createNewFile();
            Log.e("获取文件大小", "文件不存在!");
        }
        return size;
    }

    private static String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public String sqliteEscape(String keyWord) {
        keyWord = keyWord.replace("\\.", "");
        keyWord = keyWord.replace("\\|", "");
        keyWord = keyWord.replace(":", "");
        keyWord = keyWord.replace("\"", "");
        keyWord = keyWord.replace("*", "");
        keyWord = keyWord.replace("<", "");
        keyWord = keyWord.replace(">", "");
        keyWord = keyWord.replace("?", "");
        keyWord = keyWord.replace("/", "");
        keyWord = keyWord.replace("'", "");
        keyWord = keyWord.replace("%", "");
        keyWord = keyWord.replace("&", "");
        //删除回车、换行符、制表符
        Pattern p = Pattern.compile("\t|\r|\n");
        Matcher m = p.matcher(keyWord);
        keyWord = m.replaceAll("");
        return keyWord;
    }

    public void HideKeyboard(Context context,View v){
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    //dip转px和px转dip
    public int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
