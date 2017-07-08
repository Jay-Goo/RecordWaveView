package jaygoo.recordwaveview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;

import jaygoo.widget.rwv.Mp3WaveRecorder;
import jaygoo.widget.rwv.RecordWaveView;

public class MainActivity extends AppCompatActivity  {

    private RecordWaveView recordWaveView;
    private Mp3WaveRecorder mp3WaveRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordWaveView = (RecordWaveView) findViewById(R.id.recordWaveView);
        mp3WaveRecorder = (Mp3WaveRecorder) findViewById(R.id.mp3WaveRecorder);

        mp3WaveRecorder.setOnRecordListener(new Mp3WaveRecorder.OnRecordListener() {
            @Override
            public void onStart() {
                Toast.makeText(getApplicationContext(),"开始录音",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStop(File file) {
                Toast.makeText(getApplicationContext(),"录音文件："+file.getPath(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        recordWaveView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mp3WaveRecorder.clearCache();
    }
}
