package jason.tcpdemo.funcs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jason.tcpdemo.MainActivity;
import jason.tcpdemo.MyApp;
import jason.tcpdemo.R;
import jason.tcpdemo.coms.AudioHelper;
import jason.tcpdemo.coms.TcpServer;

import static android.content.ContentValues.TAG;

/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class FuncTcpServer_3 extends Activity {
    private MyApp myapp;
    private Button btnListenTime,btnTest,btnCleanServerRcv,btnPrev3;
    private TextView txtServerResult,txtHistory;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private TimeStampHelper tsh = new TimeStampHelper();
    private TimeStampHelper tsh1 = new TimeStampHelper();
    private TimeStampHelper tsh2 = new TimeStampHelper();
    private boolean isAudioRun = false;
    private boolean getMaxFromp1 = false, getMaxFromp2 = false;
    private long diff_Max = 0,diff_Over = 0;
    private long lastheartbeat1 = 0, lastheartbeat2 = 0;
    private Object lock = new Object();
    private Thread AutoThread, heartbeatThread;
    private final String HISTORY_PATH = MyApp.mainActivity.getExternalFilesDir(null).toString()+"/HistoryLog";
    //storage/emulated/0/Android/data/jason.tcpdemo/files
    private FileWriter writer = null;

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

    private void showResult() {
        long diffMax = tsh1.calcul_client_diff();
        long diffOver = tsh2.calcul_client_diff();
        diff_Max = diffMax;
        diff_Over = diffOver;
        Message messageMaxDiff = Message.obtain();
                                    /*messageMaxDiff.what = 1;
                                    messageMaxDiff.obj = "[port2]最大音量："+tempV+"\n"
                                            +"[port2]最大值时间戳："+tempT+"\n"
                                            +"最大值时间差[port1-port2]："+diffMax+"\n"
                                            +"[port2]阈值时间戳："+overTempT+"\n"
                                            +"阈值时间差[port1-port2]："+diffOver+"\n";*/

        messageMaxDiff.what = 4;
        //messageMaxDiff.obj = "最大值时间差[炮位1-炮位2]:"+diffMax+"ms";
        if(diffMax>0)
        {
            messageMaxDiff.obj = "[炮位1]比[炮位2]慢了:"+diffMax+"ms";
        }
        else if(diffMax<0)
        {
            diffMax = -diffMax;
            messageMaxDiff.obj = "[炮位2]比[炮位1]慢了:"+diffMax+"ms";
        }
        else
        {
            messageMaxDiff.obj = "[炮位1]和[炮位2]时间一致";
        }
        myHandler.sendMessage(messageMaxDiff);
    }


    private class MyHandler extends android.os.Handler{
        private final WeakReference<FuncTcpServer_3> mActivity;
        MyHandler(FuncTcpServer_3 activity){
            mActivity = new WeakReference<FuncTcpServer_3>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FuncTcpServer_3 activity = mActivity.get();
            if (activity!= null){
                String mess;
                switch (msg.what){
                    case 1:
                        mess = msg.obj.toString();
                        txtHistory.append(msg.obj.toString()+"\n");
                        break;
                    case 4:
                        txtServerResult.setText(msg.obj.toString());
                        String da = tsh.getDateToString();
                        txtHistory.append(da+":"+msg.obj.toString()+"\n");
                        String log = da + " " + msg.obj.toString() + "\n";
                        writeHistoryLog(log);
                        break;
                    case 5:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring:" + sta);
                            if (sta.equals("hatbe"))
                            {
                                /*Log.i(TAG, "get heartbeat from p1");
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        myapp.tcpServer1.SST.get(0).send("hatbe");
                                    }
                                });*/
                                long hearttime = System.currentTimeMillis();

                                lastheartbeat1 = hearttime;
                            }
                            else if (sta.equals("[新的客户"))
                            {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        myapp.tcpServer1.SST.get(0).send("[已作为 炮位1 连入服务器]");
                                    }
                                });
                                txtHistory.append("[炮位1]"+msg.obj.toString());
                            }
                            else if (sta.equals("time:")) {
                                String mun = mess.substring(5, mess.length()-1);
                                long ot = Long.parseLong(mun);
                                //txtSend.append("[port1]时间戳："+mun+"\n");
                                tsh.setClientdate1(ot);
                                //txtTime1.setText("[port1]:"+ot);
                            }
                            else if(sta.equals("maxV:")){
                                int loc1 = mess.indexOf("maxTimeStamp");
                                int loc2 = mess.indexOf("overTimeStamp");

                                double tempV = Double.parseDouble(mess.substring(5, loc1));
                                long tempT = Long.parseLong(mess.substring(loc1 + 13, loc2));
                                long overTempT = Long.parseLong(mess.substring(loc2 + 14, mess.length() - 2));
                                tsh1.setClientdate1(tempT);
                                tsh2.setClientdate1(overTempT);
                                getMaxFromp1 = true;
                                if(getMaxFromp1 && getMaxFromp2){
                                    showResult();

                                } else {
                                    /*Message msgPort1 = Message.obtain();
                                    msgPort1.what = 1;
                                    msgPort1.obj = "[port1]最大音量："+tempV+"\n"
                                            +"[port1]最大值时间戳："+tempT+"\n"
                                            +"[port1]阈值时间戳："+overTempT+"\n";
                                    myHandler.sendMessage(msgPort1);*/
                                }

                            }
                            else {
                                txtHistory.append("[炮位1]"+msg.obj.toString());
                            }
                        }
                        else
                        {
                            txtHistory.append("[炮位1]"+msg.obj.toString());
                        }
                        //txtRcv.append("[port1]"+msg.obj.toString());
                        break;
                    case 6:
                        mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("hatbe"))
                            {
                                /*Log.i(TAG, "get heartbeat from p2");
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        myapp.tcpServer2.SST.get(0).send("hatbe");
                                    }
                                });*/
                                long hearttime = System.currentTimeMillis();

                                lastheartbeat2 = hearttime;
                            }
                            else if (sta.equals("[新的客户"))
                            {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        myapp.tcpServer2.SST.get(0).send("[已作为 炮位2 连入服务器]");
                                    }
                                });
                                txtHistory.append("[炮位2]"+msg.obj.toString());
                            }
                            else if (sta.equals("time:")) {
                                String mun = mess.substring(5, mess.length()-1);
                                long ot = Long.parseLong(mun);
                                //txtSend.append("[port2]时间戳："+mun+"\n");
                                tsh.setClientdate2(ot);
                                //txtTime2.setText("[port2]:"+ot);
                            }
                            else if(sta.equals("maxV:")){
                                int loc1 = mess.indexOf("maxTimeStamp");
                                int loc2 = mess.indexOf("overTimeStamp");

                                double tempV = Double.parseDouble(mess.substring(5, loc1));
                                long tempT = Long.parseLong(mess.substring(loc1 + 13, loc2));
                                long overTempT = Long.parseLong(mess.substring(loc2 + 14, mess.length() - 2));
                                tsh2.setClientdate2(overTempT);
                                tsh1.setClientdate2(tempT);
                                getMaxFromp2 = true;
                                if(getMaxFromp1 && getMaxFromp2){
                                    showResult();

                                } else {
                                    /*Message msgPort2 = Message.obtain();
                                    msgPort2.what = 1;
                                    msgPort2.obj = "[port2]最大音量："+tempV+"\n"
                                            +"[port2]最大值时间戳："+tempT+"\n"
                                            +"[port2]阈值时间戳："+overTempT+"\n";
                                    myHandler.sendMessage(msgPort2);*/
                                }

                            }
                            else {
                                txtHistory.append("[炮位2]"+msg.obj.toString());
                            }
                        }
                        else
                        {
                            txtHistory.append("[炮位2]"+msg.obj.toString());
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
                case R.id.btn_tcpServerTest:
                    txtHistory.append("连接测试...\n");
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myapp.tcpServer1.SST.get(0).send("conTest");
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myapp.tcpServer2.SST.get(0).send("conTest");
                        }
                    });
                    break;

                case R.id.btn_tcpCleanServerRecv:
                    txtHistory.setText("");
                    break;

                case R.id.btn_tcpServerListenTime:
                    if(isAudioRun)
                    {
                        isAudioRun = false;
                        btnListenTime.setText("3.测试开始");
                    }
                    else
                    {
                        isAudioRun = true;
                        btnListenTime.setText("3.测试结束");
                        getMaxFromp1 = false;
                        getMaxFromp2 = false;
                    }

                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myapp.tcpServer1.SST.get(0).send("ListenTime");
                        }
                    });
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myapp.tcpServer2.SST.get(0).send("ListenTime");
                        }
                    });
                    break;

                case R.id.btn_tcpServerPrev3:
                    finish();
                    break;

            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG,"crash check1");
        setContentView(R.layout.tcp_server_3);
        context = this;
        myapp = (MyApp) this.getApplication();
        bindID();
        bindListener();
        bindReceiver();
        ini();
        genTodayFile();
        Log.e(TAG, "HistoryPath"+HISTORY_PATH );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private void ini(){
        txtHistory.setMovementMethod(ScrollingMovementMethod.getInstance());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                while(true) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long hearttime = System.currentTimeMillis();
                    if(lastheartbeat1 != 0 && hearttime > lastheartbeat1 + 10000)
                    {
                        Message msgloss = Message.obtain();
                        msgloss.what = 1;
                        msgloss.obj = "炮位1心跳超时，请重启！为保证时间体系能对齐，请退回到第二步，重新进行时间体系对齐。";
                        myHandler.sendMessage(msgloss);
                        //Toast toast=Toast.makeText(FuncTcpServer_3.this, "炮位1心跳超时，建议重启！", Toast.LENGTH_SHORT);
                        //toast.show();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                    if(lastheartbeat2 != 0 && hearttime > lastheartbeat2 + 10000)
                    {
                        Message msgloss = Message.obtain();
                        msgloss.what = 1;
                        msgloss.obj = "炮位2心跳超时，请重启！为保证时间体系能对齐，请退回到第二步，重新进行时间体系对齐。";
                        myHandler.sendMessage(msgloss);
                        //Toast toast=Toast.makeText(FuncTcpServer_3.this, "炮位2心跳超时，建议重启！", Toast.LENGTH_SHORT);
                        //toast.show();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        };
        heartbeatThread = new Thread(runnable);
        heartbeatThread.start();
    }

    private void bindListener() {
        btnCleanServerRcv.setOnClickListener(myBtnClicker);
        btnTest.setOnClickListener(myBtnClicker);
        btnListenTime.setOnClickListener(myBtnClicker);
        btnPrev3.setOnClickListener(myBtnClicker);
    }

    private void bindID() {
        btnCleanServerRcv = (Button) findViewById(R.id.btn_tcpCleanServerRecv);
        btnTest = (Button) findViewById(R.id.btn_tcpServerTest);
        btnListenTime = (Button) findViewById(R.id.btn_tcpServerListenTime);
        btnPrev3 = (Button) findViewById(R.id.btn_tcpServerPrev3);
        txtHistory = (TextView) findViewById(R.id.txt_ServerHistory);
        txtServerResult = (TextView) findViewById(R.id.txt_timeresult);
    }

    private void writeHistoryLog(String info){
        try{
            writer.write(info);
            writer.flush();
        }catch (FileNotFoundException e){
            e.printStackTrace();
            Log.e(TAG, "未创建log文件" );
        }catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, "写入错误" );
        }

    }
    private void genTodayFile(){
        File historyDir = new File(HISTORY_PATH);
        if(!historyDir.exists()){
            historyDir.mkdir();
        }
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String todayFilePath = date.format(formatter) + ".txt";
        File todayFile = new File(HISTORY_PATH, todayFilePath);
        try{
            writer = new FileWriter(HISTORY_PATH+"/"+todayFilePath, true);
            Log.e(TAG, "log文件路径"+ HISTORY_PATH+"/"+todayFilePath);
        }catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, "filewriter路径不存在");
        }

        if(!todayFile.exists()){
            try{
                todayFile.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
            Log.e(TAG, "生成今日log文件，todayFilePath："+ todayFilePath );
        }
    }

}
