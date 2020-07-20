package jason.tcpdemo.funcs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import jason.tcpdemo.MyApp;
import jason.tcpdemo.R;

public class FuncHistory extends Activity {
    private View historyList;
    private ListView historyListView;
    private final String HISTORY_PATH = MyApp.mainActivity.getExternalFilesDir(null).toString()+"/HistoryLog";
    private File historyLogDir = new File(HISTORY_PATH);
    private File[] fileList = historyLogDir.listFiles();
    private List<HashMap<String, Object>> fileInfoList = new ArrayList<HashMap<String, Object>>();
    private SimpleAdapter simpleAdapter = null;
    private static final String TAG = "FuncHistory";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_list);
        initLayout();
    }

    void initLayout(){
//        historyList = LayoutInflater.from(MyApp.mainActivity).inflate(R.layout.history_list, null);
        historyListView = findViewById(R.id.history_listview);
        int seq = 0;
        for(File f : fileList){
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("seq", ++seq);
            hashMap.put("name", f.getName());
            fileInfoList.add(hashMap);
            Log.e(TAG, "seq:"+seq+" name:"+f.getName());
        }
        simpleAdapter = new SimpleAdapter(this,
                fileInfoList,
                R.layout.history_overview,
                new String[]{"seq", "name"},
                new int[]{R.id.spec_item_seq, R.id.spec_item_name});
        historyListView.setAdapter(simpleAdapter);
        simpleAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(historyListView);
        historyListView.setOnItemClickListener(new myItemClickListener());

    }
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) { // pre-condition
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView); // listItem.measure(0, 0);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    private class myItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String path = fileList[position].getPath();
            Log.e(TAG, "path:"+path);
            Intent intent = new Intent();
            intent.setClass(FuncHistory.this, FuncHistoryShow.class);
            intent.putExtra("path", path);
            startActivity(intent);
        }
    }

    private Intent getWordFileIntent( String param, boolean paramBoolean ){
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (paramBoolean)
        {
//            Uri uri1 = Uri.parse(param );
            Uri uri = Uri.parse(param ).buildUpon().encodedAuthority("com.ktls.fileinfo.provider").scheme("content").encodedPath(param ).build();
            intent.setDataAndType(uri, "text/html");
        }
        else
        {
            Uri uri2 = Uri.fromFile(new File(param ));
            intent.setDataAndType(uri2, "text/plain");
        }
        return intent;
    }
}
