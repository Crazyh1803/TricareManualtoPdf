package com.tricare.manuals.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.tricare.manuals.R;
import com.tricare.manuals.data.model.Manual;
import com.tricare.manuals.data.repository.ManualRepository;
import com.tricare.manuals.databinding.FragmentManualListBinding;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0002J\b\u0010\u0012\u001a\u00020\u0011H\u0002J$\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0016J\b\u0010\u001b\u001a\u00020\u0011H\u0016J\u001a\u0010\u001c\u001a\u00020\u00112\u0006\u0010\u001d\u001a\u00020\u00142\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0016J\u0010\u0010\u001e\u001a\u00020\u00112\u0006\u0010\u001f\u001a\u00020 H\u0002J\b\u0010!\u001a\u00020\u0011H\u0002J\b\u0010\"\u001a\u00020\u0011H\u0002J\b\u0010#\u001a\u00020\u0011H\u0002J\u0010\u0010$\u001a\u00020\u00112\u0006\u0010%\u001a\u00020&H\u0002J\b\u0010\'\u001a\u00020\u0011H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\tR\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\r\u00a8\u0006("}, d2 = {"Lcom/tricare/manuals/ui/main/ManualListFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/tricare/manuals/databinding/FragmentManualListBinding;", "adapter", "Lcom/tricare/manuals/ui/main/ManualAdapter;", "binding", "getBinding", "()Lcom/tricare/manuals/databinding/FragmentManualListBinding;", "viewModel", "Lcom/tricare/manuals/ui/main/ManualListViewModel;", "getViewModel", "()Lcom/tricare/manuals/ui/main/ManualListViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "observeDownloadProgress", "", "observeViewModel", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "openUrl", "url", "", "setupBmcButton", "setupMenu", "setupRecyclerView", "shareManual", "manual", "Lcom/tricare/manuals/data/model/Manual;", "showBirthdayDialog", "app_debug"})
public final class ManualListFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable()
    private com.tricare.manuals.databinding.FragmentManualListBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.tricare.manuals.ui.main.ManualAdapter adapter;
    
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
     * Observe WorkManager for each known manual code and forward state to the adapter.
     */
    private final void observeDownloadProgress() {
    }
    
    private final void shareManual(com.tricare.manuals.data.model.Manual manual) {
    }
    
    private final void setupBmcButton() {
    }
    
    private final void setupMenu() {
    }
    
    private final void observeViewModel() {
    }
    
    private final void showBirthdayDialog() {
    }
    
    private final void openUrl(java.lang.String url) {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
}