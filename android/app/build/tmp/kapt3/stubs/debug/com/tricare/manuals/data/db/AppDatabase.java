package com.tricare.manuals.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.tricare.manuals.data.model.Bookmark;
import com.tricare.manuals.data.model.Highlight;
import com.tricare.manuals.data.model.Manual;
import com.tricare.manuals.data.model.Section;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&J\b\u0010\t\u001a\u00020\nH&\u00a8\u0006\u000b"}, d2 = {"Lcom/tricare/manuals/data/db/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "bookmarkDao", "Lcom/tricare/manuals/data/db/BookmarkDao;", "highlightDao", "Lcom/tricare/manuals/data/db/HighlightDao;", "manualDao", "Lcom/tricare/manuals/data/db/ManualDao;", "sectionDao", "Lcom/tricare/manuals/data/db/SectionDao;", "app_debug"})
@androidx.room.Database(entities = {com.tricare.manuals.data.model.Manual.class, com.tricare.manuals.data.model.Section.class, com.tricare.manuals.data.model.Bookmark.class, com.tricare.manuals.data.model.Highlight.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.tricare.manuals.data.db.ManualDao manualDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.tricare.manuals.data.db.SectionDao sectionDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.tricare.manuals.data.db.BookmarkDao bookmarkDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.tricare.manuals.data.db.HighlightDao highlightDao();
}