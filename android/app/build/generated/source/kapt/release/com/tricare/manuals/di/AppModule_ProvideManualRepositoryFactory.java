package com.tricare.manuals.di;

import com.tricare.manuals.data.db.BookmarkDao;
import com.tricare.manuals.data.db.ManualDao;
import com.tricare.manuals.data.db.SectionDao;
import com.tricare.manuals.data.network.TricareWebClient;
import com.tricare.manuals.data.network.VersionChecker;
import com.tricare.manuals.data.repository.ManualRepository;
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
public final class AppModule_ProvideManualRepositoryFactory implements Factory<ManualRepository> {
  private final Provider<ManualDao> manualDaoProvider;

  private final Provider<SectionDao> sectionDaoProvider;

  private final Provider<BookmarkDao> bookmarkDaoProvider;

  private final Provider<TricareWebClient> webClientProvider;

  private final Provider<VersionChecker> versionCheckerProvider;

  public AppModule_ProvideManualRepositoryFactory(Provider<ManualDao> manualDaoProvider,
      Provider<SectionDao> sectionDaoProvider, Provider<BookmarkDao> bookmarkDaoProvider,
      Provider<TricareWebClient> webClientProvider,
      Provider<VersionChecker> versionCheckerProvider) {
    this.manualDaoProvider = manualDaoProvider;
    this.sectionDaoProvider = sectionDaoProvider;
    this.bookmarkDaoProvider = bookmarkDaoProvider;
    this.webClientProvider = webClientProvider;
    this.versionCheckerProvider = versionCheckerProvider;
  }

  @Override
  public ManualRepository get() {
    return provideManualRepository(manualDaoProvider.get(), sectionDaoProvider.get(), bookmarkDaoProvider.get(), webClientProvider.get(), versionCheckerProvider.get());
  }

  public static AppModule_ProvideManualRepositoryFactory create(
      Provider<ManualDao> manualDaoProvider, Provider<SectionDao> sectionDaoProvider,
      Provider<BookmarkDao> bookmarkDaoProvider, Provider<TricareWebClient> webClientProvider,
      Provider<VersionChecker> versionCheckerProvider) {
    return new AppModule_ProvideManualRepositoryFactory(manualDaoProvider, sectionDaoProvider, bookmarkDaoProvider, webClientProvider, versionCheckerProvider);
  }

  public static ManualRepository provideManualRepository(ManualDao manualDao, SectionDao sectionDao,
      BookmarkDao bookmarkDao, TricareWebClient webClient, VersionChecker versionChecker) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideManualRepository(manualDao, sectionDao, bookmarkDao, webClient, versionChecker));
  }
}
