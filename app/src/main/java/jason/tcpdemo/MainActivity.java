package jason.tcpdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.w3c.dom.Text;

import jason.tcpdemo.funcs.FuncHistory;
import jason.tcpdemo.funcs.FuncTcpClient;
import jason.tcpdemo.funcs.FuncTcpServer;
import jason.tcpdemo.funcs.FuncTcpServer_2_beep;
import jason.tcpdemo.funcs.FuncTcpServer_3;
import jason.tcpdemo.util.PermissionsUtil;

public class MainActivity extends Activity implements PermissionsUtil.IPermissionsCallback{

    private MyApp myapp;
    private RadioButton radioBtnServer,radioBtnClient1,radioBtnClient2;
    private Button btnFuncEnsure, btnFuncHistory;
    private TextView txtShowFunc;
    private MyRadioButtonCheck myRadioButtonCheck = new MyRadioButtonCheck();
    private MyButtonClick myButtonClick = new MyButtonClick();
    private PermissionsUtil permissionsUtil;

    private class MyRadioButtonCheck implements RadioButton.OnCheckedChangeListener{

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()){
                case R.id.radio_Server:
                    if (b){
                        txtShowFunc.setText("你选择的位置是：计时员");
                    }
                    break;
                case R.id.radio_Client1:
                    if (b){
                        txtShowFunc.setText("你选择的位置是：炮位1");
                    }
                    break;
                case R.id.radio_Client2:
                    if (b){
                        txtShowFunc.setText("你选择的位置是：炮位2");
                    }
                    break;
            }
        }
    }

    private class MyButtonClick implements Button.OnClickListener{

        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            switch (view.getId()){
                case R.id.btn_FunctionEnsure:
                    if (radioBtnServer.isChecked()){
                        myapp.name="你的位置：计时员";
                        intent.setClass(MainActivity.this,FuncTcpServer.class);
                        startActivity(intent);
                    }
                    if (radioBtnClient1.isChecked()){
                        myapp.name="你的位置：炮位1";
                        intent.setClass(MainActivity.this, FuncTcpClient.class);
                        intent.putExtra("port","1232");
                        startActivity(intent);
                    }
                    if (radioBtnClient2.isChecked()){
                        myapp.name="你的位置：炮位2";
                        intent.setClass(MainActivity.this, FuncTcpClient.class);
                        intent.putExtra("port","1233");
                        startActivity(intent);
                    }
                    break;
                case R.id.btn_FunctionHistory:
                    intent.setClass(MainActivity.this, FuncHistory.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function);
        myapp = (MyApp) this.getApplication();
        getPermission();
        bindID();
        bindListener();
        MyApp.mainActivity = MainActivity.this;
    }

    private void bindID() {
        radioBtnServer = (RadioButton) findViewById(R.id.radio_Server);
        radioBtnClient1 = (RadioButton) findViewById(R.id.radio_Client1);
        radioBtnClient2 = (RadioButton) findViewById(R.id.radio_Client2);
        btnFuncEnsure = (Button) findViewById(R.id.btn_FunctionEnsure);
        txtShowFunc = (TextView) findViewById(R.id.txt_ShowFunction);
        btnFuncHistory = (Button) findViewById(R.id.btn_FunctionHistory);
    }

    private void bindListener(){
        radioBtnClient1.setOnCheckedChangeListener(myRadioButtonCheck);
        radioBtnClient2.setOnCheckedChangeListener(myRadioButtonCheck);
        radioBtnServer.setOnCheckedChangeListener(myRadioButtonCheck);
        btnFuncEnsure.setOnClickListener(myButtonClick);
        btnFuncHistory.setOnClickListener(myButtonClick);
    }
    private void getPermission(){
        permissionsUtil = PermissionsUtil.with(this)
                .requestCode(0)
                .isDebug(true)
                .permissions(PermissionsUtil.Permission.Microphone.RECORD_AUDIO)
                .request();
        permissionsUtil = PermissionsUtil.with(this)
                .requestCode(0)
                .isDebug(true)
                .permissions(PermissionsUtil.Permission.Storage.WRITE_EXTERNAL_STORAGE)
                .request();
        permissionsUtil = PermissionsUtil.with(this)
                .requestCode(0)
                .isDebug(true)
                .permissions(PermissionsUtil.Permission.Storage.READ_EXTERNAL_STORAGE)
                .request();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionsGranted(int requestCode, String... permission) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, String... permission) {

    }
}
