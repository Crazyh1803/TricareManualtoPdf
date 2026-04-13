package com.tricare.manuals.ui.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.tricare.manuals.data.repository.ManualRepository;
import com.tricare.manuals.databinding.FragmentDownloadDialogBinding;
import com.tricare.manuals.worker.DownloadWorker;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 ,2\u00020\u0001:\u0001,B\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u000e\u001a\u00020\u000f2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u00112\u0006\u0010\u0013\u001a\u00020\u0012H\u0082@\u00a2\u0006\u0002\u0010\u0014J\u0010\u0010\u0015\u001a\u00020\u00122\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\b\u0010\u0018\u001a\u00020\u0019H\u0002J$\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001f2\b\u0010 \u001a\u0004\u0018\u00010!H\u0016J\b\u0010\"\u001a\u00020\u000fH\u0016J\u001a\u0010#\u001a\u00020\u000f2\u0006\u0010$\u001a\u00020\u001b2\b\u0010 \u001a\u0004\u0018\u00010!H\u0016J\b\u0010%\u001a\u00020\u000fH\u0002J\b\u0010&\u001a\u00020\u000fH\u0002J\b\u0010\'\u001a\u00020\u000fH\u0002J\u0018\u0010(\u001a\u00020\u000f2\u0006\u0010)\u001a\u00020\u00122\u0006\u0010*\u001a\u00020+H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u001e\u0010\b\u001a\u00020\t8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\r\u00a8\u0006-"}, d2 = {"Lcom/tricare/manuals/ui/download/DownloadDialogFragment;", "Lcom/google/android/material/bottomsheet/BottomSheetDialogFragment;", "()V", "_binding", "Lcom/tricare/manuals/databinding/FragmentDownloadDialogBinding;", "binding", "getBinding", "()Lcom/tricare/manuals/databinding/FragmentDownloadDialogBinding;", "repository", "Lcom/tricare/manuals/data/repository/ManualRepository;", "getRepository", "()Lcom/tricare/manuals/data/repository/ManualRepository;", "setRepository", "(Lcom/tricare/manuals/data/repository/ManualRepository;)V", "enqueueDownloads", "", "codes", "", "", "format", "(Ljava/util/List;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "formatEta", "seconds", "", "isOnWifi", "", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "setupCheckboxes", "setupDownloadButton", "showProgressSection", "updateProgressForManual", "code", "workInfo", "Landroidx/work/WorkInfo;", "Companion", "app_debug"})
public final class DownloadDialogFragment extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {
    @javax.inject.Inject()
    public com.tricare.manuals.data.repository.ManualRepository repository;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TAG = "DownloadDialogFragment";
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> WIFI_ONLY_KEY = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.String> FORMAT_KEY = null;
    @org.jetbrains.annotations.Nullable()
    private com.tricare.manuals.databinding.FragmentDownloadDialogBinding _binding;
    @org.jetbrains.annotations.NotNull()
    public static final com.tricare.manuals.ui.download.DownloadDialogFragment.Companion Companion = null;
    
    public DownloadDialogFragment() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.tricare.manuals.data.repository.ManualRepository getRepository() {
        return null;
    }
    
    public final void setRepository(@org.jetbrains.annotations.NotNull()
    com.tricare.manuals.data.repository.ManualRepository p0) {
    }
    
    private final com.tricare.manuals.databinding.FragmentDownloadDialogBinding getBinding() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public android.view.View onCreateView(@org.jetbrains.annotations.NotNull()
    android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable()
    android.view.ViewGroup container, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
        return null;
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupCheckboxes() {
    }
    
    private final void setupDownloadButton() {
    }
    
    private final java.lang.Object enqueueDownloads(java.util.List<java.lang.String> codes, java.lang.String format, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void updateProgressForManual(java.lang.String code, androidx.work.WorkInfo workInfo) {
    }
    
    private final void showProgressSection() {
    }
    
    private final java.lang.String formatEta(long seconds) {
        return null;
    }
    
    private final boolean isOnWifi() {
        return false;
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\t\u001a\u00020\nR\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/tricare/manuals/ui/download/DownloadDialogFragment$Companion;", "", "()V", "FORMAT_KEY", "Landroidx/datastore/preferences/core/Preferences$Key;", "", "TAG", "WIFI_ONLY_KEY", "", "newInstance", "Lcom/tricare/manuals/ui/download/DownloadDialogFragment;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.tricare.manuals.ui.download.DownloadDialogFragment newInstance() {
            return null;
        }
    }
}