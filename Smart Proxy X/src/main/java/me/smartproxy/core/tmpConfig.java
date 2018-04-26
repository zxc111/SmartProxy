package me.smartproxy.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.ContentProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.smartproxy.ui.MainActivity;

import static android.app.PendingIntent.getActivity;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by gho on 17-11-4.
 */

public class tmpConfig {
    public static String UserName, Password, remoteIp, remotePort;
    public static boolean bypass=true;

    public static final String UserKey = "UserNameKey";
    public static final String PasswordKey = "PasswordKey";
    public static final String IpKey = "IpKey";
    public static final String PortKey = "PortKey";

    public static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";

    public static String nghttpxCmd = "";

    public static String exe_path = "/data/data/me.smartproxy/";
    public File exe_file;

    public String getConfig(Context context){
        String username = readConfigKey(UserKey, context),
                pwd = readConfigKey(PasswordKey, context),
                ip = readConfigKey(IpKey, context),
                port = readConfigKey(PortKey, context),
                user_pwd = "";

        if (!username.equals("")){
            user_pwd = String.format("%s:%s@", username, pwd);
        }
        return String.format("http://%s%s:%s", user_pwd, ip, port);

    }

    public String readConfigKey(String Key, Context context) {
        SharedPreferences preferences = context.getSharedPreferences("SmartProxy", MODE_PRIVATE);
        return preferences.getString(Key, "");
    }

    // TODO开启nghttpx放到这里
    public static void CopyAndStart(Context context){
        try {
            String filePath =  tmpConfig.exe_path+"nghttpx";
            File f=new File(filePath);

            if(!f.exists())
            {
                copyDataToSD(filePath, "nghttpx", context);
            }
            // copyBigDataToSD(filePath, "nghttpx");
            File exe_file = new File(filePath);
            exe_file.setExecutable(true, true);
            nghttpxCmd = tmpConfig.exe_path+"nghttpx";

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        startNghttpx();
        //setBypass(); // 绕过国内
    }

    public static void startNghttpx(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    execCmd();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private static void execCmd() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        String backendConfig = String.format("--backend=%s,%s;;tls;proto=h2", tmpConfig.remoteIp, tmpConfig.remotePort);

        Process process = runtime.exec(new String[]{
                tmpConfig.nghttpxCmd,
                "-k",
                "--frontend=0.0.0.0,9000;no-tls",
                backendConfig,
                "--http2-proxy",
                "--workers=4",
        });

        try {
            process.waitFor();
            InputStream error = process.getErrorStream();
            String err_msg = "";

            for (int i = 0; i < 200; i++) {
                err_msg += (char) error.read();
            }
            System.out.println(err_msg);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private static void copyDataToSD(String strOutFileName, String assertFileName, Context context) throws IOException
    {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = context.getAssets().open(assertFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while(length > 0)
        {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
    }
}
