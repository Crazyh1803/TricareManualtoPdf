package com.tricare.manuals.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.tricare.manuals.data.db.ManualDao;
import com.tricare.manuals.data.db.SectionDao;
import com.tricare.manuals.data.network.TocParser;
import com.tricare.manuals.data.network.TricareWebClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DownloadWorker_Factory {
  private final Provider<TricareWebClient> webClientProvider;

  private final Provider<TocParser> tocParserProvider;

  private final Provider<ManualDao> manualDaoProvider;

  private final Provider<SectionDao> sectionDaoProvider;

  public DownloadWorker_Factory(Provider<TricareWebClient> webClientProvider,
      Provider<TocParser> tocParserProvider, Provider<ManualDao> manualDaoProvider,
      Provider<SectionDao> sectionDaoProvider) {
    this.webClientProvider = webClientProvider;
    this.tocParserProvider = tocParserProvider;
    this.manualDaoProvider = manualDaoProvider;
    this.sectionDaoProvider = sectionDaoProvider;
  }

  public DownloadWorker get(Context appContext, WorkerParameters params) {
    return newInstance(appContext, params, webClientProvider.get(), tocParserProvider.get(), manualDaoProvider.get(), sectionDaoProvider.get());
  }

  public static DownloadWorker_Factory create(Provider<TricareWebClient> webClientProvider,
      Provider<TocParser> tocParserProvider, Provider<ManualDao> manualDaoProvider,
      Provider<SectionDao> sectionDaoProvider) {
    return new DownloadWorker_Factory(webClientProvider, tocParserProvider, manualDaoProvider, sectionDaoProvider);
  }

  public static DownloadWorker newInstance(Context appContext, WorkerParameters params,
      TricareWebClient webClient, TocParser tocParser, ManualDao manualDao, SectionDao sectionDao) {
    return new DownloadWorker(appContext, params, webClient, tocParser, manualDao, sectionDao);
  }
}
