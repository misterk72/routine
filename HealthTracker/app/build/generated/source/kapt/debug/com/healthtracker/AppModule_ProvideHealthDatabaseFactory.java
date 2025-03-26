package com.healthtracker;

import android.content.Context;
import com.healthtracker.data.HealthDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideHealthDatabaseFactory implements Factory<HealthDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideHealthDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HealthDatabase get() {
    return provideHealthDatabase(contextProvider.get());
  }

  public static AppModule_ProvideHealthDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideHealthDatabaseFactory(contextProvider);
  }

  public static HealthDatabase provideHealthDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideHealthDatabase(context));
  }
}
