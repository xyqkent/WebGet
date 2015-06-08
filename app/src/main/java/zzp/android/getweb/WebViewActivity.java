package zzp.android.getweb;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import zzp.android.getweb.OwnClass.ListData;
import zzp.android.getweb.OwnClass.UserFunction;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends Activity {
    //初始化变量
    Toast toast = null;
    WebView webView;
    ListData ld;
    RelativeLayout share, rename, setgroup, sendfiles;
    UserFunction uf = new UserFunction();
    Boolean SFFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        //禁止输入法在creat activity时弹出
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        webView = (WebView) findViewById(R.id.show2);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WebViewClient() {
            //网页中打开网页
            public boolean shouldOverrideUrlLoading(WebView view, String strUrl) {
                //禁止某些网页跳转
                if (strUrl.contains("snssdk")) {
                    return false;
                }
                showTextToast("网页跳转中");
                view.loadUrl(strUrl);
                return false;
            }
        });
        share = (RelativeLayout) findViewById(R.id.share);
        rename = (RelativeLayout) findViewById(R.id.rename);
        setgroup = (RelativeLayout) findViewById(R.id.setgroup);
        sendfiles = (RelativeLayout) findViewById(R.id.sendfiles);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uf.shareFile();
            }
        });
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uf.reName();
            }
        });
        setgroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uf.reGroup();
            }
        });
        sendfiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uf.sendSelects();
            }
        });

        //获取上一个intent读取的数据
        List<ListData> mAList = new ArrayList<ListData>();
        Intent intent = getIntent();
        Uri uri = intent.getData();
        ld = (ListData) intent.getSerializableExtra("ListData");
        //显示发送按钮
        SFFlag = intent.getBooleanExtra("SFFlag", false);
        if (SFFlag) sendfiles.setVisibility(View.VISIBLE);

        mAList.add(ld);
        if (uri != null) {
            webView.loadUrl(uri.toString());
        }
        uf.setContextNListDate(this, mAList);
    }

    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }
}