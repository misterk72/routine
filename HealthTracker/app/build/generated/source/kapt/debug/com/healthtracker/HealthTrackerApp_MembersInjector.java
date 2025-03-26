package com.healthtracker;

import androidx.hilt.work.HiltWorkerFactory;
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
public final class HealthTrackerApp_MembersInjector implements MembersInjector<HealthTrackerApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public HealthTrackerApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<HealthTrackerApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new HealthTrackerApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(HealthTrackerApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.healthtracker.HealthTrackerApp.workerFactory")
  public static void injectWorkerFactory(HealthTrackerApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
