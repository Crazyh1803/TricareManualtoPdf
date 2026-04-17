package com.tricare.manuals.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import com.tricare.manuals.data.model.Manual;
import com.tricare.manuals.databinding.ItemManualBinding;
import com.tricare.manuals.worker.DownloadWorker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010!\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u0012\u0012\u0004\u0012\u00020\u0002\u0012\b\u0012\u00060\u0003R\u00020\u00000\u0001:\u0002\u001d\u001eBU\u0012\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u000f\u001a\u00020\u00062\n\u0010\u0010\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u0011\u001a\u00020\u0012H\u0016J*\u0010\u000f\u001a\u00020\u00062\n\u0010\u0010\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u0011\u001a\u00020\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014H\u0016J\u001c\u0010\u0016\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0012H\u0016J\u0018\u0010\u001a\u001a\u00020\u00062\u0006\u0010\u001b\u001a\u00020\r2\b\u0010\u001c\u001a\u0004\u0018\u00010\u000eR\u001a\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u000b\u001a\u0010\u0012\u0004\u0012\u00020\r\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/tricare/manuals/ui/main/ManualAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/tricare/manuals/data/model/Manual;", "Lcom/tricare/manuals/ui/main/ManualAdapter$ManualViewHolder;", "onOpenClick", "Lkotlin/Function1;", "", "onDownloadClick", "onShareClick", "onExportPdfClick", "(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "progressMap", "", "", "Landroidx/work/WorkInfo;", "onBindViewHolder", "holder", "position", "", "payloads", "", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "updateProgress", "code", "workInfo", "ManualDiffCallback", "ManualViewHolder", "app_release"})
public final class ManualAdapter extends androidx.recyclerview.widget.ListAdapter<com.tricare.manuals.data.model.Manual, com.tricare.manuals.ui.main.ManualAdapter.ManualViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.tricare.manuals.data.model.Manual, kotlin.Unit> onOpenClick = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.tricare.manuals.data.model.Manual, kotlin.Unit> onDownloadClick = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.tricare.manuals.data.model.Manual, kotlin.Unit> onShareClick = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.tricare.manuals.data.model.Manual, kotlin.Unit> onExportPdfClick = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, androidx.work.WorkInfo> progressMap = null;
    
    public ManualAdapter(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.tricare.manuals.data.model.Manual, kotlin.Unit> onOpenClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.tricare.manuals.data.model.Manual, kotlin.Unit> onDownloadClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.tricare.manuals.data.model.Manual, kotlin.Unit> onShareClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.tricare.manuals.data.model.Manual, kotlin.Unit> onExportPdfClick) {
        super(null);
    }
    
    /**
     * Called by the Fragment whenever WorkManager reports a state change for a manual.
     */
    public final void updateProgress(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.Nullable()
    androidx.work.WorkInfo workInfo) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.tricare.manuals.ui.main.ManualAdapter.ManualViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.ui.main.ManualAdapter.ManualViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.ui.main.ManualAdapter.ManualViewHolder holder, int position, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Object> payloads) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/tricare/manuals/ui/main/ManualAdapter$ManualDiffCallback;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/tricare/manuals/data/model/Manual;", "()V", "areContentsTheSame", "", "oldItem", "newItem", "areItemsTheSame", "app_release"})
    public static final class ManualDiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.tricare.manuals.data.model.Manual> {
        
        public ManualDiffCallback() {
            super();
        }
        
        @java.lang.Override()
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull()
        com.tricare.manuals.data.model.Manual oldItem, @org.jetbrains.annotations.NotNull()
        com.tricare.manuals.data.model.Manual newItem) {
            return false;
        }
        
        @java.lang.Override()
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull()
        com.tricare.manuals.data.model.Manual oldItem, @org.jetbrains.annotations.NotNull()
        com.tricare.manuals.data.model.Manual newItem) {
            return false;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\nJ\u0010\u0010\u000b\u001a\u00020\u00062\b\u0010\t\u001a\u0004\u0018\u00010\nJ\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/tricare/manuals/ui/main/ManualAdapter$ManualViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/tricare/manuals/databinding/ItemManualBinding;", "(Lcom/tricare/manuals/ui/main/ManualAdapter;Lcom/tricare/manuals/databinding/ItemManualBinding;)V", "bind", "", "manual", "Lcom/tricare/manuals/data/model/Manual;", "workInfo", "Landroidx/work/WorkInfo;", "bindProgress", "formatEta", "", "seconds", "", "app_release"})
    public final class ManualViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final com.tricare.manuals.databinding.ItemManualBinding binding = null;
        
        public ManualViewHolder(@org.jetbrains.annotations.NotNull()
        com.tricare.manuals.databinding.ItemManualBinding binding) {
            super(null);
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        com.tricare.manuals.data.model.Manual manual, @org.jetbrains.annotations.Nullable()
        androidx.work.WorkInfo workInfo) {
        }
        
        /**
         * Updates only the progress row — called both from full bind and partial payloads.
         */
        public final void bindProgress(@org.jetbrains.annotations.Nullable()
        androidx.work.WorkInfo workInfo) {
        }
        
        private final java.lang.String formatEta(long seconds) {
            return null;
        }
    }
}