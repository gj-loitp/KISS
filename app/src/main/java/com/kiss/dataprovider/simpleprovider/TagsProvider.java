package com.kiss.dataprovider.simpleprovider;

import com.kiss.pojo.Pojo;
import com.kiss.pojo.TagDummyPojo;
import com.kiss.searcher.Searcher;

import java.util.Locale;

public class TagsProvider extends SimpleProvider {
    public static final String SCHEME = "kisstag://";

    public static String generateUniqueId(String tag) {
        return SCHEME + tag.toLowerCase(Locale.ROOT);
    }

    @Override
    public void requestResults(String s, Searcher searcher) {

    }

    @Override
    public boolean mayFindById(String id) {
        return id.startsWith(SCHEME);
    }

    @Override
    public Pojo findById(String id) {
        return new TagDummyPojo(id);
    }
}
