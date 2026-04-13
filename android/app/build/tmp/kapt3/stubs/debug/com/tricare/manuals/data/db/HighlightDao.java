package com.tricare.manuals.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.tricare.manuals.data.model.Highlight;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ$\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\f2\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\u0005H\'J\u0016\u0010\u0010\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u0012\u00a8\u0006\u0013"}, d2 = {"Lcom/tricare/manuals/data/db/HighlightDao;", "", "clearHighlightsForManual", "", "manualCode", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteHighlight", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getHighlights", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/tricare/manuals/data/model/Highlight;", "sectionFilename", "insertHighlight", "highlight", "(Lcom/tricare/manuals/data/model/Highlight;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface HighlightDao {
    
    @androidx.room.Query(value = "SELECT * FROM highlights WHERE manualCode = :manualCode AND sectionFilename = :sectionFilename ORDER BY startOffset ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.tricare.manuals.data.model.Highlight>> getHighlights(@org.jetbrains.annotations.NotNull()
    java.lang.String manualCode, @org.jetbrains.annotations.NotNull()
    java.lang.String sectionFilename);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertHighlight(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.model.Highlight highlight, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM highlights WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteHighlight(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM highlights WHERE manualCode = :manualCode")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearHighlightsForManual(@org.jetbrains.annotations.NotNull()
    java.lang.String manualCode, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}