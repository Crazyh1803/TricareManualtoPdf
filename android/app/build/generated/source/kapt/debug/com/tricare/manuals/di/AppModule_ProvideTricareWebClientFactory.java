package com.tricare.manuals.di;

import com.tricare.manuals.data.network.TricareWebClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideTricareWebClientFactory implements Factory<TricareWebClient> {
  @Override
  public TricareWebClient get() {
    return provideTricareWebClient();
  }

  public static AppModule_ProvideTricareWebClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TricareWebClient provideTricareWebClient() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTricareWebClient());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideTricareWebClientFactory INSTANCE = new AppModule_ProvideTricareWebClientFactory();
  }
}
