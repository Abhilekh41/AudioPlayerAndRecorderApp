package com.abhilekh.audiorecorderandplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    Button record;
    Button play;
    Button stop;
    Button stopPlaying;

    Thread runner;

    TextView soundMeasure;


    private static  double EMA = 0.0;
    private static final double EMA_FILTER = 0.6;

    public static final int RequestPermissionCode = 1;

    String audioSavePathInDevice = null;

    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;

    private double getSoundInDB(double amplitude)
    {
        return 20*Math.log10(getAmplitude()/amplitude);
    }

    private double getAmplitude()
    {
        if(mediaRecorder!=null)
        {
            return  mediaRecorder.getMaxAmplitude();
        }
        else
            return 1;
    }

    private double getAmplitudeEMA()
    {
        double amp = getAmplitude();
        EMA = EMA_FILTER * amp + (1-EMA_FILTER)*EMA;
        return (int) EMA;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        record = findViewById(R.id.RecordButton);
        play = findViewById(R.id.PlayButton);
        stop = findViewById(R.id.StopButton);
        stopPlaying = findViewById(R.id.StopPLayingButton);
        soundMeasure = findViewById(R.id.soundMeasure);

        play.setEnabled(false);
        stop.setEnabled(false);
        stopPlaying.setEnabled(false);

        if(runner == null)
        {
            runner = new Thread()
            {
                public void run()
                {
                    while(runner!=null)
                    {
                        try {
                            Thread.sleep(200);
                            Log.i(TAG,"run : ticker");
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                soundMeasure.setText(Double.toString(getSoundInDB(1)) + "DB");
                            }
                        });
                    }
                }
            };
            runner.start();
            Log.i(TAG, "onCreate: Runner has started");

        }

        record.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    audioSavePathInDevice = Environment.getExternalStorageDirectory().getPath()+"/"+ "AudioRecord.3gpp";
                    mediaRecorderReady();

                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();

                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    record.setEnabled(false);
                    play.setEnabled(false);
                    stop.setEnabled(true);
                    stopPlaying.setEnabled(false);

                }
                else
                {
                    requestPermission();
                }


            }
        });

        stop.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                    mediaRecorder.stop();
                    stop.setEnabled(false);
                    play.setEnabled(true);
                    stopPlaying.setEnabled(false);
                    record.setEnabled(true);

                    Toast.makeText(MainActivity.this,"Recording Complete",Toast.LENGTH_SHORT).show();

            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop.setEnabled(false);
                play.setEnabled(false);
                stopPlaying.setEnabled(true);
                record.setEnabled(false);

                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(audioSavePathInDevice);
                    mediaPlayer.prepare();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                mediaPlayer.start();
                Toast.makeText(MainActivity.this,"Started Playing", Toast.LENGTH_SHORT).show();
            }
        });

        stopPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop.setEnabled(false);
                record.setEnabled(true);
                stopPlaying.setEnabled(false);
                play.setEnabled(true);

                if(mediaPlayer!=null)
                {
                    mediaPlayer.stop();
                }
            }
        });
    }

    private boolean checkPermission() {
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int recordAudioPermission = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);

        return writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED
                && recordAudioPermission == PackageManager.PERMISSION_GRANTED;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean reccordPermssion = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (storagePermission && reccordPermssion) {
                        Toast.makeText(this, "Permission Is Granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Request Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]
                        {
                                WRITE_EXTERNAL_STORAGE,
                                RECORD_AUDIO
                        }, RequestPermissionCode);
    }

    private void mediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(audioSavePathInDevice);
    }
}
