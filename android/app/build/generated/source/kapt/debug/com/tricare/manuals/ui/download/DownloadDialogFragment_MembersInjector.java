package com.tricare.manuals.ui.download;

import com.tricare.manuals.data.repository.ManualRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DownloadDialogFragment_MembersInjector implements MembersInjector<DownloadDialogFragment> {
  private final Provider<ManualRepository> repositoryProvider;

  public DownloadDialogFragment_MembersInjector(Provider<ManualRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public static MembersInjector<DownloadDialogFragment> create(
      Provider<ManualRepository> repositoryProvider) {
    return new DownloadDialogFragment_MembersInjector(repositoryProvider);
  }

  @Override
  public void injectMembers(DownloadDialogFragment instance) {
    injectRepository(instance, repositoryProvider.get());
  }

  @InjectedFieldSignature("com.tricare.manuals.ui.download.DownloadDialogFragment.repository")
  public static void injectRepository(DownloadDialogFragment instance,
      ManualRepository repository) {
    instance.repository = repository;
  }
}
