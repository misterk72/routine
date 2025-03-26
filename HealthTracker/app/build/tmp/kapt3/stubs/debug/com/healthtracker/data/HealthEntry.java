package com.healthtracker.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B;\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0016\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0005H\u00c6\u0003J\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0013J\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0013J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\nH\u00c6\u0003JF\u0010\u001b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\nH\u00c6\u0001\u00a2\u0006\u0002\u0010\u001cJ\u0013\u0010\u001d\u001a\u00020\u001e2\b\u0010\u001f\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010 \u001a\u00020!H\u00d6\u0001J\t\u0010\"\u001a\u00020\nH\u00d6\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0015\u0010\b\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\n\n\u0002\u0010\u0014\u001a\u0004\b\u0012\u0010\u0013R\u0015\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\n\n\u0002\u0010\u0014\u001a\u0004\b\u0015\u0010\u0013\u00a8\u0006#"}, d2 = {"Lcom/healthtracker/data/HealthEntry;", "", "id", "", "timestamp", "Ljava/time/LocalDateTime;", "weight", "", "waistMeasurement", "notes", "", "(JLjava/time/LocalDateTime;Ljava/lang/Float;Ljava/lang/Float;Ljava/lang/String;)V", "getId", "()J", "getNotes", "()Ljava/lang/String;", "getTimestamp", "()Ljava/time/LocalDateTime;", "getWaistMeasurement", "()Ljava/lang/Float;", "Ljava/lang/Float;", "getWeight", "component1", "component2", "component3", "component4", "component5", "copy", "(JLjava/time/LocalDateTime;Ljava/lang/Float;Ljava/lang/Float;Ljava/lang/String;)Lcom/healthtracker/data/HealthEntry;", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
@androidx.room.Entity(tableName = "health_entries")
public final class HealthEntry {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final long id = 0L;
    @org.jetbrains.annotations.NotNull
    private final java.time.LocalDateTime timestamp = null;
    @org.jetbrains.annotations.Nullable
    private final java.lang.Float weight = null;
    @org.jetbrains.annotations.Nullable
    private final java.lang.Float waistMeasurement = null;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String notes = null;
    
    public HealthEntry(long id, @org.jetbrains.annotations.NotNull
    java.time.LocalDateTime timestamp, @org.jetbrains.annotations.Nullable
    java.lang.Float weight, @org.jetbrains.annotations.Nullable
    java.lang.Float waistMeasurement, @org.jetbrains.annotations.Nullable
    java.lang.String notes) {
        super();
    }
    
    public final long getId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.time.LocalDateTime getTimestamp() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Float getWeight() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Float getWaistMeasurement() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getNotes() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.time.LocalDateTime component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Float component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Float component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.healthtracker.data.HealthEntry copy(long id, @org.jetbrains.annotations.NotNull
    java.time.LocalDateTime timestamp, @org.jetbrains.annotations.Nullable
    java.lang.Float weight, @org.jetbrains.annotations.Nullable
    java.lang.Float waistMeasurement, @org.jetbrains.annotations.Nullable
    java.lang.String notes) {
        return null;
    }
    
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public java.lang.String toString() {
        return null;
    }
}