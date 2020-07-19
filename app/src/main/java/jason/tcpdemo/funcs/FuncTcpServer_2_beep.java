package jason.tcpdemo.funcs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jason.tcpdemo.MainActivity;
import jason.tcpdemo.MyApp;
import jason.tcpdemo.R;

import static android.content.ContentValues.TAG;

public class FuncTcpServer_2_beep extends Activity {
    private MyApp myapp;
    private Button btnCheckTime, btnNext2, btnPrev2;
    private TextView txtCheckStatus;
    private boolean getfromp1 = false, getfromp2 = false, correcting = false;
    ExecutorService exec = Executors.newCachedThreadPool();
    public static MainActivity beepContext;
    private BeepHelper beepHelper = new BeepHelper(beepContext);
    private MyButtonClicker myButtonClicker = new MyButtonClicker();
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private Thread timeCheckThread;
    private MyHandler myHandler = new MyHandler();
    private TimeStampHelper tsh = new TimeStampHelper();
    private long checkTimeLimit = 20;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tcp_server_2_beep);
        myapp = (MyApp)this.getApplication();
        bindID();
        bindListener();
        bindReceiver();
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            String mess = msg.obj.toString();
            String sta = mess.substring(0, 5);
            switch (msg.what){
                case 2:
                    txtCheckStatus.setText("标定成功，请点击下一步");
                    btnCheckTime.setText("已标定成功");
                    btnCheckTime.setEnabled(false);
                    break;
                case 5:
                    if(sta.equals("maxV:")) {
                        int loc1 = mess.indexOf("maxTimeStamp");
                        int loc2 = mess.indexOf("overTimeStamp");

                        double tempV = Double.parseDouble(mess.substring(5, loc1));
                        long tempT = Long.parseLong(mess.substring(loc1 + 13, loc2));
                        long overTempT = Long.parseLong(mess.substring(loc2 + 14, mess.length() - 2));
                        tsh.setClientdate1(tempT);
                        getfromp1 = true;
                    }
                    break;
                case 6:
                    if(sta.equals("maxV:")) {
                        int loc1 = mess.indexOf("maxTimeStamp");
                        int loc2 = mess.indexOf("overTimeStamp");

                        double tempV = Double.parseDouble(mess.substring(5, loc1));
                        long tempT = Long.parseLong(mess.substring(loc1 + 13, loc2));
                        long overTempT = Long.parseLong(mess.substring(loc2 + 14, mess.length() - 2));
                        tsh.setClientdate2(tempT);
                        getfromp2 = true;
                    }
                    break;
            }
        }
    }

    private class MyButtonClicker implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_tcpServerCheckTimeBeep:
                    btnCheckTime.setEnabled(false);
                    btnCheckTime.setText("正在对齐...");
                    getfromp1 = false;
                    getfromp2 = false;
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            int successNum = 0;
                            long lastCorrectAmount = 0;
                            while (successNum < 3){
                                Log.e(TAG, "successNum: "+successNum);
                                txtCheckStatus.setText("正在标定");
                                Log.e(TAG, "打开声音监听" );
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
                                try {
                                    Thread.sleep(3000);
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                                Log.e(TAG, "蜂鸣。。" );
                                beepHelper.beep();
                                try {
                                    Thread.sleep(3000);
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                                Log.e(TAG, "关闭声音监听，并返回时间戳" );
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
                                try {
                                    Thread.sleep(1000);
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                }
                                Log.e(TAG, "getfromp1:"+String.valueOf(getfromp1)
                                        +"\ngetfromp2:"+String.valueOf(getfromp2) );
                                if(getfromp1 && getfromp2){
                                    getfromp1 = false;
                                    getfromp2 = false;
                                    final long correctAmount = -tsh.calcul_client_diff();
                                    Log.e(TAG, "run: "+String.valueOf(Math.abs(correctAmount)));
                                    try {
                                        Thread.sleep(500);
                                    }catch (InterruptedException e){
                                        e.printStackTrace();
                                    }
                                    exec.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            myapp.tcpServer1.SST.get(0).send("cort:"+String.valueOf(correctAmount));
                                        }
                                    });
                                    try {
                                        Thread.sleep(500);
                                    }catch (InterruptedException e){
                                        e.printStackTrace();
                                    }
                                    if(Math.abs(correctAmount) < checkTimeLimit){
                                        successNum++;
                                    }else {
                                        successNum = 0;
                                    }
                                } else {
                                    Log.e(TAG, "run: "+"客户端监听出现错误" );
                                    successNum = 0;
                                    getfromp1 = false;
                                    getfromp2 = false;
                                }
                            }
                            Message msg = Message.obtain();
                            msg.what = 2;
                            msg.obj = "correct succ";
                            myHandler.sendMessage(msg);
                            //txtCheckStatus.setText("标定成功，请点击下一步");
                            //btnCheckTime.setText("已标定成功");
                            //btnCheckTime.setEnabled(false);
                        }
                    };
                    timeCheckThread = new Thread(runnable);
                    timeCheckThread.start();
                    break;
                case R.id.btn_tcpServerNext2Beep:
                    Intent intent = new Intent();
                    intent.setClass(FuncTcpServer_2_beep.this,FuncTcpServer_3.class);
                    startActivity(intent);
                    break;
                case R.id.btn_tcpServerPrev2Beep:
                    finish();
                    break;
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

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

    private class BeepHelper {
        private MediaPlayer mediaPlayer = new MediaPlayer();
        private static final String TAG = "BeepHelper";
        private Uri notification;

        BeepHelper(Context context){
            Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.beep);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
//                    mp.seekTo(0);
                }
            });
            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
//                    Log.e(TAG, "已经调整到初始位置");
                }
            });
            try {
                mediaPlayer.setDataSource(context, uri);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        void beep(){
            mediaPlayer.start();
        }
    }

    private void bindListener() {
        btnCheckTime.setOnClickListener(myButtonClicker);
        btnNext2.setOnClickListener(myButtonClicker);
        btnPrev2.setOnClickListener(myButtonClicker);
    }

    private void bindID() {
        btnCheckTime = (Button) findViewById(R.id.btn_tcpServerCheckTimeBeep);
        btnNext2 = (Button) findViewById(R.id.btn_tcpServerNext2Beep);
        btnPrev2 = (Button) findViewById(R.id.btn_tcpServerPrev2Beep);
        txtCheckStatus = (TextView) findViewById(R.id.txt_tcpServerCheckStatusBeep);

    }
    private void bindReceiver(){
        //IntentFilter intentFilter = new IntentFilter("tcpServerReceiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("tcpServerReceiver");
        intentFilter.addAction("tcpServerReceiver1");
        intentFilter.addAction("tcpServerReceiver2");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }
}
