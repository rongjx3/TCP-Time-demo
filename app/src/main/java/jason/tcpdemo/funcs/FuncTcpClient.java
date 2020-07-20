package jason.tcpdemo.funcs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import jason.tcpdemo.MyApp;
import jason.tcpdemo.R;
import jason.tcpdemo.coms.AudioHelper;
import jason.tcpdemo.coms.CrashHandler;
import jason.tcpdemo.coms.TcpClient;

import static android.content.ContentValues.TAG;


/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class FuncTcpClient extends Activity {
    private String TAG = "FuncTcpClient";
    @SuppressLint("StaticFieldLeak")
    public static Context context ;
    private MyApp myapp;
    private Button btnStartClient,btnNext1;
    private String port;
    private EditText editClientIp;
    private TextView txtConnStatus, txtName;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    ExecutorService exec = Executors.newCachedThreadPool();
    private SharedPreferences sp;
    private Thread AutoThread;
    private boolean isAudioRun = false;
    private boolean needPause = false;
    private boolean needStop = false;
    private Object lock = new Object();
    private long lastThresholdTimeStamp = System.currentTimeMillis();
    private long curThresholdTimeStamp = 0;
    private double voicelimit = 70;
    private double maxVolume = 0;
    private Bundle maxVolumeBundle = new Bundle();
    private boolean isConnecting = false, isConnected = false;
    private Bundle overLimitBundle = new Bundle();

    CrashHandler crashHandler = CrashHandler.getInstance();

    private void stopThread(){
        synchronized (lock){
            try{
                lock.wait();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private class MyBtnClicker implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btn_tcpClientConn:
                    Log.i(TAG, "onClick: 开始");
                    btnStartClient.setText("连接中...");
                    btnStartClient.setEnabled(false);

                    String rem_ip;
                    rem_ip = editClientIp.getText().toString();
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("IP", rem_ip);
                    editor.commit();

                    isConnecting = true;

                    myapp.tcpClient = new TcpClient(editClientIp.getText().toString(),getPort(port));
                    exec.execute(myapp.tcpClient);
                    break;
                case R.id.btn_tcpClientNext1:
                    Intent intent = new Intent();
                    intent.setClass(FuncTcpClient.this,FuncTcpClient_2.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    private class MyHandler extends android.os.Handler{
        private WeakReference<FuncTcpClient> mActivity;

        MyHandler(FuncTcpClient activity){
            mActivity = new WeakReference<FuncTcpClient>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity != null){
                String mess;
                switch (msg.what){
                    case 1:
                        mess = msg.obj.toString();
                        if(mess.length()>=4) {
                            String sta = mess.substring(0, 4);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("[已作为")) {
                                //连接成功
                                btnNext1.setEnabled(true);
                                btnStartClient.setText("已连接到计时员");
                                String str="<font color='#00dd00'>已连接</font>";
                                txtConnStatus.setText(Html.fromHtml(str));
                                isConnected = true;
                            }
                        }
                        break;
                }
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();
            switch (mAction){
                case "tcpClientReceiver":
                    String msg = intent.getStringExtra("tcpClientReceiver");
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = msg;
                    myHandler.sendMessage(message);
                    break;
                case "Error":
                    tipToast();
                    break;
            }
        }
    }


    private int getPort(String msg){
        if (msg.equals("")){
            msg = "1234";
        }
        return Integer.parseInt(msg);
    }

    public void tipToast() {
        Toast.makeText(FuncTcpClient.this, "网络出现异常，程序即将关闭", Toast.LENGTH_SHORT).show();
        android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
        System.exit(0);   //常规java、c#的标准退出法，返回值为0代表正常退出
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "Client ONCREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tcp_client);
        context = this;
        myapp = (MyApp) this.getApplication();
        sp = this.getSharedPreferences("IPInfo", Context.MODE_PRIVATE);
        crashHandler.initCrashHandler(myapp);
        Intent intent = getIntent();
        port = intent.getStringExtra("port");
        bindID();
        bindListener();
        bindReceiver();
        Ini();
    }



    private void bindID(){
        btnStartClient = (Button) findViewById(R.id.btn_tcpClientConn);
        btnNext1 = (Button) findViewById(R.id.btn_tcpClientNext1);
        editClientIp = (EditText) findViewById(R.id.edit_tcpClientIp);
        txtConnStatus = (TextView) findViewById(R.id.txt_ClientConnStatus);
        txtName = (TextView) findViewById(R.id.txt_ClientName);
    }
    private void bindListener(){
        btnStartClient.setOnClickListener(myBtnClicker);
        btnNext1.setOnClickListener(myBtnClicker);
    }
    private void bindReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("tcpClientReceiver");
        intentFilter.addAction("Error");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }
    private void Ini(){
        editClientIp.setText(sp.getString("IP", "192.168.1.100"));
        btnNext1.setEnabled(false);
        myapp.name = sp.getString("name", "炮位");
        txtName.setText(myapp.name);
        isConnecting = false;
        isConnected = false;
    }

    @Override
    protected void onResume(){
        super.onResume();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(12500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(!isConnecting) {
                    btnStartClient.setText("连接中...");
                    btnStartClient.setEnabled(false);

                    myapp.tcpClient = new TcpClient(editClientIp.getText().toString(), getPort(port));
                    exec.execute(myapp.tcpClient);

                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(isConnected) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                myapp.tcpClient.send("hatbe");
                            }
                        });

                        Intent intent = new Intent();
                        intent.setClass(FuncTcpClient.this, FuncTcpClient_2.class);
                        startActivity(intent);
                    }
                    else
                    {
                        Toast toast=Toast.makeText(FuncTcpClient.this, "心跳超时，程序重启！", Toast.LENGTH_SHORT);
                        toast.show();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
                        System.exit(0);   //常规java、c#的标准退出法，返回值为0代表正常退出
                    }
                }
            }
        };
        AutoThread = new Thread(runnable);
        AutoThread.start();
    }
}
