package jason.tcpdemo.history;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jason.tcpdemo.R;

public class FuncHistory extends Activity {
    private View historyOverview;
    private LayoutInflater layoutInflater = getLayoutInflater();
    private ListView historyListView;
    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            +"/"+"tcpTimeDemo/"+"historyLog";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        historyOverview = layoutInflater.inflate(R.layout.history_overview, null);
        historyListView = historyOverview.findViewById(R.id.history_listview);
    }
}
