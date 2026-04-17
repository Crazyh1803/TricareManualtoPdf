package com.tricare.manuals.data.repository;

import com.tricare.manuals.data.db.BookmarkDao;
import com.tricare.manuals.data.db.ManualDao;
import com.tricare.manuals.data.db.SectionDao;
import com.tricare.manuals.data.model.Bookmark;
import com.tricare.manuals.data.model.Manual;
import com.tricare.manuals.data.model.Section;
import com.tricare.manuals.data.network.TricareWebClient;
import com.tricare.manuals.data.network.VersionChecker;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\t\n\u0002\b\u0007\b\u0007\u0018\u0000 02\u00020\u0001:\u00010B/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u0016\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0086@\u00a2\u0006\u0002\u0010\u0011J\u0018\u0010\u0012\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u0014\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010\u0016J\u000e\u0010\u0017\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u0018J\u0016\u0010\u0019\u001a\u00020\u000e2\u0006\u0010\u0014\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010\u0016J$\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00130\u001b2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u001c\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u001dJ\u000e\u0010\u001e\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u0018J\u0012\u0010\u001f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020!0\u001b0 J\u001a\u0010\"\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u001b0 2\u0006\u0010\u0014\u001a\u00020\u0015J\u0018\u0010#\u001a\u0004\u0018\u00010!2\u0006\u0010\u0014\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010\u0016J\"\u0010$\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020%0\u001b0 2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010&\u001a\u00020\u0013J$\u0010\'\u001a\b\u0012\u0004\u0012\u00020%0\u001b2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010&\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u001dJ\u0016\u0010(\u001a\u00020\u000e2\u0006\u0010)\u001a\u00020*H\u0086@\u00a2\u0006\u0002\u0010+J.\u0010,\u001a\u00020\u000e2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010&\u001a\u00020\u00132\u0006\u0010-\u001a\u00020\u00152\u0006\u0010.\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010/R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00061"}, d2 = {"Lcom/tricare/manuals/data/repository/ManualRepository;", "", "manualDao", "Lcom/tricare/manuals/data/db/ManualDao;", "sectionDao", "Lcom/tricare/manuals/data/db/SectionDao;", "bookmarkDao", "Lcom/tricare/manuals/data/db/BookmarkDao;", "webClient", "Lcom/tricare/manuals/data/network/TricareWebClient;", "versionChecker", "Lcom/tricare/manuals/data/network/VersionChecker;", "(Lcom/tricare/manuals/data/db/ManualDao;Lcom/tricare/manuals/data/db/SectionDao;Lcom/tricare/manuals/data/db/BookmarkDao;Lcom/tricare/manuals/data/network/TricareWebClient;Lcom/tricare/manuals/data/network/VersionChecker;)V", "addBookmark", "", "bookmark", "Lcom/tricare/manuals/data/model/Bookmark;", "(Lcom/tricare/manuals/data/model/Bookmark;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkLatestVersion", "", "code", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearAllDownloads", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearDownloadInfo", "discoverAvailableChanges", "", "latestChange", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "ensureDefaultManualsExist", "getAllManuals", "Lkotlinx/coroutines/flow/Flow;", "Lcom/tricare/manuals/data/model/Manual;", "getBookmarks", "getManual", "getSections", "Lcom/tricare/manuals/data/model/Section;", "change", "getSectionsList", "removeBookmark", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateDownloadInfo", "format", "path", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_release"})
public final class ManualRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.db.ManualDao manualDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.db.SectionDao sectionDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.db.BookmarkDao bookmarkDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.network.TricareWebClient webClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.network.VersionChecker versionChecker = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<com.tricare.manuals.data.model.Manual> KNOWN_MANUALS = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.tricare.manuals.data.repository.ManualRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public ManualRepository(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.ManualDao manualDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.SectionDao sectionDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.BookmarkDao bookmarkDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.TricareWebClient webClient, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.VersionChecker versionChecker) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.tricare.manuals.data.model.Manual>> getAllManuals() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object ensureDefaultManualsExist(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object checkLatestVersion(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object discoverAvailableChanges(@org.jetbrains.annotations.NotNull()
    java.lang.String code, int latestChange, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.Integer>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.tricare.manuals.data.model.Bookmark>> getBookmarks(@org.jetbrains.annotations.NotNull()
    java.lang.String code) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addBookmark(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.model.Bookmark bookmark, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object removeBookmark(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.tricare.manuals.data.model.Section>> getSections(@org.jetbrains.annotations.NotNull()
    java.lang.String code, int change) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getSectionsList(@org.jetbrains.annotations.NotNull()
    java.lang.String code, int change, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.tricare.manuals.data.model.Section>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getManual(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.tricare.manuals.data.model.Manual> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateDownloadInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String code, int change, @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    java.lang.String path, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearDownloadInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearAllDownloads(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\b"}, d2 = {"Lcom/tricare/manuals/data/repository/ManualRepository$Companion;", "", "()V", "KNOWN_MANUALS", "", "Lcom/tricare/manuals/data/model/Manual;", "getKNOWN_MANUALS", "()Ljava/util/List;", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.tricare.manuals.data.model.Manual> getKNOWN_MANUALS() {
            return null;
        }
    }
}