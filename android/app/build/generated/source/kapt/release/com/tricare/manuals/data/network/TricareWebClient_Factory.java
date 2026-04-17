package com.tricare.manuals.data.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TricareWebClient_Factory implements Factory<TricareWebClient> {
  @Override
  public TricareWebClient get() {
    return newInstance();
  }

  public static TricareWebClient_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TricareWebClient newInstance() {
    return new TricareWebClient();
  }

  private static final class InstanceHolder {
    private static final TricareWebClient_Factory INSTANCE = new TricareWebClient_Factory();
  }
}
