package org.olxscrapper.pageobject;

public class OlxData {
    private final String anchor;
    private final String title;

    public OlxData(String anchor, String title) {
        this.anchor = anchor;
        this.title = title;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getTitle() {
        return title;
    }
}
