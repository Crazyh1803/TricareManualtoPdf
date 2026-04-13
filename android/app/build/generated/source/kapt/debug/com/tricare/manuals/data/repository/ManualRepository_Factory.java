package com.tricare.manuals.data.repository;

import com.tricare.manuals.data.db.BookmarkDao;
import com.tricare.manuals.data.db.ManualDao;
import com.tricare.manuals.data.db.SectionDao;
import com.tricare.manuals.data.network.TricareWebClient;
import com.tricare.manuals.data.network.VersionChecker;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ManualRepository_Factory implements Factory<ManualRepository> {
  private final Provider<ManualDao> manualDaoProvider;

  private final Provider<SectionDao> sectionDaoProvider;

  private final Provider<BookmarkDao> bookmarkDaoProvider;

  private final Provider<TricareWebClient> webClientProvider;

  private final Provider<VersionChecker> versionCheckerProvider;

  public ManualRepository_Factory(Provider<ManualDao> manualDaoProvider,
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
    return newInstance(manualDaoProvider.get(), sectionDaoProvider.get(), bookmarkDaoProvider.get(), webClientProvider.get(), versionCheckerProvider.get());
  }

  public static ManualRepository_Factory create(Provider<ManualDao> manualDaoProvider,
      Provider<SectionDao> sectionDaoProvider, Provider<BookmarkDao> bookmarkDaoProvider,
      Provider<TricareWebClient> webClientProvider,
      Provider<VersionChecker> versionCheckerProvider) {
    return new ManualRepository_Factory(manualDaoProvider, sectionDaoProvider, bookmarkDaoProvider, webClientProvider, versionCheckerProvider);
  }

  public static ManualRepository newInstance(ManualDao manualDao, SectionDao sectionDao,
      BookmarkDao bookmarkDao, TricareWebClient webClient, VersionChecker versionChecker) {
    return new ManualRepository(manualDao, sectionDao, bookmarkDao, webClient, versionChecker);
  }
}
