package jason.tcpdemo.funcs;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeStampHelper {
    private long mydate = 0, otherdate = 0 ; //我的时间戳和对方的时间戳
    private long correct = 0; //时间戳补正值

    //从网络上获取时间戳（不好用）
    public long getMydate_online() {
        URLConnection uc= null;//生成连接对象
        try {
            URL url=new URL("http://www.bjtime.cn");//取得资源对象

            uc = url.openConnection();
            uc.connect(); //发出连接
        } catch (IOException e) {
            e.printStackTrace();
        }

        long ld=uc.getDate(); //取得网站日期时间
        Date date=new Date(ld); //转换为标准时间对象
        //分别取得时间中的小时，分钟和秒，并输出
        System.out.print(date.getHours()+"时"+date.getMinutes()+"分"+date.getSeconds()+"秒");

        mydate = ld + correct;
        return mydate;
    }

    //获取本地时间戳
    public long getMydate_local() {
        SimpleDateFormat dff = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dff.setTimeZone(TimeZone.getTimeZone("GMT+08"));
        String ee = dff.format(new Date());
        Date date = null;
        try {
            date = dff.parse(ee);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long ts = date.getTime();
        mydate = ts + correct;

        return mydate;
    }

    public void setOtherdate(long otherdate) {
        this.otherdate = otherdate;
    }

    public void setCorrect(long correct) {
        this.correct = correct;
    }

    public long getOtherdate() {
        return otherdate;
    }

    public long getMydate() {
        return mydate;
    }

    //计算差值
    public long calcul_diff() {
        return mydate - otherdate;
    }
}
