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

import jason.tcpdemo.R;
import jason.tcpdemo.coms.AudioHelper;
import jason.tcpdemo.coms.TcpServer;

import static android.content.ContentValues.TAG;

/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class FuncTcpServer extends Activity {
    private Button btnStartServer, btnCheckTime, btnListenTime;
    private Button btnTest,btnCleanServerRcv,btnServerVoice,btnSwitch;
    private TextView txtRcv,txtServerIp,txtServerCorrect,txtServerResult;
    private EditText editServerVoice, editServerPort1,editServerPort2;
    private AudioHelper audioHelper = new AudioHelper();
    private static TcpServer tcpServer1 = null, tcpServer2 = null;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private TimeStampHelper tsh = new TimeStampHelper();
    private TimeStampHelper tsh1 = new TimeStampHelper();
    private boolean isAudioRun = false;
    private boolean getfromp1 = false, getfromp2 = false, correcting = false;
    private boolean getMaxFromp1 = false, getMaxFromp2 = false;
    //private AudioHandler audioHandler = new AudioHandler(this);
    /*private boolean isAudioRun = false;
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
    private boolean needStop = false;
    private boolean isfirstListen = true;*/
    private Object lock = new Object();
    private boolean needPause = false;

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
                        mess = msg.obj.toString();
                        txtRcv.append(msg.obj.toString());
                        break;
                    case 2:
                        //txtSend.append(msg.obj.toString());
                        txtRcv.append("[你]:"+msg.obj.toString()+"\n");
                        break;
                    case 3:
                        //txtSend.append(msg.obj.toString());
                        txtServerCorrect.setText("[标定误差]："+msg.obj.toString()+"ms");
                        break;
                    case 4:
                        txtServerResult.setText("[时间差]([port1]-[port2]):"+msg.obj.toString()+"ms");
                        break;
                    case 5:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("[新的客户"))
                            {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        tcpServer1.SST.get(0).send("[已作为 port1 连入服务器]");
                                    }
                                });
                                txtRcv.append("[port1]"+msg.obj.toString());
                            }
                            else if (sta.equals("time:")) {
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
                                    Message messagediff = Message.obtain();
                                    messagediff.what = 3;
                                    messagediff.obj = String.valueOf(-diff);
                                    myHandler.sendMessage(messagediff);
                                    final String diff_s= String.valueOf(-diff);
                                    exec.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            tcpServer1.SST.get(0).send("cort:" + diff_s);
                                        }
                                    });
                                }
                            }
                            else if(sta.equals("maxV:")){
                                int loc1 = 0;
                                for(int i = 5; i < mess.length(); i++){
                                    if(mess.toCharArray()[i] == '时'){
                                        loc1 = i;
                                        break;
                                    }
                                }
                                double tempV = Double.parseDouble(mess.substring(5, loc1));
                                long tempT = Long.parseLong(mess.substring(loc1+4, mess.length()-2));
                                tsh1.setClientdate1(tempT);
                                getMaxFromp1 = true;
                                if(getMaxFromp1 && getMaxFromp2){
                                    long diff = tsh1.calcul_client_diff();
                                    Message messageMaxDiff = Message.obtain();
                                    /*messageMaxDiff.what = 1;
                                    messageMaxDiff.obj = "[port1]最大音量："+tempV+"\n"
                                            +"[port1]时间戳："+tempT+"\n"
                                            +"时间差[port1-port2]："+diff+"\n";*/
                                    messageMaxDiff.what = 4;
                                    messageMaxDiff.obj = ""+diff;

                                    myHandler.sendMessage(messageMaxDiff);
                                } else {
                                    /*Message msgPort1 = Message.obtain();
                                    msgPort1.what = 1;
                                    msgPort1.obj = "[port1]最大音量："+tempV+"\n"
                                            +"[port1]时间戳："+tempT+"\n";
                                    myHandler.sendMessage(msgPort1);*/
                                }

                            }
                            else {
                                txtRcv.append("[port1]"+msg.obj.toString());
                            }
                        }
                        else
                        {
                            txtRcv.append("[port1]"+msg.obj.toString());
                        }
                        //txtRcv.append("[port1]"+msg.obj.toString());
                        break;
                    case 6:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("[新的客户"))
                            {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        tcpServer2.SST.get(0).send("[已作为 port2 连入服务器]");
                                    }
                                });
                                txtRcv.append("[port2]"+msg.obj.toString());
                            }
                            else if (sta.equals("time:")) {
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
                                    Message messagediff = Message.obtain();
                                    messagediff.what = 3;
                                    messagediff.obj = String.valueOf(-diff);
                                    myHandler.sendMessage(messagediff);
                                    final String diff_s= String.valueOf(-diff);
                                    exec.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            tcpServer1.SST.get(0).send("cort:" + diff_s);
                                        }
                                    });
                                }
                            }
                            else if(sta.equals("maxV:")){
                                int loc1 = 0;
                                for(int i = 5; i < mess.length(); i++){
                                    if(mess.toCharArray()[i] == '时'){
                                        loc1 = i;
                                        break;
                                    }
                                }
                                double tempV = Double.parseDouble(mess.substring(5, loc1));
                                long tempT = Long.parseLong(mess.substring(loc1+4, mess.length()-2));
                                tsh1.setClientdate2(tempT);
                                getMaxFromp2 = true;
                                if(getMaxFromp1 && getMaxFromp2){
                                    long diff = tsh1.calcul_client_diff();
                                    Message messageMaxDiff = Message.obtain();
                                    /*messageMaxDiff.what = 1;
                                    messageMaxDiff.obj = "[port2]最大音量："+tempV+"\n"
                                            +"[port2]时间戳："+tempT+"\n"
                                            +"时间差[port1-port2]："+diff+"\n";*/
                                    messageMaxDiff.what = 4;
                                    messageMaxDiff.obj = ""+diff;
                                    myHandler.sendMessage(messageMaxDiff);
                                } else {
                                    /*Message msgPort2 = Message.obtain();
                                    msgPort2.what = 1;
                                    msgPort2.obj = "[port2]最大音量："+tempV+"\n"
                                            +"[port2]时间戳："+tempT+"\n";
                                    myHandler.sendMessage(msgPort2);*/
                                }

                            }
                            else {
                                txtRcv.append("[port2]"+msg.obj.toString());
                            }
                        }
                        else
                        {
                            txtRcv.append("[port2]"+msg.obj.toString());
                        }
                        //txtRcv.append("[port2]"+msg.obj.toString());
                        break;

                }
            }
        }
    }

    /*private class AudioHandler extends android.os.Handler{
        private WeakReference<FuncTcpServer> mActivity;
        AudioHandler(FuncTcpServer activity){
            mActivity = new WeakReference<FuncTcpServer>(activity);
            Log.d(TAG, "run111: "+Thread.currentThread().getId());
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if(mActivity != null){
                double db = (double) msg.obj;
                txtVolume.setText(String.valueOf(db));
                if(db > 78){
                    needPause = true;
                    Message msg1 = myHandler.obtainMessage();
                    long dates = tsh.getMydate_local();
                    msg1.what = 4;
                    msg1.obj = String.valueOf(dates);
                    myHandler.sendMessage(msg1);
                    txtTime1.setText(String.valueOf(dates));
                }
            }
        }
    }*/

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
                case R.id.btn_tcpServerConn:
                    Log.i("A", "onClick: 开始");
                    btnStartServer.setEnabled(false);
                    btnStartServer.setText("已开启服务器");
                    btnTest.setEnabled(true);
                    tcpServer1 = new TcpServer(getHost(editServerPort1.getText().toString()),"1");
                    exec.execute(tcpServer1);
                    tcpServer2 = new TcpServer(getHost(editServerPort2.getText().toString()),"2");
                    exec.execute(tcpServer2);
                    break;
                case R.id.btn_tcpServerTest:
                    txtRcv.append("连接测试...\n");
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer1.SST.get(0).send("conTest");
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer2.SST.get(0).send("conTest");
                        }
                    });
                    break;
                case R.id.btn_tcpCleanServerRecv:
                    txtRcv.setText("");
                    break;
                case R.id.btn_tcpServerVoiceLimit:
                    final String voi = editServerVoice.getText().toString();
                    double voic = Double.parseDouble(voi);

                    Message messagediff = Message.obtain();
                    messagediff.what = 2;
                    messagediff.obj = "设置客户端音量阈值："+voi;
                    myHandler.sendMessage(messagediff);

                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer1.SST.get(0).send("setv:" + voi);
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer2.SST.get(0).send("setv:" + voi);
                        }
                    });
                    break;

                case R.id.btn_tcpServerCheckTime:
                    getfromp1 = false;
                    getfromp2 = false;
                    correcting = true;
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer1.SST.get(0).send("Timecheck");
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer2.SST.get(0).send("Timecheck");
                        }
                    });

                    break;

                case R.id.btn_tcpServerListenTime:
                    if(isAudioRun)
                    {
                        isAudioRun = false;
                        btnListenTime.setText("3.远程开关:OFF");
                    }
                    else
                    {
                        isAudioRun = true;
                        btnListenTime.setText("3.远程开关:ON");
                        getMaxFromp1 = false;
                        getMaxFromp2 = false;
                    }

                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer1.SST.get(0).send("ListenTime");
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer2.SST.get(0).send("ListenTime");
                        }
                    });
                    break;
                case R.id.btn_tcpServerSwitch:

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
        bindID();
        bindListener();
        bindReceiver();
        ini();
    }

    private void ini(){
        btnTest.setEnabled(false);
        txtServerIp.setText(getHostIP());
    }

    private void bindListener() {
        btnStartServer.setOnClickListener(myBtnClicker);
        btnCleanServerRcv.setOnClickListener(myBtnClicker);
        btnServerVoice.setOnClickListener(myBtnClicker);
        btnCheckTime.setOnClickListener(myBtnClicker);
        btnTest.setOnClickListener(myBtnClicker);
        btnListenTime.setOnClickListener(myBtnClicker);
        btnSwitch.setOnClickListener(myBtnClicker);
    }

    private void bindID() {
        btnStartServer = (Button) findViewById(R.id.btn_tcpServerConn);
        btnCleanServerRcv = (Button) findViewById(R.id.btn_tcpCleanServerRecv);
        btnServerVoice = (Button) findViewById(R.id.btn_tcpServerVoiceLimit);
        btnCheckTime = (Button) findViewById(R.id.btn_tcpServerCheckTime);
        btnTest = (Button) findViewById(R.id.btn_tcpServerTest);
        btnListenTime = (Button) findViewById(R.id.btn_tcpServerListenTime);
        btnSwitch = (Button) findViewById(R.id.btn_tcpServerSwitch);
        txtRcv = (TextView) findViewById(R.id.txt_ServerRcv);
        txtServerIp = (TextView) findViewById(R.id.txt_Server_Ip);
        txtServerCorrect = (TextView) findViewById(R.id.txt_timecorrect);
        txtServerResult = (TextView) findViewById(R.id.txt_timeresult);
        editServerVoice = (EditText) findViewById(R.id.edit_tcpServerVoiceLimit);
        editServerPort1 = (EditText)findViewById(R.id.edit_Server_Port1);
        editServerPort2 = (EditText)findViewById(R.id.edit_Server_Port2);
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
