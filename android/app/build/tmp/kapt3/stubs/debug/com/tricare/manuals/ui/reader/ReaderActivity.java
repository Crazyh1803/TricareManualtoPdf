package com.tricare.manuals.ui.reader;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tricare.manuals.R;
import com.tricare.manuals.databinding.ActivityReaderBinding;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u0000 12\u00020\u0001:\u00011B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\nH\u0002J\b\u0010\u001c\u001a\u00020\u001aH\u0002J\u0012\u0010\u001d\u001a\u00020\u001a2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0014J\u0010\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020#H\u0016J\b\u0010$\u001a\u00020\u001aH\u0014J\u0010\u0010%\u001a\u00020!2\u0006\u0010&\u001a\u00020\'H\u0016J\u0016\u0010(\u001a\u00020\u001a2\f\u0010)\u001a\b\u0012\u0004\u0012\u00020+0*H\u0002J\b\u0010,\u001a\u00020\u001aH\u0002J\b\u0010-\u001a\u00020\u001aH\u0002J\b\u0010.\u001a\u00020\u001aH\u0002J\b\u0010/\u001a\u00020\u001aH\u0002J\b\u00100\u001a\u00020\u001aH\u0002R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0013\u001a\u00020\u00148BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\u0018\u001a\u0004\b\u0015\u0010\u0016\u00a8\u00062"}, d2 = {"Lcom/tricare/manuals/ui/reader/ReaderActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/tricare/manuals/databinding/ActivityReaderBinding;", "getBinding", "()Lcom/tricare/manuals/databinding/ActivityReaderBinding;", "setBinding", "(Lcom/tricare/manuals/databinding/ActivityReaderBinding;)V", "currentSectionIndex", "", "filePath", "", "format", "manualCode", "pdfPageAdapter", "Lcom/tricare/manuals/ui/reader/PdfPageAdapter;", "sectionAdapter", "Lcom/tricare/manuals/ui/reader/SectionAdapter;", "viewModel", "Lcom/tricare/manuals/ui/reader/ReaderViewModel;", "getViewModel", "()Lcom/tricare/manuals/ui/reader/ReaderViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "navigateToSection", "", "index", "observeViewModel", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onCreateOptionsMenu", "", "menu", "Landroid/view/Menu;", "onDestroy", "onOptionsItemSelected", "item", "Landroid/view/MenuItem;", "populateDrawerMenu", "sections", "", "Lcom/tricare/manuals/data/model/Section;", "saveCurrentBookmark", "setupDrawer", "setupMarkdownViewer", "setupPdfViewer", "showBookmarkSheet", "Companion", "app_debug"})
public final class ReaderActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_MANUAL_CODE = "extra_manual_code";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_FILE_PATH = "extra_file_path";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_FORMAT = "extra_format";
    public com.tricare.manuals.databinding.ActivityReaderBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String manualCode = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String filePath = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String format = "md";
    @org.jetbrains.annotations.Nullable()
    private com.tricare.manuals.ui.reader.SectionAdapter sectionAdapter;
    @org.jetbrains.annotations.Nullable()
    private com.tricare.manuals.ui.reader.PdfPageAdapter pdfPageAdapter;
    private int currentSectionIndex = 0;
    @org.jetbrains.annotations.NotNull()
    public static final com.tricare.manuals.ui.reader.ReaderActivity.Companion Companion = null;
    
    public ReaderActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.databinding.ActivityReaderBinding getBinding() {
        return null;
    }
    
    public final void setBinding(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.databinding.ActivityReaderBinding p0) {
    }
    
    private final com.tricare.manuals.ui.reader.ReaderViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupPdfViewer() {
    }
    
    private final void setupMarkdownViewer() {
    }
    
    private final void setupDrawer() {
    }
    
    private final void saveCurrentBookmark() {
    }
    
    private final void showBookmarkSheet() {
    }
    
    private final void navigateToSection(int index) {
    }
    
    private final void populateDrawerMenu(java.util.List<com.tricare.manuals.data.model.Section> sections) {
    }
    
    private final void observeViewModel() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @java.lang.Override()
    public boolean onCreateOptionsMenu(@org.jetbrains.annotations.NotNull()
    android.view.Menu menu) {
        return false;
    }
    
    @java.lang.Override()
    public boolean onOptionsItemSelected(@org.jetbrains.annotations.NotNull()
    android.view.MenuItem item) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/tricare/manuals/ui/reader/ReaderActivity$Companion;", "", "()V", "EXTRA_FILE_PATH", "", "EXTRA_FORMAT", "EXTRA_MANUAL_CODE", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}