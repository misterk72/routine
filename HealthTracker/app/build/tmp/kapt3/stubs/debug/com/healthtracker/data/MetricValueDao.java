package com.healthtracker.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\u0019\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0006J\u001b\u0010\u0007\u001a\u0004\u0018\u00010\u00052\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\r0\f2\u0006\u0010\u000e\u001a\u00020\u000fH\'J\u001c\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\r0\f2\u0006\u0010\u0011\u001a\u00020\tH\'J\u0019\u0010\u0012\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0006J\u0019\u0010\u0013\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0006\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0014"}, d2 = {"Lcom/healthtracker/data/MetricValueDao;", "", "delete", "", "value", "Lcom/healthtracker/data/MetricValue;", "(Lcom/healthtracker/data/MetricValue;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getValueById", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getValuesByType", "Lkotlinx/coroutines/flow/Flow;", "", "metricType", "", "getValuesForEntry", "entryId", "insert", "update", "app_debug"})
@androidx.room.Dao
public abstract interface MetricValueDao {
    
    @androidx.room.Insert
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.MetricValue value, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.MetricValue value, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull
    com.healthtracker.data.MetricValue value, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM metric_values WHERE entryId = :entryId")
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.healthtracker.data.MetricValue>> getValuesForEntry(long entryId);
    
    @androidx.room.Query(value = "SELECT * FROM metric_values WHERE metricType = :metricType")
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.healthtracker.data.MetricValue>> getValuesByType(@org.jetbrains.annotations.NotNull
    java.lang.String metricType);
    
    @androidx.room.Query(value = "SELECT * FROM metric_values WHERE id = :id")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getValueById(long id, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.healthtracker.data.MetricValue> $completion);
}