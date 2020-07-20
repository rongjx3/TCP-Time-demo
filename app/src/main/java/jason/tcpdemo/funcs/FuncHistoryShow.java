package jason.tcpdemo.funcs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;

import jason.tcpdemo.R;

public class FuncHistoryShow extends Activity {
    private TextView textHistoryShow;
    private FileReader fileReader;
    private static final String TAG = "FuncHistoryShow";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_show);
        textHistoryShow = findViewById(R.id.txt_HistoryShow);
        textHistoryShow.setMovementMethod(ScrollingMovementMethod.getInstance());
        showContent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            fileReader.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    void showContent(){
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        char[] buffer = new char[1024];
        try{
            fileReader = new FileReader(path);
            int c;
            while ((c = fileReader.read(buffer)) != -1){
                textHistoryShow.append(new String(buffer, 0, buffer.length));
//                textHistoryShow.append("\n");
                buffer = new char[1024];
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        Log.e(TAG, "buffer length"+ buffer.length);
//        textHistoryShow.setText(buffer, 0, buffer.length-1);

    }
}
