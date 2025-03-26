package com.healthtracker.ui;

import com.healthtracker.data.HealthDatabase;
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
public final class HealthTrackerViewModel_Factory implements Factory<HealthTrackerViewModel> {
  private final Provider<HealthDatabase> databaseProvider;

  public HealthTrackerViewModel_Factory(Provider<HealthDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public HealthTrackerViewModel get() {
    return newInstance(databaseProvider.get());
  }

  public static HealthTrackerViewModel_Factory create(Provider<HealthDatabase> databaseProvider) {
    return new HealthTrackerViewModel_Factory(databaseProvider);
  }

  public static HealthTrackerViewModel newInstance(HealthDatabase database) {
    return new HealthTrackerViewModel(database);
  }
}
