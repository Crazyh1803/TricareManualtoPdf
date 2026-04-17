package com.tricare.manuals;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TricareApp_MembersInjector implements MembersInjector<TricareApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public TricareApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<TricareApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new TricareApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(TricareApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.tricare.manuals.TricareApp.workerFactory")
  public static void injectWorkerFactory(TricareApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
