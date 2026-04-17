package com.tricare.manuals.di;

import com.tricare.manuals.data.db.AppDatabase;
import com.tricare.manuals.data.db.SectionDao;
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
public final class AppModule_ProvideSectionDaoFactory implements Factory<SectionDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideSectionDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SectionDao get() {
    return provideSectionDao(dbProvider.get());
  }

  public static AppModule_ProvideSectionDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideSectionDaoFactory(dbProvider);
  }

  public static SectionDao provideSectionDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSectionDao(db));
  }
}
