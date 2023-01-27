package com.kiss.searcher;

import android.content.Context;

import com.kiss.KissApplication;
import com.kiss.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.kiss.pojo.AppPojo;
import com.kiss.pojo.Pojo;
import com.kiss.pojo.PojoComparator;
import com.kiss.pojo.ShortcutPojo;

/**
 * Returns the list of all applications on the system
 */
public class ApplicationsSearcher extends Searcher {
    public ApplicationsSearcher(MainActivity activity) {
        super(activity, "<application>");
    }

    @Override
    PriorityQueue<Pojo> getPojoProcessor(Context context) {
        // Sort from A to Z, so reverse (last item needs to be A, listview starts at the bottom)
        return new PriorityQueue<>(DEFAULT_MAX_RESULTS, Collections.reverseOrder(new PojoComparator()));
    }

    @Override
    protected int getMaxResultCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        Set<String> excludedFavoriteIds = KissApplication.getApplication(activity).getDataHandler().getExcludedFavorites();

        // add apps
        List<AppPojo> pojos = KissApplication.getApplication(activity).getDataHandler().getApplicationsWithoutExcluded();
        if (pojos != null) {
            this.addResult(getPojosWithoutFavorites(pojos, excludedFavoriteIds).toArray(new Pojo[0]));
        }

        // add pinned shortcuts (PWA, ...)
        List<ShortcutPojo> shortcuts = KissApplication.getApplication(activity).getDataHandler().getPinnedShortcuts();
        if (shortcuts != null) {
            this.addResult(getPojosWithoutFavorites(shortcuts, excludedFavoriteIds).toArray(new Pojo[0]));
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        super.onPostExecute(param);
        // Build sections for fast scrolling
        activityWeakReference.get().adapter.buildSections();
    }

    /**
     * @param pojos               list of pojos
     * @param excludedFavoriteIds ids of favorites to exclude from pojos
     * @return pojos without favorites
     */
    private <T extends Pojo> List<T> getPojosWithoutFavorites(List<T> pojos, Set<String> excludedFavoriteIds) {
        if (excludedFavoriteIds.isEmpty()) {
            return pojos;
        }
        List<T> records = new ArrayList<>(pojos.size());

        for (T pojo : pojos) {
            if (!excludedFavoriteIds.contains(pojo.getFavoriteId())) {
                records.add(pojo);
            }
        }
        return records;
    }

}
