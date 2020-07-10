package jason.tcpdemo.coms;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

public class AudioHelper {
    private static final String TAG = "AudioHelper";
    private AudioRecord audioRecord;
    private int bufferSize;
    private static final int DOWN_SAMPLE_RATE = 8000;
    private boolean run_status = false;

    public AudioHelper(){
        bufferSize = AudioRecord.getMinBufferSize(DOWN_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                DOWN_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

    }

    public void startRecord(){
        audioRecord.startRecording();
        run_status = true;
    }

    public void stopRecord(){
        audioRecord.stop();
        run_status = false;
    }

    public double getVolume(){
        if(run_status){
            short[] buffer = new short[bufferSize];
            audioRecord.read(buffer, 0, bufferSize);
            long sum = 0;
            for (short b : buffer) {
                sum += Math.abs(b);
            }
            int length = buffer.length;
            sum = sum / (length);
            double db = 20 * Math.log10(sum);
            return db;
        } else {
            Log.d(TAG, "Did not start recording!");
            return 0;
        }
    }




}
