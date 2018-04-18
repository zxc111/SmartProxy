package me.smartproxy.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.ContentProvider;

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
}
