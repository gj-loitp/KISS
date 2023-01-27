
package com.kiss.loader;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.kiss.KissApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kiss.DataHandler;
import com.kiss.TagsHandler;
import com.kiss.db.DBHelper;
import com.kiss.db.ShortcutRecord;
import com.kiss.pojo.ShortcutPojo;
import com.kiss.utils.ShortcutUtil;

public class LoadShortcutsPojos extends LoadPojos<ShortcutPojo> {

    public LoadShortcutsPojos(Context context) {
        super(context, ShortcutPojo.SCHEME);
    }

    @Override
    protected ArrayList<ShortcutPojo> doInBackground(Void... arg0) {
        Context context = this.context.get();
        if (context == null) {
            return new ArrayList<>();
        }

        List<ShortcutRecord> records = DBHelper.getShortcuts(context);
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        TagsHandler tagsHandler = dataHandler.getTagsHandler();
        Set<String> excludedApps = dataHandler.getExcluded();
        Set<String> excludedShortcutApps = dataHandler.getExcludedShortcutApps();

        ArrayList<ShortcutPojo> pojos = new ArrayList<>();

        Set<String> visibleShortcutIds = new HashSet<>();
        for (ShortcutRecord shortcutRecord : records) {
            ShortcutPojo pojo = createPojo(shortcutRecord, tagsHandler, null, true, false);
            if (pojo.isOreoShortcut()) {
                // collect ids of oreo shortcuts for visibility check
                visibleShortcutIds.add(pojo.getOreoId());
            } else {
                // add older shortcuts from DB
                pojos.add(pojo);
            }
        }

        // get all oreo shortcuts from system directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<ShortcutInfo> shortcutInfos = ShortcutUtil.getAllShortcuts(context);
            for (ShortcutInfo shortcutInfo : shortcutInfos) {
                if (isShortcutVisible(context, shortcutInfo, excludedApps, excludedShortcutApps, visibleShortcutIds)) {
                    ShortcutRecord shortcutRecord = ShortcutUtil.createShortcutRecord(context, shortcutInfo, !shortcutInfo.isPinned());
                    if (shortcutRecord != null) {
                        ShortcutPojo pojo = createPojo(shortcutRecord, tagsHandler, ShortcutUtil.getComponentName(context, shortcutInfo), shortcutInfo.isPinned(), shortcutInfo.isDynamic());
                        pojos.add(pojo);
                    }
                }
            }
        }

        return pojos;
    }

    private ShortcutPojo createPojo(ShortcutRecord shortcutRecord, TagsHandler tagsHandler, String componentName, boolean pinned, boolean dynamic) {
        ShortcutPojo pojo = new ShortcutPojo(shortcutRecord, componentName, pinned, dynamic);
        pojo.setName(shortcutRecord.name);
        pojo.setTags(tagsHandler.getTags(pojo.id));
        return pojo;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private boolean isShortcutVisible(Context context, ShortcutInfo shortcutInfo, Set<String> excludedApps, Set<String> excludedShortcutApps, Set<String> visibleShortcutIds) {
        if (!shortcutInfo.isEnabled()) {
            return false;
        }
        String packageName = shortcutInfo.getPackage();
        String componentName = ShortcutUtil.getComponentName(context, shortcutInfo);

        // if related package is excluded from KISS then the shortcut must be excluded too
        boolean isExcluded = excludedApps.contains(componentName) || excludedShortcutApps.contains(packageName);
        if (isExcluded) {
            return false;
        }

        if (shortcutInfo.isPinned()) {
            return visibleShortcutIds.contains(shortcutInfo.getId());
        }
        return true;
    }
}
