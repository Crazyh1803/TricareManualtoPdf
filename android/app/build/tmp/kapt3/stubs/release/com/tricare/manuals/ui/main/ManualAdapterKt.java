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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\b\n\u0000\n\u0002\u0010\u000e\n\u0000\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0002"}, d2 = {"PAYLOAD_PROGRESS", "", "app_release"})
public final class ManualAdapterKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PAYLOAD_PROGRESS = "progress";
}