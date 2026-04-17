package com.tricare.manuals.di;

import com.tricare.manuals.data.db.AppDatabase;
import com.tricare.manuals.data.db.BookmarkDao;
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
public final class AppModule_ProvideBookmarkDaoFactory implements Factory<BookmarkDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideBookmarkDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BookmarkDao get() {
    return provideBookmarkDao(dbProvider.get());
  }

  public static AppModule_ProvideBookmarkDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideBookmarkDaoFactory(dbProvider);
  }

  public static BookmarkDao provideBookmarkDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBookmarkDao(db));
  }
}
