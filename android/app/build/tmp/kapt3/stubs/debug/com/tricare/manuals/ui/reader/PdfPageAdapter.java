package com.tricare.manuals.ui.reader;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;

/**
 * Renders a PDF file page-by-page using Android's built-in PdfRenderer.
 * Each page is rendered lazily as it scrolls into view.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u0016B\r\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0005J\b\u0010\n\u001a\u00020\u000bH\u0016J\u0018\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u00022\u0006\u0010\u000f\u001a\u00020\u000bH\u0016J\u0018\u0010\u0010\u001a\u00020\u00022\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u000bH\u0016J\u0010\u0010\u0014\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u0002H\u0016J\u0006\u0010\u0015\u001a\u00020\rR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/tricare/manuals/ui/reader/PdfPageAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/tricare/manuals/ui/reader/PdfPageAdapter$PageViewHolder;", "file", "Ljava/io/File;", "(Ljava/io/File;)V", "fileDescriptor", "Landroid/os/ParcelFileDescriptor;", "renderer", "Landroid/graphics/pdf/PdfRenderer;", "getItemCount", "", "onBindViewHolder", "", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "onViewRecycled", "release", "PageViewHolder", "app_debug"})
public final class PdfPageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.tricare.manuals.ui.reader.PdfPageAdapter.PageViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final java.io.File file = null;
    @org.jetbrains.annotations.Nullable()
    private android.graphics.pdf.PdfRenderer renderer;
    @org.jetbrains.annotations.Nullable()
    private android.os.ParcelFileDescriptor fileDescriptor;
    
    public PdfPageAdapter(@org.jetbrains.annotations.NotNull()
    java.io.File file) {
        super();
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.tricare.manuals.ui.reader.PdfPageAdapter.PageViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.ui.reader.PdfPageAdapter.PageViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public void onViewRecycled(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.ui.reader.PdfPageAdapter.PageViewHolder holder) {
    }
    
    public final void release() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/tricare/manuals/ui/reader/PdfPageAdapter$PageViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "imageView", "Landroid/widget/ImageView;", "(Landroid/widget/ImageView;)V", "getImageView", "()Landroid/widget/ImageView;", "app_debug"})
    public static final class PageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.ImageView imageView = null;
        
        public PageViewHolder(@org.jetbrains.annotations.NotNull()
        android.widget.ImageView imageView) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.ImageView getImageView() {
            return null;
        }
    }
}