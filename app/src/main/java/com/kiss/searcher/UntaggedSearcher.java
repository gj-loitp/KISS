package com.kiss.searcher;

import com.kiss.KissApplication;
import com.kiss.MainActivity;

import java.util.Iterator;
import java.util.List;

import com.kiss.pojo.AppPojo;
import com.kiss.pojo.Pojo;
import com.kiss.pojo.PojoWithTags;

public class UntaggedSearcher extends Searcher {

    public UntaggedSearcher(MainActivity activity )
    {
        super( activity, "<untagged>" );
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if ( activity == null )
            return null;
        List<AppPojo> results = KissApplication.getApplication(activity).getDataHandler().getApplicationsWithoutExcluded();
        if (results == null)
            return null;
        for(Iterator<AppPojo> iterator = results.iterator(); iterator.hasNext(); ) {
            Pojo pojo = iterator.next();
            if (!(pojo instanceof PojoWithTags)) {
                iterator.remove();
                continue;
            }
            PojoWithTags pojoWithTags = (PojoWithTags) pojo;
            if (pojoWithTags.getTags() == null || pojoWithTags.getTags().isEmpty()) {
                continue;
            }
            iterator.remove();
        }
        this.addResult(results.toArray(new Pojo[0]));
        return null;
    }
}
