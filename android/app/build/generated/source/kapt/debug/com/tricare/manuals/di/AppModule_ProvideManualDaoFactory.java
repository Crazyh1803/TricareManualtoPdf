package com.tricare.manuals.di;

import com.tricare.manuals.data.db.AppDatabase;
import com.tricare.manuals.data.db.ManualDao;
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
public final class AppModule_ProvideManualDaoFactory implements Factory<ManualDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideManualDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ManualDao get() {
    return provideManualDao(dbProvider.get());
  }

  public static AppModule_ProvideManualDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideManualDaoFactory(dbProvider);
  }

  public static ManualDao provideManualDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideManualDao(db));
  }
}
