package com.tricare.manuals.ui.reader;

import androidx.lifecycle.ViewModel;
import com.tricare.manuals.data.model.Bookmark;
import com.tricare.manuals.data.model.Highlight;
import com.tricare.manuals.data.model.Section;
import com.tricare.manuals.data.repository.ManualRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;
import com.tricare.manuals.data.db.HighlightDao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\t\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00122\u0006\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001aJ.\u0010\u001b\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00122\u0006\u0010\u001c\u001a\u00020\u001a2\u0006\u0010\u001d\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00122\u0006\u0010\u001f\u001a\u00020\u0012J\u000e\u0010 \u001a\u00020\u00162\u0006\u0010!\u001a\u00020\u0012J\u000e\u0010\"\u001a\u00020\u00162\u0006\u0010#\u001a\u00020$R\u001a\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\t0\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0010\u00a8\u0006%"}, d2 = {"Lcom/tricare/manuals/ui/reader/ReaderViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/tricare/manuals/data/repository/ManualRepository;", "highlightDao", "Lcom/tricare/manuals/data/db/HighlightDao;", "(Lcom/tricare/manuals/data/repository/ManualRepository;Lcom/tricare/manuals/data/db/HighlightDao;)V", "_bookmarks", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/tricare/manuals/data/model/Bookmark;", "_sections", "Lcom/tricare/manuals/data/model/Section;", "bookmarks", "Lkotlinx/coroutines/flow/StateFlow;", "getBookmarks", "()Lkotlinx/coroutines/flow/StateFlow;", "manualCode", "", "sections", "getSections", "addBookmark", "", "sectionFilename", "sectionTitle", "scrollY", "", "addHighlight", "start", "end", "text", "color", "init", "code", "removeBookmark", "id", "", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class ReaderViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.repository.ManualRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.tricare.manuals.data.db.HighlightDao highlightDao = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.tricare.manuals.data.model.Section>> _sections = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.tricare.manuals.data.model.Section>> sections = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.tricare.manuals.data.model.Bookmark>> _bookmarks = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.tricare.manuals.data.model.Bookmark>> bookmarks = null;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String manualCode = "";
    
    @javax.inject.Inject()
    public ReaderViewModel(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.repository.ManualRepository repository, @org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.db.HighlightDao highlightDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.tricare.manuals.data.model.Section>> getSections() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.tricare.manuals.data.model.Bookmark>> getBookmarks() {
        return null;
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    java.lang.String code) {
    }
    
    public final void addBookmark(@org.jetbrains.annotations.NotNull()
    java.lang.String sectionFilename, @org.jetbrains.annotations.NotNull()
    java.lang.String sectionTitle, int scrollY) {
    }
    
    public final void removeBookmark(long id) {
    }
    
    public final void addHighlight(@org.jetbrains.annotations.NotNull()
    java.lang.String sectionFilename, int start, int end, @org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    java.lang.String color) {
    }
}