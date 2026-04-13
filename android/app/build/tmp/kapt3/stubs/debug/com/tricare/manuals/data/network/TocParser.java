package com.tricare.manuals.data.network;

import org.jsoup.Jsoup;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\t\b\u0007\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J$\u0010\u0003\u001a\u00020\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0002J\u0010\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bH\u0002J\u000e\u0010\f\u001a\u00020\t2\u0006\u0010\r\u001a\u00020\tJ\u0010\u0010\u000e\u001a\u00020\t2\u0006\u0010\u000f\u001a\u00020\tH\u0002J\u000e\u0010\u0010\u001a\u00020\t2\u0006\u0010\r\u001a\u00020\tJ\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\tJ\u001a\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\t0\u00062\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\t0\u0006J\u001c\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\t0\u00062\u0006\u0010\r\u001a\u00020\t2\u0006\u0010\u0017\u001a\u00020\tJ\u0016\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00062\u0006\u0010\u0019\u001a\u00020\tH\u0002\u00a8\u0006\u001b"}, d2 = {"Lcom/tricare/manuals/data/network/TocParser;", "", "()V", "compareNaturalParts", "", "a", "", "b", "elementToMarkdown", "", "element", "Lorg/jsoup/nodes/Element;", "extractTitle", "html", "filenameFromUrl", "url", "htmlToMarkdown", "isChapterToc", "", "filename", "naturalSort", "urls", "parseChapterTocUrls", "baseUrl", "splitNatural", "s", "Companion", "app_debug"})
public final class TocParser {
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex CHAPTER_TOC_REGEX = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex SECTION_URL_PATTERN = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex NATURAL_SORT_REGEX = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.tricare.manuals.data.network.TocParser.Companion Companion = null;
    
    @javax.inject.Inject()
    public TocParser() {
        super();
    }
    
    /**
     * Parses a TOC HTML page and returns all hrefs that look like chapter or section HTML files.
     * [baseUrl] is used to resolve relative paths.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> parseChapterTocUrls(@org.jetbrains.annotations.NotNull()
    java.lang.String html, @org.jetbrains.annotations.NotNull()
    java.lang.String baseUrl) {
        return null;
    }
    
    /**
     * Returns true if [filename] looks like a chapter TOC file (e.g. C1TOC.html, C12TOC.html).
     */
    public final boolean isChapterToc(@org.jetbrains.annotations.NotNull()
    java.lang.String filename) {
        return false;
    }
    
    /**
     * Sorts a list of section URLs using natural (numeric) sort order based on filename components.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> naturalSort(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> urls) {
        return null;
    }
    
    private final java.lang.String filenameFromUrl(java.lang.String url) {
        return null;
    }
    
    private final java.util.List<java.lang.Object> splitNatural(java.lang.String s) {
        return null;
    }
    
    private final int compareNaturalParts(java.util.List<? extends java.lang.Object> a, java.util.List<? extends java.lang.Object> b) {
        return 0;
    }
    
    /**
     * Extracts the section title from an HTML page.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String extractTitle(@org.jetbrains.annotations.NotNull()
    java.lang.String html) {
        return null;
    }
    
    /**
     * Converts HTML content to a simplified Markdown string.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String htmlToMarkdown(@org.jetbrains.annotations.NotNull()
    java.lang.String html) {
        return null;
    }
    
    private final java.lang.String elementToMarkdown(org.jsoup.nodes.Element element) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/tricare/manuals/data/network/TocParser$Companion;", "", "()V", "CHAPTER_TOC_REGEX", "Lkotlin/text/Regex;", "NATURAL_SORT_REGEX", "SECTION_URL_PATTERN", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}