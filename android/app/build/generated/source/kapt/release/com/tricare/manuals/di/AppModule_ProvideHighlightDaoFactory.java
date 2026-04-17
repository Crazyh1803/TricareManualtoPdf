package com.tricare.manuals.di;

import com.tricare.manuals.data.db.AppDatabase;
import com.tricare.manuals.data.db.HighlightDao;
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
public final class AppModule_ProvideHighlightDaoFactory implements Factory<HighlightDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideHighlightDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HighlightDao get() {
    return provideHighlightDao(dbProvider.get());
  }

  public static AppModule_ProvideHighlightDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideHighlightDaoFactory(dbProvider);
  }

  public static HighlightDao provideHighlightDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHighlightDao(db));
  }
}
