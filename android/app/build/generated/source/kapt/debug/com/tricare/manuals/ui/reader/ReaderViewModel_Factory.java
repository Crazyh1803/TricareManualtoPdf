package com.tricare.manuals.ui.reader;

import com.tricare.manuals.data.db.HighlightDao;
import com.tricare.manuals.data.repository.ManualRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ReaderViewModel_Factory implements Factory<ReaderViewModel> {
  private final Provider<ManualRepository> repositoryProvider;

  private final Provider<HighlightDao> highlightDaoProvider;

  public ReaderViewModel_Factory(Provider<ManualRepository> repositoryProvider,
      Provider<HighlightDao> highlightDaoProvider) {
    this.repositoryProvider = repositoryProvider;
    this.highlightDaoProvider = highlightDaoProvider;
  }

  @Override
  public ReaderViewModel get() {
    return newInstance(repositoryProvider.get(), highlightDaoProvider.get());
  }

  public static ReaderViewModel_Factory create(Provider<ManualRepository> repositoryProvider,
      Provider<HighlightDao> highlightDaoProvider) {
    return new ReaderViewModel_Factory(repositoryProvider, highlightDaoProvider);
  }

  public static ReaderViewModel newInstance(ManualRepository repository,
      HighlightDao highlightDao) {
    return new ReaderViewModel(repository, highlightDao);
  }
}
