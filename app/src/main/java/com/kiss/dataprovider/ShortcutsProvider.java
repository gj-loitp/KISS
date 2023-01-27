package com.kiss.dataprovider;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.widget.Toast;

import com.R;
import com.kiss.KissApplication;
import com.kiss.loader.LoadShortcutsPojos;
import com.kiss.normalizer.StringNormalizer;
import com.kiss.pojo.ShortcutPojo;
import com.kiss.searcher.Searcher;
import com.kiss.utils.FuzzyScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ShortcutsProvider extends Provider<ShortcutPojo> {
    private static boolean notifiedKissNotDefaultLauncher = false;

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            launcher.registerCallback(new LauncherAppsCallback() {
                @Override
                public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, android.os.UserHandle user) {
                    KissApplication.getApplication(ShortcutsProvider.this).getDataHandler().reloadShortcuts();
                }
            });
        }

        super.onCreate();
    }

    @Override
    public void reload() {
        super.reload();
        // If the user tries to add a new shortcut, but KISS isn't the default launcher
        // AND the services are not running (low memory), then we won't be able to
        // spawn a new service on Android 8.1+.

        try {
            this.initialize(new LoadShortcutsPojos(this));
        } catch (IllegalStateException e) {
            if (!notifiedKissNotDefaultLauncher) {
                // Only display this message once per process
                Toast.makeText(this, R.string.unable_to_initialize_shortcuts, Toast.LENGTH_LONG).show();
            }
            notifiedKissNotDefaultLauncher = true;
            e.printStackTrace();
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        Set<String> excludedFavoriteIds = KissApplication.getApplication(this).getDataHandler().getExcludedFavorites();

        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ShortcutPojo pojo : pojos) {
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

    public List<ShortcutPojo> getPinnedShortcuts() {
        List<ShortcutPojo> records = new ArrayList<>(pojos.size());

        for (ShortcutPojo pojo : pojos) {
            if (!pojo.isPinned()) continue;

            pojo.relevance = 0;
            records.add(pojo);
        }
        return records;
    }
}
