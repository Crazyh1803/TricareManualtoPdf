package com.tricare.manuals.ui.main;

import android.content.Context;
import androidx.lifecycle.ViewModel;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkManager;
import com.tricare.manuals.data.model.Manual;
import com.tricare.manuals.data.network.NtpClient;
import com.tricare.manuals.data.repository.ManualRepository;
import com.tricare.manuals.worker.DownloadWorker;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.StateFlow;
import java.util.Calendar;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0018\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\"\u0014\u0010\u0000\u001a\b\u0012\u0004\u0012\u00020\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\"\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00040\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\"\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"FORMAT_KEY", "Landroidx/datastore/preferences/core/Preferences$Key;", "", "LAST_BIRTHDAY_YEAR_KEY", "", "WIFI_ONLY_KEY", "", "app_debug"})
public final class ManualListViewModelKt {
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.String> FORMAT_KEY = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Boolean> WIFI_ONLY_KEY = null;
    @org.jetbrains.annotations.NotNull()
    private static final androidx.datastore.preferences.core.Preferences.Key<java.lang.Integer> LAST_BIRTHDAY_YEAR_KEY = null;
}