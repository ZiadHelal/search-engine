package com.example.search;

public class TextResult {
    private String Title;
    private String Url;
    private String Snippet;

    public TextResult(String title, String url, String snippet) {
        Title = title;
        Url = url;
        Snippet = snippet;
    }

    public String getTitle() {
        return Title;
    }

    public String getUrl() {
        return Url;
    }

    public String getSnippet() {
        return Snippet;
    }

}
