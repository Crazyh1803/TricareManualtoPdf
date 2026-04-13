package com.tricare.manuals.di;

import android.content.Context;
import androidx.room.Room;
import com.tricare.manuals.data.db.AppDatabase;
import com.tricare.manuals.data.db.BookmarkDao;
import com.tricare.manuals.data.db.HighlightDao;
import com.tricare.manuals.data.db.ManualDao;
import com.tricare.manuals.data.db.SectionDao;
import com.tricare.manuals.data.network.TocParser;
import com.tricare.manuals.data.network.TricareWebClient;
import com.tricare.manuals.data.network.VersionChecker;
import com.tricare.manuals.data.repository.ManualRepository;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\t\u001a\u00020\u0004H\u0007J0\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\b2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0007J\u0010\u0010\u0018\u001a\u00020\u00122\u0006\u0010\t\u001a\u00020\u0004H\u0007J\b\u0010\u0019\u001a\u00020\u001aH\u0007J\b\u0010\u001b\u001a\u00020\u0015H\u0007J\u0010\u0010\u001c\u001a\u00020\u00172\u0006\u0010\u0014\u001a\u00020\u0015H\u0007\u00a8\u0006\u001d"}, d2 = {"Lcom/tricare/manuals/di/AppModule;", "", "()V", "provideAppDatabase", "Lcom/tricare/manuals/data/db/AppDatabase;", "context", "Landroid/content/Context;", "provideBookmarkDao", "Lcom/tricare/manuals/data/db/BookmarkDao;", "db", "provideHighlightDao", "Lcom/tricare/manuals/data/db/HighlightDao;", "provideManualDao", "Lcom/tricare/manuals/data/db/ManualDao;", "provideManualRepository", "Lcom/tricare/manuals/data/repository/ManualRepository;", "manualDao", "sectionDao", "Lcom/tricare/manuals/data/db/SectionDao;", "bookmarkDao", "webClient", "Lcom/tricare/manuals/data/network/TricareWebClient;", "versionChecker", "Lcom/tricare/manuals/data/network/VersionChecker;", "provideSectionDao", "provideTocParser", "Lcom/tricare/manuals/data/network/TocParser;", "provideTricareWebClient", "provideVersionChecker", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class AppModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.tricare.manuals.di.AppModule INSTANCE = null;
    
    private AppModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.db.AppDatabase provideAppDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.db.ManualDao provideManualDao(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.AppDatabase db) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.db.SectionDao provideSectionDao(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.AppDatabase db) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.db.BookmarkDao provideBookmarkDao(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.AppDatabase db) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.db.HighlightDao provideHighlightDao(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.AppDatabase db) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.network.TricareWebClient provideTricareWebClient() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.network.VersionChecker provideVersionChecker(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.TricareWebClient webClient) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.network.TocParser provideTocParser() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.repository.ManualRepository provideManualRepository(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.ManualDao manualDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.SectionDao sectionDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.BookmarkDao bookmarkDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.TricareWebClient webClient, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.VersionChecker versionChecker) {
        return null;
    }
}