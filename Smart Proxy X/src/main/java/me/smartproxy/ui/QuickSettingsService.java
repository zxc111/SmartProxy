package me.smartproxy.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.TileService;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.widget.Switch;

import me.smartproxy.core.tmpConfig;
import me.smartproxy.R;
import me.smartproxy.core.LocalVpnService;

/**
 * Created by3/27.
 */

@TargetApi(Build.VERSION_CODES.N)
public class QuickSettingsService extends TileService{
    //当用户从Edit栏添加到快速设置中调用

    private final int STATE_OFF = 0;
    private final int STATE_ON = 1;
    private final String LOG_TAG = "QuickSettingService";
    private int toggleState = STATE_OFF;
    private static final Boolean isrunning = true;

    private Switch switchProxy;
    @Override
    public void onTileAdded() {
        Log.d(LOG_TAG, "onTileAdded");
    }
    //当用户从快速设置栏中移除的时候调用
    @Override
    public void onTileRemoved() {
        Log.d(LOG_TAG, "onTileRemoved");
    }
    // 点击的时候
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onClick() {
        Icon icon;

        if (toggleState == STATE_ON) {
            toggleState = STATE_OFF;
            icon =  Icon.createWithResource(getApplicationContext(), R.drawable.ic_launcher);
            getQsTile().setState(Tile.STATE_INACTIVE);// 更改成非活跃状态
            // switchProxy.setChecked(false);
            if (LocalVpnService.IsRunning) {
                LocalVpnService.IsRunning = false;
                LocalVpnService.Instance.disconnectVPN();
                // stopService(new Intent(this, LocalVpnService.class));
            }

        } else {
            toggleState = STATE_ON;
            // startService(new Intent(this, QuickSettingsService.class));
            icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_launcher);
            getQsTile().setState(Tile.STATE_ACTIVE);//更改成活跃状态

            tmpConfig.CopyAndStart(this);
            String configUrl = tmpConfig.getConfig(this);
            System.out.println(configUrl);
            LocalVpnService.ConfigUrl = configUrl;
            LocalVpnService.IsRunning = true;

            Intent intent = LocalVpnService.prepare(this);
            if (intent == null) {
                // startVPNService();
                startService(new Intent(this, LocalVpnService.class));
            } else {
                // startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
            }

        }

        getQsTile().setIcon(icon);//设置图标
        getQsTile().updateTile();//更新Tile
    }

    // 打开下拉菜单的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
    //在TleAdded之后会调用一次
    @Override
    public void onStartListening () {
        Log.d(LOG_TAG, "onStartListening");
    }
    // 关闭下拉菜单的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
    // 在onTileRemoved移除之前也会调用移除
    @Override
    public void onStopListening () {
        Log.d(LOG_TAG, "onStopListening");
    }

    /*public void setStateOn() {
        String  config = new tmpConfig().getConfig(this);
        System.out.println(config);
    }
    public void setStateOff() {

    }*/
    public void QSService() {
        QuickSettingsService instance = this;
    }
}
