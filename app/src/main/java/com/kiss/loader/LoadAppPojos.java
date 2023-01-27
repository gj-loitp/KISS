package com.kiss.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import com.kiss.KissApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.kiss.TagsHandler;
import com.kiss.db.AppRecord;
import com.kiss.db.DBHelper;
import com.kiss.pojo.AppPojo;
import com.kiss.utils.UserHandle;

public class LoadAppPojos extends LoadPojos<AppPojo> {

    private final TagsHandler tagsHandler;

    public LoadAppPojos(Context context) {
        super(context, "app://");
        tagsHandler = KissApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<AppPojo> doInBackground(Void... params) {
        long start = System.currentTimeMillis();

        ArrayList<AppPojo> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        Set<String> excludedAppList = KissApplication.getApplication(ctx).getDataHandler().getExcluded();
        Set<String> excludedFromHistoryAppList = KissApplication.getApplication(ctx).getDataHandler().getExcludedFromHistory();
        Set<String> excludedShortcutsAppList = KissApplication.getApplication(ctx).getDataHandler().getExcludedShortcutApps();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            // Handle multi-profile support introduced in Android 5 (#542)
            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
                for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                    ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                    final AppPojo app = createPojo(user, appInfo.packageName, activityInfo.getName(), activityInfo.getLabel(), excludedAppList, excludedFromHistoryAppList, excludedShortcutsAppList);
                    apps.add(app);
                }
            }
        } else {
            PackageManager manager = ctx.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                final AppPojo app = createPojo(new UserHandle(), appInfo.packageName, info.activityInfo.name, info.loadLabel(manager), excludedAppList, excludedFromHistoryAppList, excludedShortcutsAppList);
                apps.add(app);
            }
        }

        HashMap<String, AppRecord> customApps = DBHelper.getCustomAppData(ctx);
        for (AppPojo app : apps) {
            AppRecord customApp = customApps.get(app.getComponentName());
            if (customApp == null)
                continue;
            if (customApp.hasCustomName())
                app.setName(customApp.name);
            if (customApp.hasCustomIcon())
                app.setCustomIconId(customApp.dbId);
        }

        long end = System.currentTimeMillis();
        Log.i("time", (end - start) + " milliseconds to list apps");

        return apps;
    }

    private AppPojo createPojo(UserHandle userHandle, String packageName, String activityName, CharSequence label, Set<String> excludedAppList, Set<String> excludedFromHistoryAppList, Set<String> excludedShortcutsAppList) {
        String id = userHandle.addUserSuffixToString(pojoScheme + packageName + "/" + activityName, '/');

        boolean isExcluded = excludedAppList.contains(AppPojo.getComponentName(packageName, activityName, userHandle));
        boolean isExcludedFromHistory = excludedFromHistoryAppList.contains(id);
        boolean isExcludedShortcuts = excludedShortcutsAppList.contains(packageName);

        AppPojo app = new AppPojo(id, packageName, activityName, userHandle, isExcluded, isExcludedFromHistory, isExcludedShortcuts);

        app.setName(label.toString());

        app.setTags(tagsHandler.getTags(app.id));

        return app;
    }
}
