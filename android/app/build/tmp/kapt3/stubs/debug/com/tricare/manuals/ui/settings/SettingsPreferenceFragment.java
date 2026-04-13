package com.tricare.manuals.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.tricare.manuals.R;
import com.tricare.manuals.databinding.FragmentSettingsBinding;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0002J\u001c\u0010\u0005\u001a\u00020\u00042\b\u0010\u0006\u001a\u0004\u0018\u00010\u00072\b\u0010\b\u001a\u0004\u0018\u00010\tH\u0016J\b\u0010\n\u001a\u00020\u0004H\u0002J\b\u0010\u000b\u001a\u00020\u0004H\u0002\u00a8\u0006\f"}, d2 = {"Lcom/tricare/manuals/ui/settings/SettingsPreferenceFragment;", "Landroidx/preference/PreferenceFragmentCompat;", "()V", "deleteAllDownloads", "", "onCreatePreferences", "savedInstanceState", "Landroid/os/Bundle;", "rootKey", "", "setupDeleteAll", "setupStorageUsed", "app_debug"})
public final class SettingsPreferenceFragment extends androidx.preference.PreferenceFragmentCompat {
    
    public SettingsPreferenceFragment() {
        super();
    }
    
    @java.lang.Override()
    public void onCreatePreferences(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState, @org.jetbrains.annotations.Nullable()
    java.lang.String rootKey) {
    }
    
    private final void setupStorageUsed() {
    }
    
    private final void setupDeleteAll() {
    }
    
    private final void deleteAllDownloads() {
    }
}