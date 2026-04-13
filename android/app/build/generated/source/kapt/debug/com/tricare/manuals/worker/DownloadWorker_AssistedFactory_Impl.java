package com.tricare.manuals.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DownloadWorker_AssistedFactory_Impl implements DownloadWorker_AssistedFactory {
  private final DownloadWorker_Factory delegateFactory;

  DownloadWorker_AssistedFactory_Impl(DownloadWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public DownloadWorker create(Context arg0, WorkerParameters arg1) {
    return delegateFactory.get(arg0, arg1);
  }

  public static Provider<DownloadWorker_AssistedFactory> create(
      DownloadWorker_Factory delegateFactory) {
    return InstanceFactory.create(new DownloadWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<DownloadWorker_AssistedFactory> createFactoryProvider(
      DownloadWorker_Factory delegateFactory) {
    return InstanceFactory.create(new DownloadWorker_AssistedFactory_Impl(delegateFactory));
  }
}
