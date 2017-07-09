package jaygoo.widget.rwv;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;

import com.czt.mp3recorder.MP3Recorder;

import java.io.File;
import java.io.IOException;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/7/8
 * 描    述:
 * 封装录音按钮，支持自定义Recorder，请参照此类实现即可
 * ================================================
 */
public class Mp3WaveRecorder extends RecordWaveView implements View.OnClickListener{

    private OnRecordListener mOnRecordListener;
    private MP3Recorder mMP3Recorder;
    private String filePath;
    //每10次绘制时间(16ms) 更新振幅
    private int cycleTimes = 0;

    public Mp3WaveRecorder(Context context) {
        this(context, null);
    }

    public Mp3WaveRecorder(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Mp3WaveRecorder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(this);
        filePath = Environment.getExternalStorageDirectory().getPath() +  "/waveRecorder.mp3";
        mMP3Recorder = new MP3Recorder(new File(filePath));
    }


    //每16ms调用一次
    @Override
    protected void refreshAmplitude() {
        super.refreshAmplitude();
        cycleTimes++;
        if (cycleTimes % 10 == 0) {
            cycleTimes = 0;
            setVolume(getVolumeDb());
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isRecording()) {
           startAnim();
        }
    }

    public int getVolume(){
        if (mMP3Recorder != null){
           return mMP3Recorder.getVolume();
        }
        return 0;
    }

    public int getVolumeDb(){
        if (mMP3Recorder != null){
            return mMP3Recorder.getVolumeDb();
        }
        return 0;
    }


    @Override
    public void onClick(View v) {
        if (isRunning()){
            stop();
        }else {
            start();
        }
    }

    public void start() {
        startAnim();

        if (mMP3Recorder != null && !mMP3Recorder.isRecording()){
            if (mOnRecordListener != null){
                mOnRecordListener.onStart();
            }
            try {
                mMP3Recorder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        stopAnim();
        if (mMP3Recorder != null && mMP3Recorder.isRecording()){
            mMP3Recorder.stop();
            if (mOnRecordListener != null){
                mOnRecordListener.onStop(new File(filePath));
            }
        }

    }

    public void clearCache(){
        File file = new File(filePath);
        if (file.exists()){
            file.delete();
        }
    }

    public boolean isRecording() {
        if (mMP3Recorder != null) {
            return mMP3Recorder.isRecording();
        }else {
            return false;
        }
    }

    public interface OnRecordListener{
        void onStart();
        void onStop(File file);
    }

    public void setOnRecordListener(OnRecordListener listener){
        mOnRecordListener = listener;
    }
}
