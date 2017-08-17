package com.example.su.myftpdemo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button mButton, btn_stop;
    private EditText mEditText;
    private static final String TAG = "FtpServerService";
    private static String hostip = ""; // 本机IP
    private static final int PORT = 8090;
    // sd卡目录
    @SuppressLint("SdCardPath")
    private static final String dirname = "/mnt/sdcard/ftp";
    // ftp服务器配置文件路径
    private static final String filename = dirname + "/users.properties";
    private FtpServer mFtpServer = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x0001:
                    Toast.makeText(MainActivity.this, "开启了FTP服务器  ip = " + hostip, Toast.LENGTH_SHORT).show();
                    break;
                case 0x0002:
                    Toast.makeText(MainActivity.this, "关闭了FTP服务器  ip = " + hostip, Toast.LENGTH_SHORT).show();
                    break;
                case 0x0003:
                    Toast.makeText(MainActivity.this, "当前FTP服务已开启 ip=" + hostip, Toast.LENGTH_SHORT).show();
                    break;
                case 0x0004:
                    Toast.makeText(MainActivity.this, "当前FTP服务已关闭 ip=" + hostip, Toast.LENGTH_SHORT).show();
                    break;

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.btn);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        mEditText = (EditText) findViewById(R.id.et);
        //创建服务器配置文件
        try {
            creatDirsFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //hostip = mEditText.getText().toString().trim();
                        hostip = getLocalIpAddress();
                        if (mFtpServer == null) {
                            startFtpServer(hostip);
                        } else {
                            mHandler.sendEmptyMessage(0x0003);
                        }
                    }
                }).start();

            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stopFtpServer();
                    }
                }).start();
            }
        });
    }

    /**
     * 创建服务器配置文件
     */
    private void creatDirsFiles() throws IOException {
        File dir = new File(dirname);
        if (!dir.exists()) {
            dir.mkdir();
        }
        FileOutputStream fos = null;
        String tmp = getString(R.string.users);
        File sourceFile = new File(dirname + "/users.properties");
        fos = new FileOutputStream(sourceFile);
        fos.write(tmp.getBytes());
        if (fos != null) {
            fos.close();
        }
    }

    /**
     * 开启FTP服务器
     *
     * @param hostip 本机ip
     */
    private void startFtpServer(String hostip) {
        FtpServerFactory serverFactory = new FtpServerFactory();
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File files = new File(filename);
        //设置配置文件
        userManagerFactory.setFile(files);
        serverFactory.setUserManager(userManagerFactory.createUserManager());
        // 设置监听IP和端口号
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(PORT);
        factory.setServerAddress(hostip);
        // start the server
        mFtpServer = serverFactory.createServer();
        try {
            mFtpServer.start();
            mHandler.sendEmptyMessage(0x0001);
            Log.d(TAG, "开启了FTP服务器  ip = " + hostip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭FTP服务器
     */
    private void stopFtpServer() {
        if (mFtpServer != null) {
            mFtpServer.stop();
            mFtpServer = null;
            mHandler.sendEmptyMessage(0x0002);
            Log.d(TAG, "关闭了FTP服务器 ip = " + hostip);
        }else {
            mHandler.sendEmptyMessage(0x0004);
        }
    }

    /**
     * 获取本机ip
     */
    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf
                        .getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = Isipv4(sAddr);
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public void onStartServer(View view) {
        hostip = getLocalIpAddress();
        Log.d(TAG, "获取本机IP = " + hostip);
        startFtpServer(hostip);

    }

    public static boolean Isipv4(String ipv4) {
        if (ipv4 == null || ipv4.length() == 0) {
            return false;//字符串为空或者空串
        }
        String[] parts = ipv4.split("\\.");//因为java doc里已经说明, split的参数是reg, 即正则表达式, 如果用"|"分割, 则需使用"\\|"
        if (parts.length != 4) {
            return false;//分割开的数组根本就不是4个数字
        }
        for (int i = 0; i < parts.length; i++) {
            try {
                int n = Integer.parseInt(parts[i]);
                if (n < 0 || n > 255) {
                    return false;//数字不在正确范围内
                }
            } catch (NumberFormatException e) {
                return false;//转换数字不正确
            }
        }
        return true;
    }
}
