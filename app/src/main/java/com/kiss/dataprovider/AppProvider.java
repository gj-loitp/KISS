package com.kiss.dataprovider;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.preference.PreferenceManager;

import com.kiss.KissApplication;
import com.kiss.broadcast.PackageAddedRemovedHandler;
import com.kiss.loader.LoadAppPojos;
import com.kiss.normalizer.StringNormalizer;
import com.kiss.pojo.AppPojo;
import com.kiss.pojo.Pojo;
import com.kiss.searcher.Searcher;
import com.kiss.utils.FuzzyScore;
import com.kiss.utils.UserHandle;

import java.util.ArrayList;
import java.util.Set;

public class AppProvider extends Provider<AppPojo> {

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Package installation/uninstallation events for the main
            // profile are still handled using PackageAddedRemovedHandler itself
            final UserManager manager = (UserManager) this.getSystemService(USER_SERVICE);
            assert manager != null;

            final LauncherApps launcher = (LauncherApps) this.getSystemService(LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherAppsCallback() {
                @Override
                public void onPackageAdded(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                Intent.ACTION_PACKAGE_ADDED,
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackageChanged(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                Intent.ACTION_PACKAGE_ADDED,
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), true
                        );
                    }
                }

                @Override
                public void onPackageRemoved(String packageName, android.os.UserHandle user) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                Intent.ACTION_PACKAGE_REMOVED,
                                packageName, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                Intent.ACTION_MEDIA_MOUNTED,
                                null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }

                @Override
                public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
                    if (!Process.myUserHandle().equals(user)) {
                        PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                                Intent.ACTION_MEDIA_UNMOUNTED,
                                null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                        );
                    }
                }
            });
        }

        // Get notified when app changes on standard user profile
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        appChangedFilter.addDataScheme("package");
        appChangedFilter.addDataScheme("file");
        this.registerReceiver(new PackageAddedRemovedHandler(), appChangedFilter);

        super.onCreate();
    }

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadAppPojos(this));
    }

    /**
     * @param query    The string to search for
     * @param searcher The receiver of results
     */

    @Override
    public void requestResults(String query, Searcher searcher) {
        Set<String> excludedFavoriteIds = KissApplication.getApplication(this).getDataHandler().getExcludedFavorites();

        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (AppPojo pojo : pojos) {
            // exclude apps from results
            if(pojo.isExcluded() && !prefs.getBoolean("enable-excluded-apps", false)) {
                continue;
            }
            // exclude favorites from results
            if (excludedFavoriteIds.contains(pojo.getFavoriteId())) {
                continue;
            }

            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.relevance = matchInfo.score;

            // check relevance for tags
            if (pojo.getNormalizedTags() != null) {
                matchInfo = fuzzyScore.match(pojo.getNormalizedTags().codePoints);
                if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
                    match = true;
                    pojo.relevance = matchInfo.score;
                }
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    /**
     * Return a Pojo
     *
     * @param id              we're looking for
     * @return an AppPojo, or null
     */
    @Override
    public Pojo findById(String id) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    public ArrayList<AppPojo> getAllApps() {
        ArrayList<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }

    public ArrayList<AppPojo> getAllAppsWithoutExcluded() {
        ArrayList<AppPojo> records = new ArrayList<>(pojos.size());

        for (AppPojo pojo : pojos) {
            if(pojo.isExcluded()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
