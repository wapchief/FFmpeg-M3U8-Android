package com.wapchief.ffmpeg_m3u8_android;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFprobe;
import nl.bravobit.ffmpeg.FFtask;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import nl.bravobit.ffmpeg.exceptions.FFprobeCommandAlreadyRunningException;

/**
 * @author wapchief
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String M3U8_URL = "http://res.pmit.cn/F3Video/hls/a5814959235386e4e7126573030c4d79/list.m3u8";
    private TextView mTextViewCmd, mTextViewProgress;
    private Button mButton;

    FFmpeg mFFmpeg;
    FFprobe mFFprobe;
    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        mButton = findViewById(R.id.bt);
        mButton.setOnClickListener(this);
        mTextViewCmd = findViewById(R.id.cmd_tv);
        mTextViewProgress = findViewById(R.id.progress_tv);
    }

    private void initData() {
        mFFmpeg = FFmpeg.getInstance(this);
        mFFprobe = FFprobe.getInstance(this);
        mTextViewCmd.setText(cmd);
        initFFmpegVersion();
    }

    /**
     * 设置视频储存路径
     */
    String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "/m3u8/download/video/";
    String cmd;
    /**
     * 下载
     */
    private Handler handler = new Handler();
    private void downloadFFmpegM3U8() {
        cmd = String.format("-i %s -acodec %s -bsf:a aac_adtstoasc -vcodec %s %s", M3U8_URL, "copy", "copy", path + System.currentTimeMillis() + ".mp4");
        /**正则去空*/
        String[] command = cmd.split(" ");
        try {
            final FFtask fFtask=FFmpeg.getInstance(this).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    Log.e("onSuccess:", message);
                    mTextViewProgress.setText("onSuccess:\n" + message);

                }

                @Override
                public void onProgress(String message) {
                    Log.i("onProgress:", message);
                    mTextViewProgress.setText(message);
                }

                @Override
                public void onFailure(String message) {
                    Log.e("onFailure:", message);

                }

                @Override
                public void onStart() {
                    Log.e("onStart:", "");

                }

                @Override
                public void onFinish() {
                    Log.e("onFinish:", "");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (downloadContent > 0) {
                                downloadFFmpegM3U8();
                                downloadContent--;
                            }
                        }
                    }, 5000);
                }

            });
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("ExampleActivity", "STOPPING THE RENDERING!");
                    fFtask.sendQuitSignal();
                }
            }, 8000);
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private void downloadFFprobeM3U8() {
        /**设置视频储存路径*/
        String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "/m3u8/download/video/";
        File dir = new File(path);
        if (!dir.exists()) {
            //判断路径是否存在，如果不存在则创建
            dir.mkdirs();
        }
//    String cmd = String.format("-i %s -acodec %s -bsf:a aac_adtstoasc -vcodec %s %s", M3U8_URL, "copy", "copy", path +System.currentTimeMillis()+".mp4");
        String cmd = "ffmpeg -version";
        mTextViewCmd.setText(cmd);

        /**正则去空*/
        String[] command = cmd.split(" ");
        try {
            mFFprobe.execute(command, new FFcommandExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {

                }

                @Override
                public void onProgress(String message) {
                    mTextViewProgress.setText(message);

                }

                @Override
                public void onFailure(String message) {

                }

                @Override
                public void onStart() {

                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFprobeCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取FFmpeg版本信息
     */
    private void initFFmpegVersion() {
        try {
            FFmpeg.getInstance(this).execute(new String[]{"-version"}, new ExecuteBinaryResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    mTextViewProgress.setText("onSuccess\n" + message);
                }

                @Override
                public void onProgress(String message) {
                    mTextViewProgress.setText("onProgress\n" + message);

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    int downloadContent = 2;
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt) {
            if (FFmpeg.getInstance(this).isSupported()) {
                downloadContent = 2;
                    downloadFFmpegM3U8();
            } else {
                Toast.makeText(this, "请检查是否安装了FFmpeg", Toast.LENGTH_LONG).show();
            }
        }
    }
}
