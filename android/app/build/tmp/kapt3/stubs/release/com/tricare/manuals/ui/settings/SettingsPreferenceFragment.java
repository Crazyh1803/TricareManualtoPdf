package com.tricare.manuals.ui.settings;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.tricare.manuals.R;
import com.tricare.manuals.databinding.FragmentSettingsBinding;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.File;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0002J\b\u0010\u0005\u001a\u00020\u0006H\u0003J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0002J\n\u0010\u000b\u001a\u0004\u0018\u00010\fH\u0002J\b\u0010\r\u001a\u00020\u000eH\u0003J\u001c\u0010\u000f\u001a\u00020\u00042\b\u0010\u0010\u001a\u0004\u0018\u00010\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\nH\u0016J\b\u0010\u0013\u001a\u00020\u0004H\u0002J\b\u0010\u0014\u001a\u00020\u0004H\u0002J\b\u0010\u0015\u001a\u00020\u0004H\u0002J\b\u0010\u0016\u001a\u00020\u0004H\u0002J\b\u0010\u0017\u001a\u00020\u0004H\u0002\u00a8\u0006\u0018"}, d2 = {"Lcom/tricare/manuals/ui/settings/SettingsPreferenceFragment;", "Landroidx/preference/PreferenceFragmentCompat;", "()V", "deleteAllDownloads", "", "deleteMediaStoreManuals", "", "isManualFile", "", "name", "", "legacyDownloadsDir", "Ljava/io/File;", "mediaStoreManualBytes", "", "onCreatePreferences", "savedInstanceState", "Landroid/os/Bundle;", "rootKey", "setupDarkTheme", "setupDeleteAll", "setupStorageUsed", "setupSupportDeveloper", "setupVersion", "app_release"})
public final class SettingsPreferenceFragment extends androidx.preference.PreferenceFragmentCompat {
    
    public SettingsPreferenceFragment() {
        super();
    }
    
    @java.lang.Override()
    public void onCreatePreferences(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState, @org.jetbrains.annotations.Nullable()
    java.lang.String rootKey) {
    }
    
    private final void setupDarkTheme() {
    }
    
    private final void setupStorageUsed() {
    }
    
    private final void setupDeleteAll() {
    }
    
    private final void deleteAllDownloads() {
    }
    
    private final boolean isManualFile(java.lang.String name) {
        return false;
    }
    
    private final java.io.File legacyDownloadsDir() {
        return null;
    }
    
    @androidx.annotation.RequiresApi(value = android.os.Build.VERSION_CODES.Q)
    private final long mediaStoreManualBytes() {
        return 0L;
    }
    
    @androidx.annotation.RequiresApi(value = android.os.Build.VERSION_CODES.Q)
    private final int deleteMediaStoreManuals() {
        return 0;
    }
    
    private final void setupSupportDeveloper() {
    }
    
    private final void setupVersion() {
    }
}