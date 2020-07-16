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
    private Button btnStartClient,btnCleanClientSend, btnCleanClientRcv,btnClientSend,btnClientVoice;
    private Button btnGetTime, btnSendTime, btnTimeCorrect, btnLock;
    private TextView txtRcv,txtSend,txtTime;
    private EditText editClientSend,editClientVoice, editClientPort,editClientIp,editTimeCorrect;
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
    private boolean isLock = false;
    private boolean isfirstListen = true;
    private Object lock = new Object();
    private long lastThresholdTimeStamp = System.currentTimeMillis();
    private long curThresholdTimeStamp = 0;
    private double voicelimit = 70;
    private double maxVolume = 0;
    private Bundle maxVolumeBundle = new Bundle();
    private boolean isCaptureVolume = false;
    private Bundle overLimitBundle = new Bundle();
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
                        audioHelper.stopRecord();
                        Thread.sleep(2000);
                        audioHelper.startRecord();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    needPause = false;
                }
                if(isAudioRun){
                    Message msg = audioHandler.obtainMessage();
                    double v = audioHelper.getVolume();
                    long dates = tsh.getMydate_local();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("volume", v);
                    bundle.putLong("timeStamp", dates);
                    msg.setData(bundle);
                    if(v > maxVolume){
                        maxVolume = v;
                        maxVolumeBundle.putDouble("maxVolume", maxVolume);
                        maxVolumeBundle.putLong("timeStamp", dates);
                        if(isCaptureVolume){
                            maxVolumeBundle.putDouble("overVolume", overLimitBundle.getDouble("overVolume"));
                            maxVolumeBundle.putLong("overTimeStamp", overLimitBundle.getLong("overTimeStamp"));
                        }else {
                            maxVolumeBundle.putDouble("overVolume", 0);
                            maxVolumeBundle.putLong("overTimeStamp", 0);
                        }
                    }
                    if(v > voicelimit && !isCaptureVolume){
                        curThresholdTimeStamp = System.currentTimeMillis();
                        if(curThresholdTimeStamp - lastThresholdTimeStamp > 2000){
                            overLimitBundle.putDouble("overVolume", bundle.getDouble("volume"));
                            overLimitBundle.putLong("overTimeStamp", bundle.getLong("timeStamp"));
                            maxVolumeBundle.putDouble("overVolume", overLimitBundle.getDouble("overVolume"));
                            maxVolumeBundle.putLong("overTimeStamp", overLimitBundle.getLong("overTimeStamp"));
                            audioHandler.sendMessage(msg);
                        }
                        lastThresholdTimeStamp = curThresholdTimeStamp;
                        isCaptureVolume = true;
                    }else if(v < voicelimit){
                        audioHandler.sendMessage(msg);

                    }

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
                    btnLock.setEnabled(true);
                    btnClientSend.setEnabled(true);
                    tcpClient = new TcpClient(editClientIp.getText().toString(),getPort(editClientPort.getText().toString()));
                    exec.execute(tcpClient);
                    break;
                case R.id.btn_tcpCleanClientRecv:
                    txtRcv.setText("");
                    break;
                case R.id.btn_tcpCleanClientSend:
                    txtSend.setText("");
                    break;
                case R.id.btn_tcpClientVoiceLimit:
                    String voi = editClientVoice.getText().toString();
                    double voic = Double.parseDouble(voi);
                    voicelimit = voic;
                    break;
                case R.id.btn_tcpClientSend:
                    Message message = Message.obtain();
                    message.what = 2;
                    message.obj = editClientSend.getText().toString();
                    myHandler.sendMessage(message);
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpClient.send(":" + editClientSend.getText().toString());
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
                    String dates = txtTime.getText().toString();
                    Message messagetime = Message.obtain();
                    messagetime.what = 4;
                    messagetime.obj = dates;
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
                        maxVolume = 0;
                        isCaptureVolume = false;
                        btnControlAudio.setText("监听声音:ON");
                        audioHelper.startRecord();
                        audioListenThread.start();
                        isfirstListen = false;
                    }else if(!isAudioRun && !isfirstListen){
                        //第二次
                        isCaptureVolume = false;
                        maxVolume = 0;
                        btnControlAudio.setText("监听声音:ON");
                        audioHelper.startRecord();
                        synchronized (lock){
                            lock.notify();
                        }
                    } else {
                        //再按开始

                        btnControlAudio.setText("监听声音:OFF");
                        audioHelper.stopRecord();
                        needStop = true;
                        double tempV = maxVolumeBundle.getDouble("maxVolume");
                        long tempT = maxVolumeBundle.getLong("timeStamp");
                        Message messagevoi = Message.obtain();
                        messagevoi.what = 5;
                        messagevoi.obj = "最大音量："+tempV+"\n"+"时间戳："+tempT+"\n";
                        myHandler.sendMessage(messagevoi);
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                tcpClient.send("maxV:" + maxVolumeBundle.getDouble("maxVolume")
                                        + "时间戳：" + maxVolumeBundle.getLong("timeStamp") + "\n");
                            }
                        });
                    }
                    isAudioRun = !isAudioRun;
                    break;
                case R.id.btn_tcpClientLock:
                    if(isLock)
                    {
                        isLock = false;
                        btnLock.setText("锁定模式:OFF");
                        btnCleanClientRcv.setEnabled(true);
                        btnCleanClientSend.setEnabled(true);
                        btnClientVoice.setEnabled(true);
                        btnClientSend.setEnabled(true);
                        btnGetTime.setEnabled(true);
                        btnSendTime.setEnabled(true);
                        btnTimeCorrect.setEnabled(true);
                        btnControlAudio.setEnabled(true);
                        editClientVoice.setFocusable(true);
                        editTimeCorrect.setFocusable(true);
                        editClientIp.setFocusable(true);
                        editClientPort.setFocusable(true);
                        editClientSend.setFocusable(true);
                    }
                    else
                    {
                        isLock = true;
                        btnLock.setText("锁定模式:ON");
                        btnCleanClientRcv.setEnabled(false);
                        btnCleanClientSend.setEnabled(false);
                        btnClientVoice.setEnabled(false);
                        btnClientSend.setEnabled(false);
                        btnGetTime.setEnabled(false);
                        btnSendTime.setEnabled(false);
                        btnTimeCorrect.setEnabled(false);
                        btnControlAudio.setEnabled(false);
                        editClientVoice.setFocusable(false);
                        editTimeCorrect.setFocusable(false);
                        editClientIp.setFocusable(false);
                        editClientPort.setFocusable(false);
                        editClientSend.setFocusable(false);
                    }
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
                        }
                        else if (t.equals("ListenTime")) {
                            txtSend.append("远程监听请求\n");
                            //第一次按监听键
                            if(!isAudioRun && isfirstListen){
                                maxVolume = 0;
                                isCaptureVolume = false;
                                btnControlAudio.setText("监听声音:ON");
                                audioHelper.startRecord();
                                audioListenThread.start();
                                isfirstListen = false;
                            }else if(!isAudioRun && !isfirstListen){
                                //第二次
                                maxVolume = 0;
                                isCaptureVolume = false;
                                btnControlAudio.setText("监听声音:ON");
                                audioHelper.startRecord();
                                synchronized (lock){
                                    lock.notify();
                                }
                            } else {
                                //再按开始
                                btnControlAudio.setText("监听声音:OFF");
                                audioHelper.stopRecord();
                                needStop = true;

                                double tempV = maxVolumeBundle.getDouble("maxVolume");
                                long tempT = maxVolumeBundle.getLong("timeStamp");
                                Message messagevoi = Message.obtain();
                                messagevoi.what = 5;
                                messagevoi.obj = "最大音量："+tempV+"\n"+"时间戳："+tempT+"\n";
                                myHandler.sendMessage(messagevoi);
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        tcpClient.send("maxV:" + maxVolumeBundle.getDouble("maxVolume")
                                                + "maxTimeStamp:" + maxVolumeBundle.getLong("timeStamp")
                                                + "overTimeStamp:" + maxVolumeBundle.getLong("overTimeStamp") + "\n");
                                    }
                                });
                            }
                            isAudioRun = !isAudioRun;
                        }
                        else if (t.equals("AskTime")) {
                            txtSend.append("获取时间戳请求\n");
                            String dates = txtTime.getText().toString();
                            txtSend.append("发送时间戳:"+dates+"\n");
                            exec.execute(new Runnable() {
                                @Override
                                public void run() {
                                    tcpClient.send("time:"+txtTime.getText().toString());
                                }
                            });
                        }
                        else if (t.equals("conTest")) {
                            txtRcv.append("[服务器端]:连接测试\n");
                            exec.execute(new Runnable() {
                                @Override
                                public void run() {
                                    tcpClient.send("连接正常");
                                }
                            });
                        }
                        else
                        {
                            if(mess.length()>=5) {
                                String sta = mess.substring(0, 5);
                                Log.i(TAG, "substring : " + sta);
                                if (sta.equals("time:")) {
                                    String result = mess.substring(5, mess.length()-1);
                                    txtSend.append("差值结果："+result+"\n");
                                }
                                else if(sta.equals("cort:"))
                                {
                                    String add = mess.substring(5, mess.length()-1);
                                    txtSend.append("时间戳矫正："+add+"\n");
                                    long addo = Long.parseLong(add);

                                    String cor = editTimeCorrect.getText().toString();
                                    long corr = Long.parseLong(cor);

                                    long result = corr + addo;
                                    editTimeCorrect.setText(String.valueOf(result));
                                    tsh.setCorrect(result);
                                }
                                else if(sta.equals("setv:"))
                                {
                                    String voi = mess.substring(5, mess.length()-1);
                                    txtSend.append("设置音量阈值："+voi+"\n");
                                    double voic = Double.parseDouble(voi);

                                    editClientVoice.setText(String.valueOf(voic));
                                    voicelimit = voic;
                                }else {
                                    txtRcv.append("[服务器端]:"+msg.obj.toString());
                                }
                            }
                            else
                            {
                                txtRcv.append("[服务器端]:"+msg.obj.toString());
                            }
                        }
                        break;
                    case 2:
                        //txtSend.append(msg.obj.toString());
                        txtRcv.append("[你]:"+msg.obj.toString()+"\n");
                        break;
                    case 3:
                        //txtSend.append(msg.obj.toString());
                        txtSend.append("获取时间戳:"+msg.obj.toString()+"\n");
                        break;
                    case 4:
                        txtSend.append("发送时间戳:"+msg.obj.toString()+"\n");
                        break;
                    case 5:
                        txtRcv.append(msg.obj.toString()+"\n");

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
                Bundle bundle = msg.getData();
                double db = bundle.getDouble("volume");
                long dates = bundle.getLong("timeStamp");
                String dbs = String.format("%.2f", db);
                txtVolume.setText(dbs);
                if(db > voicelimit){
//                    needPause = true;
                    Message msg1 = myHandler.obtainMessage();
                    Log.d(TAG, "handleMessage: "+tsh.getMydate()+" "+dates);
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
        Intent intent = getIntent();
        String port = intent.getStringExtra("port");
        bindID();
        bindListener();
        bindReceiver();
        Ini();
        editClientPort.setText(port);
    }



    private void bindID(){
        btnStartClient = (Button) findViewById(R.id.btn_tcpClientConn);
        btnCleanClientRcv = (Button) findViewById(R.id.btn_tcpCleanClientRecv);
        btnCleanClientSend = (Button) findViewById(R.id.btn_tcpCleanClientSend);
        btnClientVoice = (Button) findViewById(R.id.btn_tcpClientVoiceLimit);
        btnClientSend = (Button) findViewById(R.id.btn_tcpClientSend);
        btnGetTime = (Button) findViewById(R.id.btn_tcpClientGetTime);
        btnSendTime = (Button) findViewById(R.id.btn_tcpClientSendTime);
        btnTimeCorrect = (Button) findViewById(R.id.btn_tcpClientTimecorrect);
        editClientPort = (EditText) findViewById(R.id.edit_tcpClientPort);
        editClientIp = (EditText) findViewById(R.id.edit_tcpClientIp);
        editClientSend = (EditText) findViewById(R.id.edit_tcpClientSend);
        editClientVoice = (EditText) findViewById(R.id.edit_tcpClientVoiceLimit);
        editTimeCorrect = (EditText) findViewById(R.id.edit_Client_Timecorrect);
        txtRcv = (TextView) findViewById(R.id.txt_ClientRcv);
        txtSend = (TextView) findViewById(R.id.txt_ClientSend);
        txtTime = (TextView) findViewById(R.id.txt_clienttimestamp);
        btnControlAudio = (Button)findViewById(R.id.btn_tcpClientControlAudio);
        txtVolume = (TextView) findViewById(R.id.txt_tcpClientVolumeShow);
        btnLock = (Button)findViewById(R.id.btn_tcpClientLock);
    }
    private void bindListener(){
        btnStartClient.setOnClickListener(myBtnClicker);
        btnCleanClientRcv.setOnClickListener(myBtnClicker);
        btnCleanClientSend.setOnClickListener(myBtnClicker);
        btnClientVoice.setOnClickListener(myBtnClicker);
        btnClientSend.setOnClickListener(myBtnClicker);
        btnGetTime.setOnClickListener(myBtnClicker);
        btnSendTime.setOnClickListener(myBtnClicker);
        btnTimeCorrect.setOnClickListener(myBtnClicker);
        btnControlAudio.setOnClickListener(myBtnClicker);
        btnLock.setOnClickListener(myBtnClicker);
    }
    private void bindReceiver(){
        IntentFilter intentFilter = new IntentFilter("tcpClientReceiver");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }
    private void Ini(){
        btnClientSend.setEnabled(false);
        btnLock.setEnabled(false);
    }
}
