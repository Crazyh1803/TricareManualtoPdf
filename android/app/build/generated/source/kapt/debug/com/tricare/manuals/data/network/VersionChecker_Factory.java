package com.tricare.manuals.data.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class VersionChecker_Factory implements Factory<VersionChecker> {
  private final Provider<TricareWebClient> webClientProvider;

  public VersionChecker_Factory(Provider<TricareWebClient> webClientProvider) {
    this.webClientProvider = webClientProvider;
  }

  @Override
  public VersionChecker get() {
    return newInstance(webClientProvider.get());
  }

  public static VersionChecker_Factory create(Provider<TricareWebClient> webClientProvider) {
    return new VersionChecker_Factory(webClientProvider);
  }

  public static VersionChecker newInstance(TricareWebClient webClient) {
    return new VersionChecker(webClient);
  }
}
