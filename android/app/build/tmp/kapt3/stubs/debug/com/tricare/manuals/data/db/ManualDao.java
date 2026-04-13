package com.tricare.manuals.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.tricare.manuals.data.model.Manual;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\n\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\u000f0\u000eH\'J\u0018\u0010\u0010\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ6\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00072\u0006\u0010\u0015\u001a\u00020\u00072\u0006\u0010\u0016\u001a\u00020\u0017H\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u001e\u0010\u0019\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u001a\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\u0016\u0010\u001c\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ\u0016\u0010\u001d\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ\u001c\u0010\u001e\u001a\u00020\u00032\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000fH\u00a7@\u00a2\u0006\u0002\u0010 \u00a8\u0006!"}, d2 = {"Lcom/tricare/manuals/data/db/ManualDao;", "", "clearAllDownloadInfo", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearDownloadInfo", "code", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteManual", "manual", "Lcom/tricare/manuals/data/model/Manual;", "(Lcom/tricare/manuals/data/model/Manual;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllManuals", "Lkotlinx/coroutines/flow/Flow;", "", "getManual", "updateDownloadInfo", "change", "", "format", "path", "timestamp", "", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateLatestChange", "latestChange", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateManual", "upsertManual", "upsertManuals", "manuals", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface ManualDao {
    
    @androidx.room.Query(value = "SELECT * FROM manuals ORDER BY name ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.tricare.manuals.data.model.Manual>> getAllManuals();
    
    @androidx.room.Query(value = "SELECT * FROM manuals WHERE code = :code LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getManual(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.tricare.manuals.data.model.Manual> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsertManual(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.model.Manual manual, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsertManuals(@org.jetbrains.annotations.NotNull()
    java.util.List<com.tricare.manuals.data.model.Manual> manuals, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateManual(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.model.Manual manual, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteManual(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.model.Manual manual, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        UPDATE manuals\n        SET downloadedChange = :change,\n            downloadedFormat = :format,\n            filePath = :path,\n            downloadedAt = :timestamp\n        WHERE code = :code\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateDownloadInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String code, int change, @org.jetbrains.annotations.NotNull()
    java.lang.String format, @org.jetbrains.annotations.NotNull()
    java.lang.String path, long timestamp, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE manuals SET latestChange = :latestChange WHERE code = :code")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateLatestChange(@org.jetbrains.annotations.NotNull()
    java.lang.String code, int latestChange, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        UPDATE manuals\n        SET downloadedChange = NULL,\n            downloadedFormat = NULL,\n            filePath = NULL,\n            downloadedAt = NULL\n        WHERE code = :code\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearDownloadInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE manuals SET downloadedChange = NULL, downloadedFormat = NULL, filePath = NULL, downloadedAt = NULL")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearAllDownloadInfo(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}