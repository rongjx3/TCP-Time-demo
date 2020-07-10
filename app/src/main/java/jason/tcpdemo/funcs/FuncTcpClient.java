package jason.tcpdemo.funcs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import jason.tcpdemo.R;
import jason.tcpdemo.coms.AudioHelper;
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
    private Button btnStartClient,btnCloseClient, btnCleanClientSend, btnCleanClientRcv,btnClientSend,btnClientRandom;
    private Button btnGetTime, btnSendTime,btnTimeCorrect;
    private TextView txtRcv,txtSend,txtTime;
    private EditText editClientSend,editClientID, editClientPort,editClientIp,editTimeCorrect;
    private Button btnControlAudio;
    private TextView txtVolume;
    private AudioHelper audioHelper = new AudioHelper();
    private static TcpClient tcpClient = null;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    ExecutorService exec = Executors.newCachedThreadPool();
    private TimeStampHelper tsh = new TimeStampHelper();
    private AudioHandler audioHandler = new AudioHandler(this);
    private boolean isAudioRun = false;
    private boolean needPause = false;
    private boolean needStop = false;
    private boolean isfirstListen = true;
    private Object lock = new Object();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (true){
                if(needStop){
                    stopThread();
                    needStop = false;
                }
                if(needPause){
                    try{
                        Thread.sleep(1500);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    needPause = false;
                }
                if(isAudioRun){
                    Message msg = audioHandler.obtainMessage();
                    msg.obj = audioHelper.getVolume();
                    audioHandler.sendMessage(msg);
                }
                Log.d(TAG, "run: "+Thread.currentThread().getId());
            }
        }
    };
    private Thread audioListenThread = new Thread(runnable);

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
                    btnStartClient.setEnabled(false);
                    btnCloseClient.setEnabled(true);
                    btnClientSend.setEnabled(true);
                    tcpClient = new TcpClient(editClientIp.getText().toString(),getPort(editClientPort.getText().toString()));
                    exec.execute(tcpClient);
                    break;
                case R.id.btn_tcpClientClose:
                    tcpClient.closeSelf();
                    btnStartClient.setEnabled(true);
                    btnCloseClient.setEnabled(false);
                    btnClientSend.setEnabled(false);
                    break;
                case R.id.btn_tcpCleanClientRecv:
                    txtRcv.setText("");
                    break;
                case R.id.btn_tcpCleanClientSend:
                    txtSend.setText("");
                    break;
                case R.id.btn_tcpClientRandomID:
                    editClientID.setText("Client"+editClientPort.getText().toString());
                    break;
                case R.id.btn_tcpClientSend:
                    Message message = Message.obtain();
                    message.what = 2;
                    message.obj = editClientSend.getText().toString();
                    myHandler.sendMessage(message);
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpClient.send(editClientID.getText().toString() + ":" + editClientSend.getText().toString());
                        }
                    });
                    break;

                case R.id.btn_tcpClientGetTime:
                    long date = tsh.getMydate_local();
                    Log.i(TAG, "timestamp: "+date);
                    txtTime.setText(String.valueOf(date));
                    Message messagetime1 = Message.obtain();
                    messagetime1.what = 3;
                    messagetime1.obj = String.valueOf(date);
                    myHandler.sendMessage(messagetime1);
                    break;

                case R.id.btn_tcpClientSendTime:
                    long dates = tsh.getMydate();
                    Message messagetime = Message.obtain();
                    messagetime.what = 4;
                    messagetime.obj = String.valueOf(dates);
                    myHandler.sendMessage(messagetime);
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpClient.send("time:"+txtTime.getText().toString());
                        }
                    });
                    break;

                case R.id.btn_tcpClientTimecorrect:
                    String cor = editTimeCorrect.getText().toString();
                    long corr = Long.parseLong(cor);
                    tsh.setCorrect(corr);
                    break;
                case R.id.btn_tcpClientControlAudio:
                    //第一次按监听键
                    if(!isAudioRun && isfirstListen){
                        audioHelper.startRecord();
                        audioListenThread.start();
                        isfirstListen = false;
                    }else if(!isAudioRun && !isfirstListen){
                        //第二次
                        audioHelper.startRecord();
                        synchronized (lock){
                            lock.notify();
                        }
                    } else {
                        //再按开始
                        audioHelper.stopRecord();
                        needStop = true;
                    }
                    isAudioRun = !isAudioRun;
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
                        String t = mess.substring(0, mess.length()-1);
                        if (t.equals("Timecheck")) {
                            long date = tsh.getMydate_local();
                            Log.i(TAG, "timestamp: "+date);
                            txtTime.setText(String.valueOf(date));
                            txtSend.append("时间戳矫正中:"+date+"\n");
                            exec.execute(new Runnable() {
                                @Override
                                public void run() {
                                    tcpClient.send("time:"+txtTime.getText().toString());
                                }
                            });
                        }else
                        {
                            if(mess.length()>=5) {
                                String sta = mess.substring(0, 5);
                                Log.i(TAG, "substring : " + sta);
                                if (sta.equals("time:")) {
                                    String result = mess.substring(5, mess.length()-1);
                                    txtSend.append("差值结果："+result+"\n");
                                } else {
                                    txtRcv.append("服务器端:"+msg.obj.toString());
                                }
                            }
                            else
                            {
                                txtRcv.append("服务器端:"+msg.obj.toString());
                            }
                        }
                        break;
                    case 2:
                        //txtSend.append(msg.obj.toString());
                        txtRcv.append(editClientID.getText().toString()+"（你）:"+msg.obj.toString()+"\n");
                        break;
                    case 3:
                        //txtSend.append(msg.obj.toString());
                        txtSend.append("获取时间戳:"+msg.obj.toString()+"\n");
                        break;
                    case 4:
                        txtSend.append("发送时间戳:"+msg.obj.toString()+"\n");
                        break;
                }
            }
        }
    }

    private class AudioHandler extends android.os.Handler{
        private WeakReference<FuncTcpClient> mActivity;
        AudioHandler(FuncTcpClient activity){
            mActivity = new WeakReference<FuncTcpClient>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if(mActivity != null){
                double db = (double) msg.obj;
                txtVolume.setText(String.valueOf(db));
                if(db > 70){
                    needPause = true;
                    Message msg1 = myHandler.obtainMessage();
                    long dates = tsh.getMydate_local();
                    Log.d(TAG, "handleMessage: "+tsh.getMydate());
                    msg1.what = 3;
                    msg1.obj = String.valueOf(dates);
                    myHandler.sendMessage(msg1);
                    txtTime.setText(String.valueOf(dates));
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
            }
        }
    }


    private int getPort(String msg){
        if (msg.equals("")){
            msg = "1234";
        }
        return Integer.parseInt(msg);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tcp_client);
        context = this;
        bindID();
        bindListener();
        bindReceiver();
        Ini();
    }



    private void bindID(){
        btnStartClient = (Button) findViewById(R.id.btn_tcpClientConn);
        btnCloseClient = (Button) findViewById(R.id.btn_tcpClientClose);
        btnCleanClientRcv = (Button) findViewById(R.id.btn_tcpCleanClientRecv);
        btnCleanClientSend = (Button) findViewById(R.id.btn_tcpCleanClientSend);
        btnClientRandom = (Button) findViewById(R.id.btn_tcpClientRandomID);
        btnClientSend = (Button) findViewById(R.id.btn_tcpClientSend);
        btnGetTime = (Button) findViewById(R.id.btn_tcpClientGetTime);
        btnSendTime = (Button) findViewById(R.id.btn_tcpClientSendTime);
        btnTimeCorrect = (Button) findViewById(R.id.btn_tcpClientTimecorrect);
        editClientPort = (EditText) findViewById(R.id.edit_tcpClientPort);
        editClientIp = (EditText) findViewById(R.id.edit_tcpClientIp);
        editClientSend = (EditText) findViewById(R.id.edit_tcpClientSend);
        editClientID = (EditText) findViewById(R.id.edit_tcpClientID);
        editTimeCorrect = (EditText) findViewById(R.id.edit_Client_Timecorrect);
        txtRcv = (TextView) findViewById(R.id.txt_ClientRcv);
        txtSend = (TextView) findViewById(R.id.txt_ClientSend);
        txtTime = (TextView) findViewById(R.id.txt_clienttimestamp);
        btnControlAudio = (Button)findViewById(R.id.btn_tcpClientControlAudio);
        txtVolume = (TextView) findViewById(R.id.txt_tcpClientVolumeShow);
    }
    private void bindListener(){
        btnStartClient.setOnClickListener(myBtnClicker);
        btnCloseClient.setOnClickListener(myBtnClicker);
        btnCleanClientRcv.setOnClickListener(myBtnClicker);
        btnCleanClientSend.setOnClickListener(myBtnClicker);
        btnClientRandom.setOnClickListener(myBtnClicker);
        btnClientSend.setOnClickListener(myBtnClicker);
        btnGetTime.setOnClickListener(myBtnClicker);
        btnSendTime.setOnClickListener(myBtnClicker);
        btnTimeCorrect.setOnClickListener(myBtnClicker);
        btnControlAudio.setOnClickListener(myBtnClicker);
    }
    private void bindReceiver(){
        IntentFilter intentFilter = new IntentFilter("tcpClientReceiver");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }
    private void Ini(){
        btnCloseClient.setEnabled(false);
        btnClientSend.setEnabled(false);

    }
}
