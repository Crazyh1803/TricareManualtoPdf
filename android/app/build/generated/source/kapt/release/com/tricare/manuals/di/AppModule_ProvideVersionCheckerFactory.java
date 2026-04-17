package com.tricare.manuals.di;

import com.tricare.manuals.data.network.TricareWebClient;
import com.tricare.manuals.data.network.VersionChecker;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideVersionCheckerFactory implements Factory<VersionChecker> {
  private final Provider<TricareWebClient> webClientProvider;

  public AppModule_ProvideVersionCheckerFactory(Provider<TricareWebClient> webClientProvider) {
    this.webClientProvider = webClientProvider;
  }

  @Override
  public VersionChecker get() {
    return provideVersionChecker(webClientProvider.get());
  }

  public static AppModule_ProvideVersionCheckerFactory create(
      Provider<TricareWebClient> webClientProvider) {
    return new AppModule_ProvideVersionCheckerFactory(webClientProvider);
  }

  public static VersionChecker provideVersionChecker(TricareWebClient webClient) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideVersionChecker(webClient));
  }
}
