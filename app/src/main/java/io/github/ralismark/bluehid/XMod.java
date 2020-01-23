package io.github.ralismark.bluehid;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XMod implements IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private static final String TAG = "io.github.ralismark.bluehid.XMod";

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.bluetooth")) {
            return;
        }

        resparam.res.setReplacement("com.android.bluetooth",
                "bool", "profile_supported_hidd", true);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.bluetooth")) {
            return;
        }

        Class<?> adapterApp = XposedHelpers.findClass(
                "com.android.bluetooth.btservice.AdapterApp", lpparam.classLoader);

        XposedBridge.hookAllMethods(adapterApp, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application app = (Application) param.thisObject;

                PackageManager pm = app.getPackageManager();

                ComponentName hidServiceName = new ComponentName(
                        "com.android.bluetooth", "com.android.bluetooth.hid.HidDevService");
                if (pm.getComponentEnabledSetting(hidServiceName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(hidServiceName,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    XposedBridge.log("Forced enable com.android.bluetooth.hid.HidDevService");
                }
            }
        });
    }
}
