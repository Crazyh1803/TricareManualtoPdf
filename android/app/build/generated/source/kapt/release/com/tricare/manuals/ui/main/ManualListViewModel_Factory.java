package com.tricare.manuals.ui.main;

import android.content.Context;
import com.tricare.manuals.data.repository.ManualRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ManualListViewModel_Factory implements Factory<ManualListViewModel> {
  private final Provider<ManualRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public ManualListViewModel_Factory(Provider<ManualRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ManualListViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static ManualListViewModel_Factory create(Provider<ManualRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new ManualListViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static ManualListViewModel newInstance(ManualRepository repository, Context context) {
    return new ManualListViewModel(repository, context);
  }
}
