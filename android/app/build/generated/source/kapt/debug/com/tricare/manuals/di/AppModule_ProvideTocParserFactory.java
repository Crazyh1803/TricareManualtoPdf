package com.tricare.manuals.di;

import com.tricare.manuals.data.network.TocParser;
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
public final class AppModule_ProvideTocParserFactory implements Factory<TocParser> {
  @Override
  public TocParser get() {
    return provideTocParser();
  }

  public static AppModule_ProvideTocParserFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TocParser provideTocParser() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTocParser());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideTocParserFactory INSTANCE = new AppModule_ProvideTocParserFactory();
  }
}
