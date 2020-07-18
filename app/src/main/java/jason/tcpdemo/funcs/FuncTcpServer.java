package jason.tcpdemo.funcs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jason.tcpdemo.MyApp;
import jason.tcpdemo.R;
import jason.tcpdemo.coms.AudioHelper;
import jason.tcpdemo.coms.TcpServer;

import static android.content.ContentValues.TAG;

/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class FuncTcpServer extends Activity {
    private MyApp myapp;
    private Button btnStartServer,btnNext1;
    private TextView txtConnStatus1, txtConnStatus2, txtServerIp;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private Object lock = new Object();
    private boolean isport1in = false, isport2in = false;

    private void stopThread(){
        synchronized (lock){
            try{
                lock.wait();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    ExecutorService exec = Executors.newCachedThreadPool();



    private class MyHandler extends android.os.Handler{
        private final WeakReference<FuncTcpServer> mActivity;
        MyHandler(FuncTcpServer activity){
            mActivity = new WeakReference<FuncTcpServer>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FuncTcpServer activity = mActivity.get();
            if (activity!= null){
                String mess;
                switch (msg.what){
                    case 1:

                        break;
                    case 2:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("[新的客户"))
                            {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        myapp.tcpServer1.SST.get(0).send("[已作为 炮位1 连入服务器]");
                                    }
                                });
                                String str="<font color='#00dd00'>已连接</font>";
                                txtConnStatus1.setText(Html.fromHtml(str));
                                isport1in = true;
                                if(isport1in && isport2in)
                                {
                                    btnNext1.setEnabled(true);
                                }
                            }
                        }
                        //txtRcv.append("[port1]"+msg.obj.toString());
                        break;
                    case 3:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("[新的客户"))
                            {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        myapp.tcpServer2.SST.get(0).send("[已作为 炮位2 连入服务器]");
                                    }
                                });
                                String str="<font color='#00dd00'>已连接</font>";
                                txtConnStatus2.setText(Html.fromHtml(str));
                                isport2in = true;
                                if(isport1in && isport2in)
                                {
                                    btnNext1.setEnabled(true);
                                }
                            }
                        }
                        //txtRcv.append("[port2]"+msg.obj.toString());
                        break;

                }
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();
            Log.i(TAG, "mAction:"+mAction);
            switch (mAction){
                case "tcpServerReceiver":
                    String msg = intent.getStringExtra("tcpServerReceiver");
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = msg;
                    myHandler.sendMessage(message);
                    break;
                case "tcpServerReceiver1":
                    String msg1 = intent.getStringExtra("tcpServerReceiver1");
                    Message message1 = Message.obtain();
                    message1.what = 2;
                    message1.obj = msg1;
                    myHandler.sendMessage(message1);
                    break;
                case "tcpServerReceiver2":
                    String msg2 = intent.getStringExtra("tcpServerReceiver2");
                    Message message2 = Message.obtain();
                    message2.what = 3;
                    message2.obj = msg2;
                    myHandler.sendMessage(message2);
                    break;
            }
        }
    }

    private void bindReceiver(){
        //IntentFilter intentFilter = new IntentFilter("tcpServerReceiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("tcpServerReceiver");
        intentFilter.addAction("tcpServerReceiver1");
        intentFilter.addAction("tcpServerReceiver2");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }

    private class MyBtnClicker implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btn_tcpServerConn:
                    Log.i("A", "onClick: 开始");
                    btnStartServer.setEnabled(false);
                    btnStartServer.setText("已开启服务器");
                    myapp.tcpServer1 = new TcpServer(getHost("1232"),"1");
                    exec.execute(myapp.tcpServer1);
                    myapp.tcpServer2 = new TcpServer(getHost("1233"),"2");
                    exec.execute(myapp.tcpServer2);
                    break;
                case R.id.btn_tcpServerNext1:
                    Intent intent = new Intent();
                    intent.setClass(FuncTcpServer.this,FuncTcpServer_2.class);
                    startActivity(intent);
                    break;

            }
        }
    }

    private int getHost(String msg){
        if (msg.equals("")){
            msg = "1233";
        }
        return Integer.parseInt(msg);
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG,"crash check1");
        setContentView(R.layout.tcp_server);
        context = this;
        myapp = (MyApp) this.getApplication();
        bindID();
        bindListener();
        bindReceiver();
        ini();
    }

    private void ini(){
        txtServerIp.setText(getHostIP());
        btnNext1.setEnabled(false);
    }

    private void bindListener() {
        btnStartServer.setOnClickListener(myBtnClicker);
        btnNext1.setOnClickListener(myBtnClicker);
    }

    private void bindID() {
        btnStartServer = (Button) findViewById(R.id.btn_tcpServerConn);
        btnNext1 = (Button) findViewById(R.id.btn_tcpServerNext1);
        txtConnStatus1 = (TextView) findViewById(R.id.txt_ServerConnStatus1);
        txtConnStatus2 = (TextView) findViewById(R.id.txt_ServerConnStatus2);
        txtServerIp = (TextView) findViewById(R.id.txt_Server_Ip);
    }

    /**
     * 获取ip地址
     * @return
     */
    public String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("FuncTcpServer", "SocketException");
            Toast.makeText(FuncTcpServer.this,"Socket错误", Toast.LENGTH_LONG).show();

            e.printStackTrace();
        }
        return hostIp;

    }

}
