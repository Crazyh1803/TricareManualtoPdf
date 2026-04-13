package com.tricare.manuals.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.hilt.work.HiltWorker;
import androidx.work.CoroutineWorker;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import com.tricare.manuals.data.db.ManualDao;
import com.tricare.manuals.data.db.SectionDao;
import com.tricare.manuals.data.model.Section;
import com.tricare.manuals.data.network.TocParser;
import com.tricare.manuals.data.network.TricareWebClient;
import com.tricare.manuals.data.repository.ManualRepository;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import kotlinx.coroutines.Dispatchers;
import android.os.Environment;
import java.io.File;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u0000 \"2\u00020\u0001:\u0001\"B;\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ7\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0016\u001a\u00020\u00142\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0002\u00a2\u0006\u0002\u0010\u0019J\u000e\u0010\u001a\u001a\u00020\u001bH\u0096@\u00a2\u0006\u0002\u0010\u001cJ\u0010\u0010\u001d\u001a\u00020\u00122\u0006\u0010\u001e\u001a\u00020\u0018H\u0002J\b\u0010\u001f\u001a\u00020 H\u0002J\b\u0010!\u001a\u00020\u0018H\u0002R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"Lcom/tricare/manuals/worker/DownloadWorker;", "Landroidx/work/CoroutineWorker;", "appContext", "Landroid/content/Context;", "params", "Landroidx/work/WorkerParameters;", "webClient", "Lcom/tricare/manuals/data/network/TricareWebClient;", "tocParser", "Lcom/tricare/manuals/data/network/TocParser;", "manualDao", "Lcom/tricare/manuals/data/db/ManualDao;", "sectionDao", "Lcom/tricare/manuals/data/db/SectionDao;", "(Landroid/content/Context;Landroidx/work/WorkerParameters;Lcom/tricare/manuals/data/network/TricareWebClient;Lcom/tricare/manuals/data/network/TocParser;Lcom/tricare/manuals/data/db/ManualDao;Lcom/tricare/manuals/data/db/SectionDao;)V", "createForegroundInfo", "Landroidx/work/ForegroundInfo;", "code", "", "changeNum", "", "current", "total", "etaSeconds", "", "(Ljava/lang/String;IIILjava/lang/Long;)Landroidx/work/ForegroundInfo;", "doWork", "Landroidx/work/ListenableWorker$Result;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "formatEta", "seconds", "isOnWifi", "", "randomDelay", "Companion", "app_debug"})
@androidx.hilt.work.HiltWorker()
public final class DownloadWorker extends androidx.work.CoroutineWorker {
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.network.TricareWebClient webClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.network.TocParser tocParser = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.db.ManualDao manualDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.db.SectionDao sectionDao = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_MANUAL_CODE = "manual_code";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_FORMAT = "format";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_CHANGE_NUM = "change_num";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_PROGRESS_CURRENT = "current";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_PROGRESS_TOTAL = "total";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_MANUAL_PROGRESS = "manual";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_ERROR_REASON = "error_reason";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_ETA_SECONDS = "eta_seconds";
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> WIFI_ONLY_KEY = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_TOC_URL = "https://manuals.health.mil/pages/ManualToc.aspx?Manual=";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHANNEL_ID = "tricare_download";
    private static final int NOTIFICATION_ID = 1001;
    @org.jetbrains.annotations.NotNull()
    public static final com.tricare.manuals.worker.DownloadWorker.Companion Companion = null;
    
    @dagger.assisted.AssistedInject()
    public DownloadWorker(@dagger.assisted.Assisted()
    @org.jetbrains.annotations.NotNull()
    android.content.Context appContext, @dagger.assisted.Assisted()
    @org.jetbrains.annotations.NotNull()
    androidx.work.WorkerParameters params, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.TricareWebClient webClient, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.network.TocParser tocParser, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.ManualDao manualDao, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.SectionDao sectionDao) {
        super(null, null);
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object doWork(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super androidx.work.ListenableWorker.Result> $completion) {
        return null;
    }
    
    /**
     * Builds a [ForegroundInfo] for the persistent download notification.
     * Safe to call multiple times — the notification channel is created on first call.
     */
    private final androidx.work.ForegroundInfo createForegroundInfo(java.lang.String code, int changeNum, int current, int total, java.lang.Long etaSeconds) {
        return null;
    }
    
    private final java.lang.String formatEta(long seconds) {
        return null;
    }
    
    private final boolean isOnWifi() {
        return false;
    }
    
    private final long randomDelay() {
        return 0L;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/tricare/manuals/worker/DownloadWorker$Companion;", "", "()V", "BASE_TOC_URL", "", "CHANNEL_ID", "KEY_CHANGE_NUM", "KEY_ERROR_REASON", "KEY_ETA_SECONDS", "KEY_FORMAT", "KEY_MANUAL_CODE", "KEY_MANUAL_PROGRESS", "KEY_PROGRESS_CURRENT", "KEY_PROGRESS_TOTAL", "NOTIFICATION_ID", "", "WIFI_ONLY_KEY", "Landroidx/datastore/preferences/core/Preferences$Key;", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}