package com.panghui.adhoctest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

import hts.IpMaker;
import hts.WifiCmd;
import hts.WifiConfigurationNew;
import hts.WifiManagerNew;

public class MainActivity extends AppCompatActivity {
    public static final int RECEIVEMSG = 1;
    private static final String TAG = "MainActivity";

    WifiManager mWifiManager;
    WifiManagerNew mWifiManagerNew;
    String ip;

    ListView mListView;
    EditText meditText;
    Button mbutton;

    String data[] = {"Apple","Banana","Orange","Watermelon","pear","peach","kiwi"};
    ArrayAdapter<String> adapter = null;

    volatile int wifiState = 0;
    volatile int connectState = 0;

    /**
     * 是否持续发送消息
     */
    boolean isSend = true;

    ListenThread mylistenThread;
    /**
     * 每次侦听的时长
     */
    int listenDuration = 15000;
    /**
     * 每次休眠的时长
     */
    int sleepDuration = 5000;

    //long currentTime;
    double ratio;
    //int numberofPacket;
    int OnOffcount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mListView = findViewById(R.id.listView);
        meditText = findViewById(R.id.input_text);
        mbutton = findViewById(R.id.send);

        ip = IpMaker.getRandomIp();
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiManagerNew = new WifiManagerNew(mWifiManager);
        configureAdhocNetwork(false);
        data[0]=ip;
        String OnOffStr = "On: "+ listenDuration+"ms "+"Off: "+sleepDuration+"ms";
        data[3]=OnOffStr;
        adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1,data);
        mListView.setAdapter(adapter);

        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        //发送方的接收器
        //registerReceiver(mwifiBroadcastReceiver_send, filter);
        //new SenderWifiController().start();

        //接收方的接收器
        registerReceiver(mwifiBroadcastReceiver_receive,filter);
        new WifiController().start();

        // 打开 Wifi
        enableWifi();
    }



    @Override
    protected void onStart() {
        super.onStart();

        //enableWifi();
    }

    int receiveCount =0;
    /**
     * UDP包监听线程*/
    public volatile boolean Listenexit = false;
    private class ListenThread extends Thread{
        @Override
        public void run() {
            try{
                int receive_port = 23000;
                DatagramSocket rds = new DatagramSocket(receive_port);
                while(!Listenexit){
                    byte[] inBuf = new byte[1024];
                    DatagramPacket inPacket = new DatagramPacket(inBuf,inBuf.length);
                    InetAddress sourceIP = inPacket.getAddress();
                    rds.setSoTimeout(3000);//定时1秒关闭
                    Log.i(TAG,"go into receive");
                    rds.receive(inPacket);
                    Log.i(TAG,"go out receive");

                    String rdata = new String(inPacket.getData());
                    /*以上方法，可以正确解析出字符串
                     * 而 String rdata = inPacket.getData().toString()却无法正确解析出字符串*/
                    Log.i(TAG, "listenMessage: receive a message" + receiveCount);

                    //data[1]="s: "+rdata+" r: "+receiveCount;
                    data[1]=sourceIP.toString();
                    Log.i(TAG,"s: "+rdata+" r: "+receiveCount);//将收到的消息打印出来
                    adapter.notifyDataSetChanged();
                    //numberofPacket = Integer.parseInt(rdata);

                    Wifiexit = true;//放弃以点空比模式运行
                    handler.obtainMessage(RECEIVEMSG).sendToTarget();
                }

                rds.close();//关闭套接字

            }catch(SocketException e){
                Log.i(TAG,"TimeOut");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 配置 Adhoc 网络，check = true 为使能
     * @param check
     */
    public void configureAdhocNetwork(boolean check){
        try{
            WifiConfigurationNew wifiConfig = new WifiConfigurationNew();

            /*Set the SSID and security as normal */
            wifiConfig.SSID = "\"AdhocTest\"";
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            /* Use reflection until API is official */
            wifiConfig.setIsIBSS(true);
            wifiConfig.setFrequency(2412);

            /* Use reflection to configure static IP addresses*/
            wifiConfig.setIpAssignment("STATIC");
            //wifiConfig.setIpAddress(InetAddress.getByName(ip),16);
            wifiConfig.setIpAddress(InetAddress.getByName(ip),24);

            wifiConfig.setDNS(InetAddress.getByName("8.8.8.8"));

            /* Add , enable and save network as normal */
            int id = mWifiManager.addNetwork(wifiConfig);

            if (id < 0){
                Log.e(TAG, "configureAdhocNetwork: failed");
            }else{
                if(check == true){
                    mWifiManager.enableNetwork(id,true);
                    Log.i("WifiReceiver","AdHocNetwork Enabled");
                }else{
                    mWifiManager.disableNetwork(id);
                    mWifiManager.removeNetwork(id);
                    Log.i("WifiReceiver","Disabled");
                }

                mWifiManager.saveConfiguration();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /***
     * 发送方 On-Off 调节器
     */
    private class SenderWifiController extends Thread{
        public volatile boolean SWifiControllerexit = false;

        @Override
        public void run() {
            setName("SenderWifiControllerexit");
            Looper.prepare(); //暂时不明白这句话是什么意思
            while(!SWifiControllerexit){
                try{
                    enableWifi();
                    this.sleep(listenDuration);

                    disableWifi();
                    this.sleep(sleepDuration);

                }catch (InterruptedException e){
                    this.interrupt();
                }
            }
        }
    }

    public volatile boolean Wifiexit = false;
    private class WifiController extends Thread{


        @Override
        public void run() {
            setName("Wificontroller");
            Looper.prepare();
            while (!Wifiexit) {
                try {
                    enableWifi();
                    Listenexit=false;//启动接收线程
                    new ListenThread().start();
                    Log.i(TAG,"打开监听线程");

                    // 无论是否能够连接上，都会去休眠
                    Thread.sleep(listenDuration);

                    Listenexit=true;// 暂停接收线程
                    Log.i(TAG,"关闭监听线程");
                    disableWifi();

                    //休眠
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    /**
     * 侦听一段时间
     */
    //int receiveCount = 1;
    private void listenMessage() {
        DatagramSocket rds = null;
        try{
            int receive_port = 23000;
            rds = new DatagramSocket(receive_port);
            rds.setReuseAddress(true);
            byte[] inBuf = new byte[1024];
            DatagramPacket inPacket = new DatagramPacket(inBuf,inBuf.length);
            // 设置侦听时长
            rds.setSoTimeout(listenDuration);
            rds.receive(inPacket);
            Log.i(TAG, "listenMessage: receive a message" + receiveCount++);
            String rdata = inPacket.getData().toString();
            data[1]=rdata;
            handler.obtainMessage(RECEIVEMSG).sendToTarget();

            rds.close();
        } catch (SocketTimeoutException e){
            if (rds != null && !rds.isClosed()) {
                rds.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rds != null && !rds.isClosed()) {
                rds.close();
            }
        }
    }
    /**
     * 发送数据
     * @param str
     */
    int count = 1;
    public void sendMessage(String str){
        try {
            int send_port = 23000;
            DatagramSocket ds = new DatagramSocket();
            InetAddress bcAddr = InetAddress.getByName("192.168.1.255");
            DatagramPacket dp = new DatagramPacket(str.getBytes(), str.length(), bcAddr, send_port);
            //currentTime = System.currentTimeMillis();
                ds.send(dp);

            Log.i(TAG, "send a message " + count++);
            ds.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class SendMessageThread extends Thread {
        @Override
        public void run() {
            while (isSend) {
                //sendMessage(new Integer(count).toString());
                sendMessage("Hello");
                try {
                    Thread.sleep(100); //0.1秒发一个UDP包
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 广播接收器1：这个广播接收器接收到“Wifi 连接上” 信号后，开始按照一定的规则发送消息
    private BroadcastReceiver mwifiBroadcastReceiver = new BroadcastReceiver() {
        boolean init = false;
        private static final String TAG = "wifiReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            //wifi连接上与否
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {

                    // 设一个随机值，避免碰撞
                    Log.i(TAG, "connect to " + mWifiManager.getConnectionInfo().getSSID());

                    try {
                        Thread.sleep((int)(20 * Math.random()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    sendMessage("hello");
                    listenMessage();
                }
            }
            //wifi打开与否
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    Log.i(TAG, "关闭wifi ");
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    Log.i(TAG, "打开wifi ");
                    configureAdhocNetwork(true);
                }
            }
        }
    };


    // 广播接收器2：这个广播接收器接收到“Wifi 连接上” 信号后，规则为：一直发送消息
    private BroadcastReceiver mwifiBroadcastReceiver_send = new BroadcastReceiver() {
        boolean init = false;
        private static final String TAG = "wifiReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            data[1]="sender";
            //wifi连接上与否，连接上之后，开始持续发送消息
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {

                    WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo myWifiInfo = wifiManager.getConnectionInfo();
                    String macStr = myWifiInfo.getMacAddress();
                    data[4]=macStr;

                    List<ScanResult> wifiList;
                    if(wifiManager != null){
                        wifiList = wifiManager.getScanResults();
                        WifiInfo wifiinfo = wifiManager.getConnectionInfo();
                        if(wifiinfo != null && wifiinfo != null){
                            for(int i=0;i<wifiList.size();i++){
                                ScanResult result = wifiList.get(i);
                                if(wifiinfo.getBSSID().equals(result.BSSID)){
                                    data[5]=result.BSSID;
                                    adapter.notifyDataSetChanged();
                                }
                            }

                        }
                    }
                    Log.i(TAG,"connected to Ad hoc");
                    //data[1]="sending";
                    /*int times = 10;
                    while(times > 0) {
                        sendMessage("hello");
                        try {
                            Thread.sleep(500); //每隔500ms 发送一个消息
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        times--;
                    }*/
                    OnOffcount++;
                    data[2]="NumberOfConnect: "+OnOffcount;//Wifi 开合次数
                    adapter.notifyDataSetChanged();// 通知适配器作出了改变，更新显示

                    Toast.makeText(MainActivity.this,"connected "+OnOffcount,Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "connected "+OnOffcount);

                    isSend = true;
                    new SendMessageThread().start();//创建一直发送消息的线程
                    data[6]="attach";
                    adapter.notifyDataSetChanged();

                   /*while(isSend){
                       sendMessage(new Integer(count).toString());
                       try {
                           Thread.sleep(500);
                       } catch (InterruptedException e) {
                           e.printStackTrace();
                       }
                   }*/


                }
            }
            //wifi打开与否，收到打开Wifi信号后，在此处配置Ad hoc网络
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    Log.i(TAG, "关闭wifi ");
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    Log.i(TAG, "打开wifi ");
                    configureAdhocNetwork(true);
                }
                Toast.makeText(MainActivity.this,"disconnected",Toast.LENGTH_SHORT).show();

                data[4]="";
                data[5]="";
                data[6]="detach";
                adapter.notifyDataSetChanged();
                isSend = false;//取消发送消息的线程
            }
        }
    };

    // 广播接收器3：这个广播接收器接收到“Wifi 连接上” 信号后，规则为：一直接收消息
    private BroadcastReceiver mwifiBroadcastReceiver_receive = new BroadcastReceiver() {
        boolean init = false;
        private static final String TAG = "wifiReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            data[1]="receive";
            //wifi连接上与否，收到Wifi连接上信号后，打开监听线程
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {

                    WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo myWifiInfo = wifiManager.getConnectionInfo();
                    String macStr = myWifiInfo.getMacAddress();
                    data[4]=macStr;

                    List<ScanResult> wifiList;
                    if(wifiManager != null){
                        wifiList = wifiManager.getScanResults();
                        WifiInfo wifiinfo = wifiManager.getConnectionInfo();
                        if(wifiinfo != null && wifiinfo != null){
                            for(int i=0;i<wifiList.size();i++){
                                ScanResult result = wifiList.get(i);
                                if(wifiinfo.getBSSID().equals(result.BSSID)){
                                    data[5]=result.BSSID;
                                    adapter.notifyDataSetChanged();
                                }
                            }

                        }
                    }

                    //mylistenThread.start();
                    Listenexit = false;
                    new ListenThread().start();
                    data[6]="attach";

                    OnOffcount++;
                    data[2]="NumberOfConnect: "+OnOffcount;//Wifi 开合次数
                    adapter.notifyDataSetChanged();// 通知适配器作出了改变，更新显示

                    Toast.makeText(MainActivity.this,"connected "+OnOffcount,Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "connected "+OnOffcount);

                }
            }
            //wifi打开与否
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    Log.i(TAG, "关闭wifi ");
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    Log.i(TAG, "打开wifi ");
                    configureAdhocNetwork(true);
                }

                Listenexit=true;
                data[6]="detach";
                data[4]="";
                data[5]="";
                adapter.notifyDataSetChanged();//失去连接，就将邻结点mac名称关闭
            }
        }
    };
    /**
     * 开启wifi
     */
    private void enableWifi() {
        mWifiManager.setWifiEnabled(true);
    }

    private void disableWifi() {
        mWifiManager.setWifiEnabled(false);
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case RECEIVEMSG:
                    mListView.setAdapter(adapter);
                    Toast.makeText(MainActivity.this,"Hello",Toast.LENGTH_SHORT).show();
                    receiveCount++; //真正收到消息才开始计数

                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableWifi();
    }
}
