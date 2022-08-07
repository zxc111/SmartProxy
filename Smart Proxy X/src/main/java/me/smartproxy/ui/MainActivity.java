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

import me.smartproxy.R;
import me.smartproxy.core.LocalVpnService;
import me.smartproxy.core.TmpConfig;

import java.util.Calendar;

public class MainActivity extends Activity implements
        View.OnClickListener,
        OnCheckedChangeListener,
        LocalVpnService.onStatusChangedListener {

    private static String GL_HISTORY_LOGS;

    private static final String TAG = MainActivity.class.getSimpleName();

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

//        TmpConfig.bypass = byPass.isChecked();

        final TextView UserNameField = (TextView) findViewById(R.id.UserName);
        final TextView PasswordField = (TextView) findViewById(R.id.Password);
        final TextView IpField = (TextView) findViewById(R.id.remoteIp);
        final TextView PortField = (TextView) findViewById(R.id.remotePort);

        String UserNameFromDB = readConfigKey(TmpConfig.UserKey);
        String PasswordDB = readConfigKey(TmpConfig.PasswordKey);
        String IpDB = readConfigKey(TmpConfig.IpKey);
        String PortDB = readConfigKey(TmpConfig.PortKey);
        String ByPassDB = readConfigKey(TmpConfig.ByPassKey);


        if (TextUtils.isEmpty(UserNameFromDB) == false) {
            UserNameField.setText(UserNameFromDB);
            TmpConfig.UserName = UserNameFromDB;
        }
        if (TextUtils.isEmpty(PasswordDB) == false) {
            PasswordField.setText(PasswordDB);
            TmpConfig.Password = PasswordDB;
        }
        if (TextUtils.isEmpty(IpDB)) {
            IpField.setText("remote_ip");
        } else {
            IpField.setText(IpDB);
            TmpConfig.remoteIp = IpDB;
        }
        if (TextUtils.isEmpty(PortDB)) {
            PortField.setText("remote_port");
        } else {
            PortField.setText(PortDB);
            TmpConfig.remotePort = PortDB;
        }

        if (ByPassDB.equals("true")) {
            TmpConfig.bypass = true;
        } else {
            TmpConfig.bypass = false;
        }
        byPass.setChecked(TmpConfig.bypass);


        Button button = (Button) findViewById(R.id.confirm);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String UserName = (String) UserNameField.getText().toString();
                setConfigKey(TmpConfig.UserKey, UserName);
                TmpConfig.UserName = UserName;
                String Password = (String) PasswordField.getText().toString();
                setConfigKey(TmpConfig.PasswordKey, Password);
                TmpConfig.Password = Password;
                String Ip = (String) IpField.getText().toString();
                setConfigKey(TmpConfig.IpKey, Ip);
                TmpConfig.remoteIp = Ip;
                String Port = (String) PortField.getText().toString();
                setConfigKey(TmpConfig.PortKey, Port);
                TmpConfig.remotePort = Port;
                boolean bypass = (boolean) byPass.isChecked();

                if (bypass) {
                    setConfigKey(TmpConfig.ByPassKey, "true");
                } else {
                    setConfigKey(TmpConfig.ByPassKey, "false");
                }
                TmpConfig.bypass = bypass;
                TmpConfig.remotePort = Port;
            }
        });
    }

    String readConfigUrl() {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        return preferences.getString(TmpConfig.CONFIG_URL_KEY, "");
    }

    void setConfigUrl(String configUrl) {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(TmpConfig.CONFIG_URL_KEY, configUrl);
        editor.commit();
    }

    String readConfigKey(String Key) {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        return preferences.getString(Key, "");
    }

    public void setConfigKey(String Key, String Value) {
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
            // TODO clean this
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
                                break;
                            case 1:
                                showConfigUrlInputDialog();
                                break;
                        }
                    }
                })
                .show();
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
                TmpConfig.CopyAndStart(this);
                startVpn();
            } else {
                LocalVpnService.IsRunning = false;
            }
        }
    }

    public void startVpn() {
        Intent intent = LocalVpnService.prepare(this);
        if (intent == null) {
            startVPNService();
        } else {
            startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
        }
    }

    private void startVPNService() {
        String configUrl = TmpConfig.getConfig(this);
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

        super.onActivityResult(requestCode, resultCode, intent);
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
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zxc111/SmartProxy")));
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

}
