package com.healthtracker.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\bJ\u000e\u0010\u0010\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\bJ\b\u0010\u0011\u001a\u00020\u000eH\u0002J\u000e\u0010\u0012\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\bR\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0013"}, d2 = {"Lcom/healthtracker/ui/HealthTrackerViewModel;", "Landroidx/lifecycle/ViewModel;", "database", "Lcom/healthtracker/data/HealthDatabase;", "(Lcom/healthtracker/data/HealthDatabase;)V", "_entries", "Landroidx/lifecycle/MutableLiveData;", "", "Lcom/healthtracker/data/HealthEntry;", "entries", "Landroidx/lifecycle/LiveData;", "getEntries", "()Landroidx/lifecycle/LiveData;", "addEntry", "", "entry", "deleteEntry", "loadEntries", "updateEntry", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class HealthTrackerViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.healthtracker.data.HealthDatabase database = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.lifecycle.MutableLiveData<java.util.List<com.healthtracker.data.HealthEntry>> _entries = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.lifecycle.LiveData<java.util.List<com.healthtracker.data.HealthEntry>> entries = null;
    
    @javax.inject.Inject
    public HealthTrackerViewModel(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.HealthDatabase database) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final androidx.lifecycle.LiveData<java.util.List<com.healthtracker.data.HealthEntry>> getEntries() {
        return null;
    }
    
    private final void loadEntries() {
    }
    
    public final void addEntry(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.HealthEntry entry) {
    }
    
    public final void updateEntry(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.HealthEntry entry) {
    }
    
    public final void deleteEntry(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.HealthEntry entry) {
    }
}