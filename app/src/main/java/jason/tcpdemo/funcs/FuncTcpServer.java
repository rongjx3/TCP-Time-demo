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

import jason.tcpdemo.R;
import jason.tcpdemo.coms.TcpServer;

import static android.content.ContentValues.TAG;

/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class FuncTcpServer extends Activity {
    private Button btnStartServer,btnCloseServer, btnCleanServerSend, btnCleanServerRcv,btnServerSend,btnServerRandom;
    private Button btnGetTime, btnSendTime, btnCalTime, btnTimeCorrect;
    private TextView txtRcv,txtSend,txtServerIp,txtTime;
    private EditText editServerSend,editServerID, editServerPort,editTimeCorrect;
    private static TcpServer tcpServer = null;
    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private TimeStampHelper tsh = new TimeStampHelper();
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
                switch (msg.what){
                    case 1:
                        String mess = msg.obj.toString();
                        if(mess.length()>=5) {
                            String sta = mess.substring(0, 5);
                            Log.i(TAG, "substring : " + sta);
                            if (sta.equals("time:")) {
                                String mun = mess.substring(5, mess.length()-1);
                                long ot = Long.parseLong(mun);
                                txtSend.append("对方时间戳："+mun+"\n");
                                tsh.setOtherdate(ot);
                            } else {
                                txtRcv.append("对方："+msg.obj.toString());
                            }
                        }
                        else
                        {
                            txtRcv.append("对方："+msg.obj.toString());
                        }
                        break;
                    case 2:
                        //txtSend.append(msg.obj.toString());
                        txtRcv.append("你："+msg.obj.toString()+"\n");
                        break;
                    case 3:
                        //txtSend.append(msg.obj.toString());
                        txtSend.append("时间戳差值（你的-对方的）："+msg.obj.toString()+"ms\n");
                        break;
                    case 4:
                        txtSend.append("你的时间戳："+msg.obj.toString()+"\n");
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
                case "tcpServerReceiver":
                    String msg = intent.getStringExtra("tcpServerReceiver");
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = msg;
                    myHandler.sendMessage(message);
                    break;
            }
        }
    }

    private void bindReceiver(){
        IntentFilter intentFilter = new IntentFilter("tcpServerReceiver");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }

    private class MyBtnClicker implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btn_tcpServerConn:
                    Log.i("A", "onClick: 开始");
                    btnStartServer.setEnabled(false);
                    btnCloseServer.setEnabled(true);
                    btnServerSend.setEnabled(true);
                    tcpServer = new TcpServer(getHost(editServerPort.getText().toString()));
                    exec.execute(tcpServer);
                    break;
                case R.id.btn_tcpServerClose:
                    tcpServer.closeSelf();
                    btnStartServer.setEnabled(true);
                    btnCloseServer.setEnabled(false);
                    btnServerSend.setEnabled(false);
                    break;
                case R.id.btn_tcpCleanServerRecv:
                    txtRcv.setText("");
                    break;
                case R.id.btn_tcpCleanServerSend:
                    txtSend.setText("");
                    break;
                case R.id.btn_tcpServerRandomID:
                    editServerID.setText("Server");
                    break;
                case R.id.btn_tcpServerSend:
                    Message message = Message.obtain();
                    message.what = 2;
                    message.obj = editServerSend.getText().toString();
                    myHandler.sendMessage(message);
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(0).send(editServerSend.getText().toString());
                        }
                    });
                    break;

                case R.id.btn_tcpServerGetTime:
                    long date = tsh.getMydate_local();
                    Log.i(TAG, "timestamp: "+date);
                    txtTime.setText(String.valueOf(date));
                    break;
                case R.id.btn_tcpServerSendTime:
                    long dates = tsh.getMydate();
                    Message messagetime = Message.obtain();
                    messagetime.what = 4;
                    messagetime.obj = String.valueOf(dates);
                    myHandler.sendMessage(messagetime);
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(0).send("time:"+txtTime.getText().toString());
                        }
                    });
                    break;
                case R.id.btn_tcpServerCalTime:
                    long diff = tsh.calcul_diff();
                    Message messagediff = Message.obtain();
                    messagediff.what = 3;
                    messagediff.obj = String.valueOf(diff);
                    myHandler.sendMessage(messagediff);
                    break;
                case R.id.btn_tcpServerTimecorrect:
                    String cor = editTimeCorrect.getText().toString();
                    long corr = Long.parseLong(cor);
                    tsh.setCorrect(corr);
            }
        }
    }

    private int getHost(String msg){
        if (msg.equals("")){
            msg = "1234";
        }
        return Integer.parseInt(msg);
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tcp_server);
        context = this;
        bindID();
        bindListener();
        bindReceiver();
        ini();
    }

    private void ini(){
        btnCloseServer.setEnabled(false);
        btnServerSend.setEnabled(false);
        txtServerIp.setText(getHostIP());
    }

    private void bindListener() {
        btnStartServer.setOnClickListener(myBtnClicker);
        btnCloseServer.setOnClickListener(myBtnClicker);
        btnCleanServerRcv.setOnClickListener(myBtnClicker);
        btnCleanServerSend.setOnClickListener(myBtnClicker);
        btnServerRandom.setOnClickListener(myBtnClicker);
        btnServerSend.setOnClickListener(myBtnClicker);
        btnGetTime.setOnClickListener(myBtnClicker);
        btnSendTime.setOnClickListener(myBtnClicker);
        btnCalTime.setOnClickListener(myBtnClicker);
        btnTimeCorrect.setOnClickListener(myBtnClicker);
    }

    private void bindID() {
        btnStartServer = (Button) findViewById(R.id.btn_tcpServerConn);
        btnCloseServer = (Button) findViewById(R.id.btn_tcpServerClose);
        btnCleanServerRcv = (Button) findViewById(R.id.btn_tcpCleanServerRecv);
        btnCleanServerSend = (Button) findViewById(R.id.btn_tcpCleanServerSend);
        btnServerRandom = (Button) findViewById(R.id.btn_tcpServerRandomID);
        btnServerSend = (Button) findViewById(R.id.btn_tcpServerSend);
        btnGetTime = (Button) findViewById(R.id.btn_tcpServerGetTime);
        btnSendTime = (Button) findViewById(R.id.btn_tcpServerSendTime);
        btnCalTime = (Button) findViewById(R.id.btn_tcpServerCalTime);
        btnTimeCorrect = (Button) findViewById(R.id.btn_tcpServerTimecorrect);
        txtRcv = (TextView) findViewById(R.id.txt_ServerRcv);
        txtSend = (TextView) findViewById(R.id.txt_ServerSend);
        txtServerIp = (TextView) findViewById(R.id.txt_Server_Ip);
        editServerID = (EditText) findViewById(R.id.edit_Server_ID);
        editServerSend = (EditText) findViewById(R.id.edit_tcpClientSend);
        editServerPort = (EditText)findViewById(R.id.edit_Server_Port);
        editTimeCorrect = (EditText) findViewById(R.id.edit_Server_Timecorrect);
        txtTime = (TextView) findViewById(R.id.txt_timestamp);
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
