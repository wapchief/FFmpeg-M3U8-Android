package com.wapchief.ffmpeg_m3u8_android;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hdl.m3u8.M3U8DownloadTask;
import com.hdl.m3u8.bean.OnDownloadListener;
import com.hdl.m3u8.utils.NetSpeedUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import VideoHandle.EpEditor;
import VideoHandle.EpVideo;
import VideoHandle.OnEditorListener;
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFprobe;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

/**
 * @author wapchief
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String M3U8_URL = "http://res.pmit.cn/F3Video/hls/a5814959235386e4e7126573030c4d79/list.m3u8";
    private TextView mTextViewCmd, mTextViewProgress, mTextView;
    private Button mButton, mButton2,mButton3;
    private EditText mEditText;

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
        mButton2 = findViewById(R.id.bt2);
        mButton2.setOnClickListener(this);
        mEditText = findViewById(R.id.url_et);
        mTextViewCmd = findViewById(R.id.cmd_tv);
        mTextViewProgress = findViewById(R.id.progress_tv);
        mTextView = findViewById(R.id.tv);
        mEditText.setText(M3U8_URL);
        mButton3 = findViewById(R.id.bt3);
        mButton3.setOnClickListener(this);
    }

    Pattern mPattern = Pattern.compile("Duration: ([\\d\\w:]+)");
    long timeLength = 0;

    /**
     * 获取视频总时长
     */
    private long videoLengthTime(String result) {
        if (result.contains("Duration")) {
            Matcher matcher = mPattern.matcher(result);
            matcher.find();
            String tempTime = String.valueOf(matcher.group(0));
            tempTime = tempTime.substring(10);
            Log.e(TAG, "tempTime:" + tempTime);
            String[] arrayTime = tempTime.split(":");
            timeLength =
                    TimeUnit.HOURS.toSeconds(Long.parseLong(arrayTime[0]))
                            + TimeUnit.MINUTES.toSeconds(Long.parseLong(arrayTime[1]))
                            + Long.parseLong(arrayTime[2]);
            Log.e(TAG, "lengthTime:" + timeLength);
            return timeLength;
        }
        return timeLength;
    }


    Pattern pattern = Pattern.compile("time=([\\d\\w:]+)");
    long thisLength = 0;

    /**
     * 进度
     *
     * @param message 日志
     * @return
     */
    private long getProgress(String message) {

        if (message.contains("speed")) {
            Matcher matcher = pattern.matcher(message);
            matcher.find();
            String tempTime = String.valueOf(matcher.group(1));
//            LogUtils.e( "getProgress: tempTime " + tempTime);
            String[] arrayTime = tempTime.split(":");
            long currentTime =
                    TimeUnit.HOURS.toSeconds(Long.parseLong(arrayTime[0]))
                            + TimeUnit.MINUTES.toSeconds(Long.parseLong(arrayTime[1]))
                            + Long.parseLong(arrayTime[2]);
            long videoLengthInSec = timeLength;
            Log.e(TAG, videoLengthInSec + "============");
            thisLength = 100 * currentTime / videoLengthInSec;
            Log.e(TAG, "getProgressTime -> " + currentTime + "s % -> " + thisLength);

            return thisLength;
        }
        return thisLength;
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

    private void downloadFFmpegM3U8(String m3U8_URL) {
        cmd = String.format("-i %s -acodec %s -bsf:a aac_adtstoasc -vcodec %s %s ", m3U8_URL, "copy", "copy", path + System.currentTimeMillis() + ".mp4");
        /**正则去空*/
        String[] command = cmd.split(" ");
        try {
            FFmpeg.getInstance(this).execute(command, new FFcommandExecuteResponseHandler() {
                @Override
                public void onSuccess(String message) {
                    Log.e("onSuccess:", message);
                    mTextView.setText("onSuccess:\n" + message);

                }

                @Override
                public void onProgress(final String message) {
                    Log.i("onProgress:", message);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long pg = videoLengthTime(message);
                            if (pg > 0) {
//                                getProgress(message);
                                mTextViewProgress.setText("已完成：" + getProgress(message) + "%");
                            }

                        }
                    });
                    mTextView.setText(message);
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
//                                downloadFFmpegM3U8();
                                downloadContent--;
                            }
                            mTextViewProgress.setText("已完成：100" + "%");
                        }
                    }, 5000);
                }

            });
        } catch (FFmpegCommandAlreadyRunningException e) {
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
                    mTextView.setText("onSuccess\n" + message);
                }

                @Override
                public void onProgress(String message) {
                    mTextView.setText("onProgress\n" + message);

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
                downloadFFmpegM3U8(mEditText.getText().toString());
            } else {
                Toast.makeText(this, "请检查是否安装了FFmpeg", Toast.LENGTH_LONG).show();
            }
        }

        if (v.getId() == R.id.bt2) {
//            if (isFileExists(path + "down.m3u8")) {
//                startDownload(path + "down.m3u8");
//            } else {
                startDownload(mEditText.getText().toString());

//            }
        }

        if (v.getId() == R.id.bt3) {
//            epVideoM3u8();
            epVideoM3u8Cmd();
        }
    }

    private void epVideoM3u8Cmd() {
        EpEditor.execCmd(
                "-i https://preview.mypsy365.com/app_dev.php/hls/232/stream/sd/IWQrlWQun45sLBscH9lMyOLU8qJhFsZK.m3u8?1532756203 -acodec copy -bsf:a aac_adtstoasc -vcodec copy /storage/emulated/0/m3u8/download/video/275812421676F545E668AD594F966805.mp4",
                0, new OnEditorListener() {
                    @Override
                    public void onSuccess() {
                        Log.e(TAG,"onSuccess");
                    }

                    @Override
                    public void onFailure() {
                        Log.e(TAG,"onFailure");
                    }

                    @Override
                    public void onProgress(float v) {
                        Log.e(TAG, "onProgress:" + v);
                    }
                });
    }

    private void epVideoM3u8() {
        EpVideo epVideo = new EpVideo(mEditText.getText().toString());
        EpEditor.OutputOption outputOption = new EpEditor.OutputOption(path + System.currentTimeMillis() + ".mp4");
        outputOption.frameRate = 30;//输出视频帧率,默认30
        outputOption.bitRate = 10;//输出视频码率,默认10
        EpEditor.exec(epVideo, outputOption, new OnEditorListener() {
            @Override
            public void onSuccess() {
                Log.e("epVideo-------", "onSuccess");
                mTextView.setText("开始下载");
            }

            @Override
            public void onFailure() {
                Log.e("epVideo-------", "onSuccess");
                mTextView.setText("下载失败");
            }

            @Override
            public void onProgress(float progress) {
                //这里获取处理进度
                mTextViewProgress.setText("已完成" + progress + "%");
            }
        });
    }

    /**
     * 下载M3u8视频
     *
     * @param mediaUrls
     */
    M3U8DownloadTask downloadTask = new M3U8DownloadTask("1001");
    long lastLength = 0L;

    private void startDownload(final String mediaUrls) {
        downloadTask.setSaveFilePath(path + System.currentTimeMillis() + ".ts");
        downloadTask.download(mediaUrls, new OnDownloadListener() {
            @Override
            public void onDownloading(long itemFileSize, int totalTs, int curTs) {
                Log.e(TAG, "onDownloading:" + itemFileSize + "," + totalTs + "," + curTs);
                mTextViewProgress.setText("已完成" + (curTs/totalTs) + "%");
            }

            //下载完成
            @Override
            public void onSuccess() {
                mTextViewProgress.setText("已完成：100%");
                Log.e(TAG, "onSuccess");
            }

            //下载进度回调
            @Override
            public void onProgress(final long curLength) {
                Log.e("onProgress", curLength + "\n" + lastLength);
                if (curLength - lastLength > 0) {
                    //下载速度
                    final String speed = NetSpeedUtils.getInstance().displayFileSize(curLength - lastLength) + "/s";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(speed + "");
                            Log.e(TAG, "onProgress:" + speed);
                        }
                    });
                    lastLength = curLength;

                }
            }

            //开始下载
            @Override
            public void onStart() {
                Log.e(TAG, "onStart:");
            }

            //下载出错
            @Override
            public void onError(Throwable errorMsg) {
                Log.e(TAG, "onError:" + errorMsg.toString());

            }

        });
    }


    /**
     * Return whether the file exists.
     *
     * @param filePath The path of file.
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean isFileExists(final String filePath) {
        return isFileExists(getFileByPath(filePath).toString());
    }


    /**
     * Return the file by path.
     *
     * @param filePath The path of file.
     * @return the file
     */
    public static File getFileByPath(final String filePath) {
        return isSpace(filePath) ? null : new File(filePath);
    }

    private static boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
