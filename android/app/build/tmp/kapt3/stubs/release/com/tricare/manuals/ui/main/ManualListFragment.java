package com.tricare.manuals.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.tricare.manuals.R;
import com.tricare.manuals.data.model.Manual;
import com.tricare.manuals.data.repository.ManualRepository;
import com.tricare.manuals.databinding.FragmentManualListBinding;
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.Dispatchers;
import java.io.File;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010#\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0018\u001a\u00020\u000b2\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u000bH\u0002J\u0010\u0010\u001c\u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u000bH\u0002J\u0010\u0010\u001e\u001a\u00020\u001f2\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\u0010\u0010 \u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u000bH\u0002J\b\u0010!\u001a\u00020\u001fH\u0002J\b\u0010\"\u001a\u00020\u001fH\u0002J$\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020&2\b\u0010\'\u001a\u0004\u0018\u00010(2\b\u0010)\u001a\u0004\u0018\u00010*H\u0016J\b\u0010+\u001a\u00020\u001fH\u0016J\u001a\u0010,\u001a\u00020\u001f2\u0006\u0010-\u001a\u00020$2\b\u0010)\u001a\u0004\u0018\u00010*H\u0016J\b\u0010.\u001a\u00020\u001fH\u0002J\b\u0010/\u001a\u00020\u001fH\u0002J\u0010\u00100\u001a\u00020\u001f2\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u00101\u001a\u00020\u001fH\u0002J\u0010\u00102\u001a\u00020\u001f2\u0006\u00103\u001a\u00020\u000bH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\tR\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012R\u001c\u0010\u0015\u001a\u0010\u0012\f\u0012\n \u0017*\u0004\u0018\u00010\u000b0\u000b0\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00064"}, d2 = {"Lcom/tricare/manuals/ui/main/ManualListFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/tricare/manuals/databinding/FragmentManualListBinding;", "adapter", "Lcom/tricare/manuals/ui/main/ManualAdapter;", "binding", "getBinding", "()Lcom/tricare/manuals/databinding/FragmentManualListBinding;", "pendingDownloadCode", "", "toastedWorkIds", "", "Ljava/util/UUID;", "viewModel", "Lcom/tricare/manuals/ui/main/ManualListViewModel;", "getViewModel", "()Lcom/tricare/manuals/ui/main/ManualListViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "writePermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "kotlin.jvm.PlatformType", "buildHtmlForPdf", "manual", "Lcom/tricare/manuals/data/model/Manual;", "md", "esc", "s", "exportAsPdf", "", "inline", "observeDownloadProgress", "observeViewModel", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "setupMenu", "setupRecyclerView", "shareManual", "showBirthdayDialog", "startDownload", "code", "app_release"})
public final class ManualListFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable()
    private com.tricare.manuals.databinding.FragmentManualListBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.tricare.manuals.ui.main.ManualAdapter adapter;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.util.UUID> toastedWorkIds = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String pendingDownloadCode;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> writePermissionLauncher = null;
    
    public ManualListFragment() {
        super();
    }
    
    private final com.tricare.manuals.databinding.FragmentManualListBinding getBinding() {
        return null;
    }
    
    private final com.tricare.manuals.ui.main.ManualListViewModel getViewModel() {
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
    
    private final void setupRecyclerView() {
    }
    
    /**
     * On Android 10+ the MediaStore.Downloads API requires no permission.
     * On Android 9 and below we need WRITE_EXTERNAL_STORAGE — ask for it here,
     * before the worker even starts, so the worker never hits a permission error.
     */
    private final void startDownload(java.lang.String code) {
    }
    
    private final void observeDownloadProgress() {
    }
    
    private final void exportAsPdf(com.tricare.manuals.data.model.Manual manual) {
    }
    
    private final java.lang.String buildHtmlForPdf(com.tricare.manuals.data.model.Manual manual, java.lang.String md) {
        return null;
    }
    
    private final java.lang.String esc(java.lang.String s) {
        return null;
    }
    
    private final java.lang.String inline(java.lang.String s) {
        return null;
    }
    
    private final void shareManual(com.tricare.manuals.data.model.Manual manual) {
    }
    
    private final void setupMenu() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void showBirthdayDialog() {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}