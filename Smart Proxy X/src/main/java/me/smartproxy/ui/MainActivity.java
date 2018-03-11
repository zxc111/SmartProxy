package me.smartproxy.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import me.smartproxy.R;
import me.smartproxy.core.LocalVpnService;
import me.smartproxy.core.tmpConfig;

import java.io.File;
import java.util.Calendar;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements
        View.OnClickListener,
        OnCheckedChangeListener,
        LocalVpnService.onStatusChangedListener {

    private static String GL_HISTORY_LOGS;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";

    final String UserKey = "UserNameKey";
    final String PasswordKey = "PasswordKey";
    final String IpKey = "IpKey";
    final String PortKey = "PortKey";
    String nghttpxCmd = "";

    private String exe_path = "/data/data/me.smartproxy/";
    private File exe_file;

    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;

    private Switch switchProxy;
    private TextView textViewLog;
    private ScrollView scrollViewLog;
    private TextView textViewConfigUrl;
    private Calendar mCalendar;
    private CheckBox byPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollViewLog = (ScrollView) findViewById(R.id.scrollViewLog);
        textViewLog = (TextView) findViewById(R.id.textViewLog);

        textViewLog.setText(GL_HISTORY_LOGS);
        scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);

        mCalendar = Calendar.getInstance();
        LocalVpnService.addOnStatusChangedListener(this);

        byPass = (CheckBox) findViewById(R.id.bypassChina);

        final TextView UserNameField = (TextView) findViewById(R.id.UserName);
        final TextView PasswordField = (TextView) findViewById(R.id.Password);
        final TextView IpField = (TextView) findViewById(R.id.remoteIp);
        final TextView PortField = (TextView) findViewById(R.id.remotePort);

        String UserNameFromDB = readConfigKey(UserKey);
        final String PasswordDB = readConfigKey(PasswordKey);
        String IpDB = readConfigKey(IpKey);
        String PortDb = readConfigKey(PortKey);

        if (TextUtils.isEmpty(UserNameFromDB)) {
            UserNameField.setText("Please input username");
        } else {
            UserNameField.setText(UserNameFromDB);
            tmpConfig.UserName = UserNameFromDB;
        }
        if (TextUtils.isEmpty(PasswordDB)) {
            PasswordField.setText("Please input password");
        } else {
            PasswordField.setText(PasswordDB);
            tmpConfig.Password = PasswordDB;
        }
        if (TextUtils.isEmpty(IpDB)) {
            IpField.setText("Please input remote ip");
        } else {
            IpField.setText(IpDB);
            tmpConfig.remoteIp = IpDB;
        }
        if (TextUtils.isEmpty(PortDb)) {
            PortField.setText("Please input remote port");
        } else {
            PortField.setText(PortDb);
            tmpConfig.remotePort = PortDb;
        }

        Button button= (Button) findViewById(R.id.confirm);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String UserName = (String) UserNameField.getText().toString();
                setConfigKey(UserKey, UserName);
                tmpConfig.UserName = UserName;
                String Password = (String) PasswordField.getText().toString();
                setConfigKey(PasswordKey, Password);
                tmpConfig.Password = Password;
                String Ip = (String) IpField.getText().toString();
                setConfigKey(IpKey, Ip);
                tmpConfig.remoteIp = Ip;
                String Port = (String) PortField.getText().toString();
                setConfigKey(PortKey, Port);
                tmpConfig.remotePort = Port;

                try {
                    String filePath =  exe_path+"nghttpx";
                    File f=new File(filePath);

                    if(!f.exists())
                    {
                        copyBigDataToSD(filePath, "nghttpx");
                        exe_file = new File(filePath);
                        exe_file.setExecutable(true, true);
                    }

                    nghttpxCmd = exe_path+"nghttpx";
                    startNghttpx();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    String readConfigUrl() {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        return preferences.getString(CONFIG_URL_KEY, "");
    }

    void setConfigUrl(String configUrl) {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(CONFIG_URL_KEY, configUrl);
        editor.commit();
    }
    String readConfigKey(String Key) {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        return preferences.getString(Key, "");
    }
    void setConfigKey(String Key, String Value) {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(Key, Value);
        editor.commit();
    }

    String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            Log.e(TAG, "null package manager is impossible");
            return null;
        }

        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "package not found is impossible", e);
            return null;
        }
    }

    boolean isValidUrl(String url) {
        try {
            if (url == null || url.isEmpty())
                return false;

            if (url.startsWith("/")) {//file path
                File file = new File(url);
                if (!file.exists()) {
                    onLogReceived(String.format("File(%s) not exists.", url));
                    return false;
                }
                if (!file.canRead()) {
                    onLogReceived(String.format("File(%s) can't read.", url));
                    return false;
                }
            } else { //url
                Uri uri = Uri.parse(url);
                if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    return false;
                if (uri.getHost() == null)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        if (switchProxy.isChecked()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.config_url)
                .setItems(new CharSequence[]{
                        getString(R.string.config_url_scan),
                        getString(R.string.config_url_manual)
                }, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                scanForConfigUrl();
                                break;
                            case 1:
                                showConfigUrlInputDialog();
                                break;
                        }
                    }
                })
                .show();
    }

    private void scanForConfigUrl() {
        new IntentIntegrator(this)
                .setResultDisplayDuration(0)
                .setPrompt(getString(R.string.config_url_scan_hint))
                .initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    private void showConfigUrlInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(getString(R.string.config_url_hint));
        editText.setText(readConfigUrl());

        new AlertDialog.Builder(this)
                .setTitle(R.string.config_url)
                .setView(editText)
                .setPositiveButton(R.string.btn_ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editText.getText() == null) {
                            return;
                        }

                        String configUrl = editText.getText().toString().trim();

                        if (isValidUrl(configUrl)) {
                            setConfigUrl(configUrl);
                            textViewConfigUrl.setText(configUrl);
                        } else {
                            Toast.makeText(MainActivity.this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLogReceived(String logString) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        logString = String.format("[%1$02d:%2$02d:%3$02d] %4$s\n",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND),
                logString);

        System.out.println(logString);

        if (textViewLog.getLineCount() > 200) {
            textViewLog.setText("");
        }
        textViewLog.append(logString);
        scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);
        GL_HISTORY_LOGS = textViewLog.getText() == null ? "" : textViewLog.getText().toString();
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        switchProxy.setEnabled(true);
        switchProxy.setChecked(isRunning);
        onLogReceived(status);
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (LocalVpnService.IsRunning != isChecked) {
            switchProxy.setEnabled(false);

            if (isChecked) {
                setBypass(); // 绕过国内
                Intent intent = LocalVpnService.prepare(this);
                if (intent == null) {
                    startVPNService();
                } else {
                    startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
                }
            } else {
                LocalVpnService.IsRunning = false;
            }
        }
    }

    public void setBypass(){
        tmpConfig.bypass = byPass.isChecked();
    }

    private void startVPNService() {
        //String configUrl = readConfigUrl();
        String configUrl = getConfig();
        if (!isValidUrl(configUrl)) {
            Toast.makeText(this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
            switchProxy.post(new Runnable() {
                @Override
                public void run() {
                    switchProxy.setChecked(false);
                    switchProxy.setEnabled(true);
                }
            });
            return;
        }

        textViewLog.setText("");
        GL_HISTORY_LOGS = null;
        onLogReceived("starting...");
        LocalVpnService.ConfigUrl = configUrl;
        startService(new Intent(this, LocalVpnService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
            } else {
                switchProxy.setChecked(false);
                switchProxy.setEnabled(true);
                onLogReceived("canceled.");
            }
            return;
        }

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String configUrl = scanResult.getContents();
            if (isValidUrl(configUrl)) {
                setConfigUrl(configUrl);
                textViewConfigUrl.setText(configUrl);
            } else {
                Toast.makeText(MainActivity.this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }
    protected String getConfig(){
        String username = readConfigKey(UserKey),
                pwd = readConfigKey(PasswordKey),
                ip = readConfigKey(IpKey),
                port = readConfigKey(PortKey),
                user_pwd = "";


        if (!username.equals("")){
            user_pwd = String.format("%s:%s@", username, pwd);
        }
        return String.format("http://%s%s:%s", user_pwd, ip, port);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_item_switch);
        if (menuItem == null) {
            return false;
        }

        switchProxy = (Switch) menuItem.getActionView();
        if (switchProxy == null) {
            return false;
        }

        switchProxy.setChecked(LocalVpnService.IsRunning);
        switchProxy.setOnCheckedChangeListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_about:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name) + getVersionName())
                        .setMessage(R.string.about_info)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_more, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://smartproxy.me")));
                            }
                        })
                        .show();

                return true;
            case R.id.menu_item_exit:
                if (!LocalVpnService.IsRunning) {
                    finish();
                    return true;
                }

                new AlertDialog.Builder(this)
                        .setTitle(R.string.menu_item_exit)
                        .setMessage(R.string.exit_confirm_info)
                        .setPositiveButton(R.string.btn_ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LocalVpnService.IsRunning = false;
                                LocalVpnService.Instance.disconnectVPN();
                                stopService(new Intent(MainActivity.this, LocalVpnService.class));
                                System.runFinalization();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        LocalVpnService.removeOnStatusChangedListener(this);
        super.onDestroy();
    }

    private void copyBigDataToSD(String strOutFileName, String assertFileName) throws IOException
    {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = this.getAssets().open(assertFileName);
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

    private void execCmd() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        String backendConfig = String.format("--backend=%s,%s;;tls;proto=h2", tmpConfig.remoteIp, tmpConfig.remotePort);
        Process process = runtime.exec(new String[]{
                nghttpxCmd,
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

    public void startNghttpx(){
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

}
