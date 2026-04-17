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
public final class TocParser_Factory implements Factory<TocParser> {
  @Override
  public TocParser get() {
    return newInstance();
  }

  public static TocParser_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TocParser newInstance() {
    return new TocParser();
  }

  private static final class InstanceHolder {
    private static final TocParser_Factory INSTANCE = new TocParser_Factory();
  }
}
