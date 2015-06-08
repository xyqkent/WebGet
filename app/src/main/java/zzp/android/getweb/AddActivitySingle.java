package zzp.android.getweb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.OwnClass.ZipUtils;

public class AddActivitySingle extends Activity {
    //初始化变量
    String webT, webTOR, WUrl, AppSavePath;
    String[] picar, picarR;
    int picN = 0, preProgress = 0;
    Boolean Downflag = true, OWFlag = false, MenuBtnFlag = false;
    Thread Trun = null;
    public static Handler mHandler;
    ProgressDialog myDialog = null;
    Toast toast = null;
    ProgressBar pbar;
    WebView webView;
    UserFunction uf = new UserFunction();
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_view);
        mContext = this;
        //设置sdcard路径，不存在则使用appcache目录
        SetSaveDir(this);
        //获取组件id
        final AutoCompleteTextView url = (AutoCompleteTextView) findViewById(R.id.url);
        final Button jumpMain = (Button) findViewById(R.id.jumpMain);
        final ProgressBar pload = (ProgressBar) findViewById(R.id.load);
        //禁止输入法在creat activity时弹出
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        url.setSelectAllOnFocus(true);
        pbar = (ProgressBar) findViewById(R.id.pb);
        webView = (WebView) findViewById(R.id.show);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.addJavascriptInterface(new JSHandler(), "handler");
        webView.setWebViewClient(new WebViewClient() {
            //网页中打开网页
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String strUrl) {
                //禁止某些网页跳转
                if (strUrl.contains("snssdk")) {
                    return false;
                }
                showTextToast("网页跳转中");
                view.loadUrl(strUrl);
                url.setFocusable(false);
                url.setText(Uri.decode(strUrl));
                url.setFocusableInTouchMode(true);
                return true;
            }

            //读取开始
            @Override
            public void onPageStarted(WebView view, String strUrl, Bitmap favicon) {
                pload.setVisibility(View.VISIBLE);
                pbar.setVisibility(View.VISIBLE);
                //刷新Menu按键
                MenuBtnFlag = false;
                invalidateOptionsMenu();
                uf.HideKeyboard(mContext, url);
                //利用setFocusable(false)，在setText应用之前让控件不能获得焦点，这样就可以不弹出输入法
                //然后在设置setFocusableInTouchMode(true)，让该控件可以点击
                url.setFocusable(false);
                url.setText(Uri.decode(strUrl));
                url.setFocusableInTouchMode(true);
                WUrl = Uri.decode(strUrl);
                super.onPageStarted(view, strUrl, favicon);
            }

            //读取完成
            @Override
            public void onPageFinished(WebView view, String strUrl) {
                WUrl = Uri.decode(strUrl);
                super.onPageFinished(view, strUrl);
            }
        });

        //定义progressbar的进度和获取网页标题
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                pbar.setProgress(newProgress);
                //通过控制100%之前的数据是否为0来判断是否正在正常加载，预防网页出现多次重复加载
                if (newProgress == 100) {
                    if (preProgress != 0) {
                        if (url.getText().length() == 0) {
                            MenuBtnFlag = false;
                        } else {
                            MenuBtnFlag = true;
                        }
                        invalidateOptionsMenu();
                        pload.setVisibility(View.GONE);
                        showTextToast("网页加载完成");
                    }
                    newProgress = 0;
                }
                preProgress = newProgress;
            }

            public void onReceivedTitle(WebView view, String title) {
                webT = uf.sqliteEscape(title);
                webTOR = webT;
            }
        });

        //url的文本框，当按回车之后自动补全http字符
        url.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent ev) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                    String strUrl = url.getText().toString();
                    if (strUrl.indexOf("file") != 0) {
                        Pattern p = Pattern.compile("http://([\\w-]+\\.)+[\\w-]+(/[\\w-\\./?%=]*)?");
                        Matcher m = p.matcher(strUrl);
                        if (!m.find()) {
                            strUrl = "http://" + strUrl;
                        }
                    }
                    webView.loadUrl(strUrl);
                    uf.HideKeyboard(mContext, v);
                    return true;
                }
                return false;
            }
        });
        //设置url失去焦点后隐藏键盘
        url.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) uf.HideKeyboard(mContext, v);
            }
        });

        //定义按钮事件
        jumpMain.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent jumpIntent = new Intent();
                jumpIntent.setClass(mContext, MainTabActivity.class);
                jumpIntent.putExtra("url", WUrl);
                startActivity(jumpIntent);
                finish();
            }
        });
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // 更新UI
                switch (msg.what) {
                    case 1:
                        //保存主体index文件
                        if (myDialog != null) {
                            myDialog.dismiss();
                        }
                        showTextToast("网页压缩打包中\n弹出确认窗口后则为完成");
                        webView.loadUrl("javascript:window.handler.SavePage('<head>'+" + "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                        break;
                    case 2:
                        //回调process进度的值
                        myDialog.setProgress(msg.arg1);
                        break;
                    case 3:
                        //暂未使用
                        break;
                    case 4:
                        //初始话下载进度
                        processThread();
                        // 执行图片下载
                        Trun = new Thread(saveFileRunnable);
                        Trun.start();
                        break;
                    case 5:
                        //获取图片地址并根据是否下载图片控制message what
                        for (int i = 0; i < picN; i++) {
                            webView.loadUrl("javascript:window.handler.getpic(document.getElementsByTagName('img')[" + i + "].src,'" + i + "');");
                            webView.loadUrl("javascript:window.handler.getpic(document.getElementsByTagName('img')[" + i + "].getAttribute('data-src'),'" + i + "');");
                        }
                        File tmpDir = new File(AppSavePath + File.separator + "tmp");
                        uf.delete(tmpDir);//清除上次tmp下载内容
                        new AlertDialog.Builder(mContext).setTitle("询问")
                                .setMessage("是否下载网页内的图片？\n共" + String.valueOf(picN) + "张\n注：建议使用wifi下载")
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {    //下载标记，用于下载html是否替换img路径
                                        Downflag = true;
                                        // 执行下载进度条
                                        Message.obtain(mHandler, 4).sendToTarget();
                                    }
                                }).setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Downflag = false;
                                Message.obtain(mHandler, 1).sendToTarget();
                            }
                        }).show();
                        break;
                    case 6:
                        //传递message控制保存
                        CheckTitle(false, webTOR);
                        break;
                    case 7:
                        //传递message控制刷新
                        webView.reload();
                        break;
                }
            }
        };
        //接受传入数据
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();
        String tmpUrl = "";
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("text/")) {
                //利用正则表达式获取传入字符串中的网址
                Matcher m = Pattern.compile("[a-zA-z]+://[^\\s]*").matcher(intent.getStringExtra(Intent.EXTRA_TEXT));
                if (m.find()) {
                    tmpUrl = m.group();
                } else tmpUrl = "无获取到链接";
            }
        } else {
            if (uri != null) {
                tmpUrl = uri.toString();
            }
        }
        webView.loadUrl(tmpUrl);
    }

    //加入menu菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_tab_menu, menu);
        MenuItem menu_add_save = menu.findItem(R.id.menu_add_save);
        MenuItem menu_add_refresh = menu.findItem(R.id.menu_add_refresh);
        menu_add_save.setVisible(true);
        menu_add_refresh.setVisible(true);
        if (MenuBtnFlag) {
            menu_add_refresh.setEnabled(true);
            menu_add_save.setEnabled(true);
        } else {
            menu_add_refresh.setEnabled(false);
            menu_add_save.setEnabled(false);
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        /*
         * 将actionBar的HomeButtonEnabled设为ture， 将会执行此case
         */
            case R.id.menu_add_save:
                CheckTitle(false, webTOR);
                break;
            case R.id.menu_add_refresh:
                webView.reload();
                break;
            case R.id.menu_app_exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private Runnable saveFileRunnable = new Runnable() {
        @Override
        public void run() {
            Bitmap bitmap;
            picarR = new String[picN];
            for (int i = 0; i < picN; i++) {
                if (picar[i] != null && myDialog.isShowing()) {
                    try {
                        //每下载完一张图就增加进度
                        Message.obtain(mHandler, 2, i + 1).sendToTarget();
                        //下载图片
                        bitmap = BitmapFactory.decodeStream(getImageStream(picar[i]));
                        if (bitmap != null)
                            try {
                                String a[] = picar[i].split("\\.");
                                int max = a.length - 1;
                                // 定义普通图片格式
                                if (a[max].equals("jpg") || a[max].equals("jpeg") || a[max].equals("png") || a[max].equals("gif") || a[max].equals("bmp")) {
                                    String b[] = picar[i].split("/");
                                    picarR[i] = b[b.length - 1];
                                    // 微信专用的/0图片
                                } else {
                                    String c[] = picar[i].split("/");
                                    picarR[i] = c[c.length - 2] + ".jpg";
                                }
                                //保存图片文件
                                //Log.i(TAG, "save " + picar[i]);
                                saveFile(bitmap, picarR[i]);
                            } catch (IOException e) {
//								showTextToast(e.toString()+"1 error");
                            }
                    } catch (Exception e1) {
//						showTextToast(e1.toString()+"2 error");
                    }
                }
            }
            //跳转至Handler 1处获取网页源代码
            Message.obtain(mHandler, 1).sendToTarget();
        }
    };

    //保存图片到本地
    public void saveFile(Bitmap bm, String fileName) throws IOException {
        // 设置图片保存的文件夹
        String SavePic_PATH = AppSavePath + File.separator + "tmp" + File.separator + "pic";
        File dirFile = new File(SavePic_PATH);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        File myCaptureFile = new File(SavePic_PATH + File.separator + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
    }

    //网络下载图片
    public InputStream getImageStream(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return conn.getInputStream();
        }
        return null;
    }

    // 构建一个下载进度条
    @SuppressWarnings("deprecation")
    private void processThread() {
        // 创建ProgressDialog对象  
        myDialog = new ProgressDialog(mContext);
        // 设置进度条风格，风格为长形  
        myDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // 设置ProgressDialog 标题  
        myDialog.setTitle("提示");
        // 设置ProgressDialog 提示信息  
        myDialog.setMessage("正在下载图片......");
        // 设置ProgressDialog 进度条最大值（获取图片值）  
        myDialog.setMax(picN);
        // 设置ProgressDialog 的进度条是否不明确  
        myDialog.setIndeterminate(false);
        myDialog.setButton("跳过下载", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                myDialog.dismiss();
            }
        });
        // 设置ProgressDialog 是否可以按退回按键取消
        myDialog.setCancelable(false);
        //设置点击进度对话框外的区域对话框不消失
        myDialog.setCanceledOnTouchOutside(false);
        // 让ProgressDialog显示  
        myDialog.show();
    }

    private void SetSaveDir(Context context) {
        DBManger dbM = new DBManger();
        AppSavePath = dbM.getSavePath(context);
    }

    //Toast的重写，以便重复出现时不重叠
    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }


    private void RenameTitle(String saveTitle) {
        final EditText et = new EditText(mContext);
        et.setText(saveTitle);
        et.setSelection(saveTitle.length());
        new AlertDialog.Builder(mContext)
                .setTitle("请修改标题")
                .setView(et)
                .setCancelable(false)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        webT = uf.sqliteEscape(et.getText().toString());
                        uf.HideKeyboard(mContext, et);
                        CheckTitle(true, webT);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // reNameFlag判断是否从renameTitle过来的
    private void CheckTitle(final Boolean reNameFlag, final String saveTitle) {
        //检查标题是否重复
        DBManger dbM = new DBManger();
        SQLiteDatabase db = dbM.getDB(this);
        OWFlag = false;
        Cursor cur = db.rawQuery("select * from list where name='" + saveTitle + "'", null);
        if (cur.getCount() != 0) {
            new AlertDialog.Builder(mContext).setTitle("警告")
                    .setMessage("发现已存在标题重复文件，是否覆盖？")
                    .setCancelable(false)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OWFlag = true;
                            webView.loadUrl("javascript:window.handler.getpicN(document.getElementsByTagName('img').length);");
                        }
                    })
                    .setNegativeButton("修改", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RenameTitle(saveTitle);
                        }
                    })
                    .setNeutralButton("取消", null)
                    .show();
            return;
        }
        cur.close();
        db.close();
        if (!reNameFlag) RenameTitle(saveTitle);
        else
            webView.loadUrl("javascript:window.handler.getpicN(document.getElementsByTagName('img').length);");
    }

    private void AddtoDB() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = df.format(new Date());
        DBManger dbM = new DBManger();
        SQLiteDatabase db = dbM.getDB(this);
        Cursor cur = db.rawQuery("select * from list order by id desc", null);
        int id = 0;
        if (cur.moveToFirst()) {
            id = cur.getInt(0) + 1;//获取id
        }
        cur.close();
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", webT);
        cv.put("c_time", now);
        cv.put("g_id", "1");
        cv.put("link", WUrl);
        db.insert("list", null, cv);
        db.close();
        //通知SaveViewActivity刷新数据
        Message.obtain(SaveViewActivity.mHandler, 1).sendToTarget();
        toast.cancel();
    }

    private boolean ZipDownFile() {
        String zipSave = AppSavePath + File.separator + webT + ".zip";
        String savePath = AppSavePath + File.separator + "tmp";
        //zip压缩
        try {
            ZipUtils.zip(savePath, zipSave);
        } catch (Exception e) {
            new AlertDialog.Builder(mContext).setTitle("压缩失败")
                    .setMessage("压缩过程出现问题，请重新保存\n" + e.toString())
                    .setPositiveButton("确定", null).show();
            return false;
        }
        File dirFile = new File(savePath);
        uf.delete(dirFile);//清除此次下载内容
        new AlertDialog.Builder(mContext).setTitle("保存成功")
                .setMessage("文件保存如下位置：\n/内置储存/SaveThisPage/Save/" + webT)
                .setPositiveButton("确定", null).show();
        return true;
    }


    //为js调用的函数
    class JSHandler {
        @JavascriptInterface
        public void SavePage(String data) {
            try {
                String savePath = AppSavePath + File.separator + "tmp";
                int start = 0;
                int end = 0;
                int s_meta = 0;
                File dirFile = new File(savePath);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                String getCS = "";
                FileOutputStream fos = new FileOutputStream(savePath + File.separator + "index.html");
                //查找网页中charset的编码
                s_meta = data.indexOf("<meta");
                if (data.indexOf("charset=") > 0) {
                    start = data.indexOf(" charset=", s_meta) + 9;
                    end = data.indexOf("\"", start + 1);
                    getCS = data.substring(start, end);
                }
                //清除charset中的双引号
                getCS = getCS.replace("\"", "");
                //替换图片文件到本地文件
                if (Downflag) {
                    for (int i = 0; i < picN; i++) {
                        if (picar[i] != null && picarR[i] != null) {
                            data = data.replace(picar[i], "pic/" + picarR[i]);
                        }
                    }
                }
                if (data.indexOf("data-src") > 0) {
                    //强制网页显示图片（跳过需拖动才加载的js）
                    data = data.replace("!important", "");
                    data = data.replace("data-src", "src");
                    String Css = "<style type=\"text/css\">img{width:auto!important;height:auto!important;visibility: visible!important;}</style></body>";
                    data = data.replace("</body>", Css);
                }
                //根据getCS的编码写入文件
                fos.write(data.getBytes(getCS));
                fos.close();
                //先压缩文件，然后再写入数据库
                if (!ZipDownFile()) {
                    return;
                }
                if (!OWFlag) AddtoDB();
            } catch (Exception e) {
                new AlertDialog.Builder(mContext).setTitle("错误")
                        .setMessage("保存失败\n请重新下载\n" + e.getCause().toString())
                        .setPositiveButton("确定", null).show();
            }

        }

        //获取图片地址
        @JavascriptInterface
        public void getpic(String pic, String n) {
            if (pic != null) {
                picar[Integer.valueOf(n)] = pic;
            }
        }

        //获取图片数量
        @JavascriptInterface
        public void getpicN(int num) {
            picar = new String[num];
            picN = num;
            //获取网页中img的图片绝对
            Message.obtain(mHandler, 5).sendToTarget();
        }

    }

    private long exitTime;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) // System.currentTimeMillis()无论何时调用，肯定大于2000
            {
                showTextToast("再按一次退出程序");
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}