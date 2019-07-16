package hts;
import android.util.Log;

import java.io.IOException;

/**
 * Android运行linux命令
 */
public class WifiCmd {
    private static final String TAG = "WIFICMD";
    private static final String WIFI_ON = "svc wifi enable";
    private static final String WIFI_OFF = "svc wifi disable";
    private Runtime runtime;

    private static volatile WifiCmd wifiCmd;

    private WifiCmd(Runtime runtime) {
        this.runtime = runtime;
    }

    public static WifiCmd getInstance(Runtime runtime) {
        if (wifiCmd == null) {
            synchronized (WifiCmd.class) {
                if (wifiCmd == null) {
                    wifiCmd = new WifiCmd(runtime);
                }
            }
        }
        return wifiCmd;
    }


    public void turnOn() {
        try {
            runtime.exec(WIFI_ON);
        } catch (IOException e) {
            Log.e(TAG, "{}", e);
            e.printStackTrace();
        }
    }

    public void turnOff() {
        try {
            runtime.exec(WIFI_OFF);
        } catch (IOException e) {
            Log.e(TAG, "{}", e);
            e.printStackTrace();
        }
    }
}

