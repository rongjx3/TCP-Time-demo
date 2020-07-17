package jason.tcpdemo.funcs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class FuncTcpServer_2 extends Activity {
    private MyApp myapp;
    private Button btnCheckTime, btnNext2, btnPrev2;
    private TextView txtServerCorrect;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private TimeStampHelper tsh = new TimeStampHelper();
    private boolean getfromp1 = false, getfromp2 = false, correcting = false;
    private Object lock = new Object();

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
        private final WeakReference<FuncTcpServer_2> mActivity;
        MyHandler(FuncTcpServer_2 activity){
            mActivity = new WeakReference<FuncTcpServer_2>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FuncTcpServer_2 activity = mActivity.get();
            if (activity!= null){
                String mess;
                switch (msg.what){
                    case 3:
                        //txtSend.append(msg.obj.toString());
                        txtServerCorrect.setText("[标定误差]："+msg.obj.toString()+"ms");
                        break;
                    case 5:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("time:")) {
                                String mun = mess.substring(5, mess.length()-1);
                                long ot = Long.parseLong(mun);
                                //txtSend.append("[port1]时间戳："+mun+"\n");
                                tsh.setClientdate1(ot);
                                //txtTime1.setText("[port1]:"+ot);
                                getfromp1 = true;

                                if(getfromp2 && getfromp1 && correcting)
                                {
                                    correcting = false;
                                    long diff = tsh.calcul_client_diff();
                                    if(diff >= -10 && diff <= 10)
                                    {
                                        btnNext2.setEnabled(true);
                                    }
                                    Message messagediff = Message.obtain();
                                    messagediff.what = 3;
                                    messagediff.obj = String.valueOf(-diff);
                                    myHandler.sendMessage(messagediff);
                                    final String diff_s= String.valueOf(-diff);
                                    exec.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            myapp.tcpServer1.SST.get(0).send("cort:" + diff_s);
                                        }
                                    });
                                }
                            }
                        }
                        //txtRcv.append("[port1]"+msg.obj.toString());
                        break;
                    case 6:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("time:")) {
                                String mun = mess.substring(5, mess.length()-1);
                                long ot = Long.parseLong(mun);
                                //txtSend.append("[port2]时间戳："+mun+"\n");
                                tsh.setClientdate2(ot);
                                //txtTime2.setText("[port2]:"+ot);
                                getfromp2 = true;

                                if(getfromp2 && getfromp1 && correcting)
                                {
                                    correcting = false;
                                    long diff = tsh.calcul_client_diff();
                                    if(diff >= -10 && diff <= 10)
                                    {
                                        btnNext2.setEnabled(true);
                                    }
                                    Message messagediff = Message.obtain();
                                    messagediff.what = 3;
                                    messagediff.obj = String.valueOf(-diff);
                                    myHandler.sendMessage(messagediff);
                                    final String diff_s= String.valueOf(-diff);
                                    exec.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            myapp.tcpServer1.SST.get(0).send("cort:" + diff_s);
                                        }
                                    });
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
                    message1.what = 5;
                    message1.obj = msg1;
                    myHandler.sendMessage(message1);
                    break;
                case "tcpServerReceiver2":
                    String msg2 = intent.getStringExtra("tcpServerReceiver2");
                    Message message2 = Message.obtain();
                    message2.what = 6;
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

                case R.id.btn_tcpServerCheckTime:
                    getfromp1 = false;
                    getfromp2 = false;
                    correcting = true;
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myapp.tcpServer1.SST.get(0).send("Timecheck");
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myapp.tcpServer2.SST.get(0).send("Timecheck");
                        }
                    });
                    break;
                case R.id.btn_tcpServerNext2:
                    Intent intent = new Intent();
                    intent.setClass(FuncTcpServer_2.this,FuncTcpServer_3.class);
                    startActivity(intent);
                    break;
                case R.id.btn_tcpServerPrev2:
                    finish();
                    break;

            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG,"crash check1");
        setContentView(R.layout.tcp_server_2);
        context = this;
        myapp = (MyApp) this.getApplication();
        bindID();
        bindListener();
        bindReceiver();
        ini();
    }

    private void ini(){
        btnNext2.setEnabled(false);
    }

    private void bindListener() {
        btnCheckTime.setOnClickListener(myBtnClicker);
        btnNext2.setOnClickListener(myBtnClicker);
        btnPrev2.setOnClickListener(myBtnClicker);
    }

    private void bindID() {
        btnCheckTime = (Button) findViewById(R.id.btn_tcpServerCheckTime);
        btnNext2 = (Button) findViewById(R.id.btn_tcpServerNext2);
        btnPrev2 = (Button) findViewById(R.id.btn_tcpServerPrev2);
        txtServerCorrect = (TextView) findViewById(R.id.txt_timecorrect);

    }
}
