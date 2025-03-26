package com.healthtracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.*

@RunWith(AndroidJUnit4::class)
class HealthDatabaseTest {
    private lateinit var db: HealthDatabase
    private lateinit var healthEntryDao: HealthEntryDao
    private lateinit var metricValueDao: MetricValueDao
    private lateinit var metricTypeDao: MetricTypeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, HealthDatabase::class.java
        ).build()
        healthEntryDao = db.healthEntryDao()
        metricValueDao = db.metricValueDao()
        metricTypeDao = db.metricTypeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveHealthEntry() = runBlocking {
        val entry = HealthEntry(
            id = 0, // Room will auto-generate
            timestamp = Date().time,
            note = "Test entry"
        )
        
        val id = healthEntryDao.insert(entry)
        val retrievedEntry = healthEntryDao.getEntryById(id)
        
        assertNotNull(retrievedEntry)
        assertEquals(entry.timestamp, retrievedEntry?.timestamp)
        assertEquals(entry.note, retrievedEntry?.note)
    }

    @Test
    fun testInsertAndRetrieveMetricValue() = runBlocking {
        val entry = HealthEntry(0, Date().time, "Test entry")
        val entryId = healthEntryDao.insert(entry)
        
        val metricValue = MetricValue(
            id = 0, // Room will auto-generate
            entryId = entryId,
            metricType = "weight",
            value = "75.5",
            unit = "kg"
        )
        
        val id = metricValueDao.insert(metricValue)
        val retrievedValue = metricValueDao.getValueById(id)
        
        assertNotNull(retrievedValue)
        assertEquals(metricValue.metricType, retrievedValue?.metricType)
        assertEquals(metricValue.value, retrievedValue?.value)
        assertEquals(metricValue.unit, retrievedValue?.unit)
    }

    @Test
    fun testInsertAndRetrieveMetricType() = runBlocking {
        val metricType = MetricType(
            name = "weight",
            displayName = "Body Weight",
            valueType = ValueType.NUMBER,
            defaultUnit = "kg"
        )
        
        metricTypeDao.insert(metricType)
        val retrievedType = metricTypeDao.getMetricTypeByName("weight")
        
        assertNotNull(retrievedType)
        assertEquals(metricType.displayName, retrievedType?.displayName)
        assertEquals(metricType.valueType, retrievedType?.valueType)
        assertEquals(metricType.defaultUnit, retrievedType?.defaultUnit)
    }

    @Test
    fun testDeleteHealthEntry() = runBlocking {
        val entry = HealthEntry(0, Date().time, "Test entry")
        val id = healthEntryDao.insert(entry)
        
        val retrievedEntry = healthEntryDao.getEntryById(id)
        assertNotNull(retrievedEntry)
        
        healthEntryDao.delete(retrievedEntry!!)
        val deletedEntry = healthEntryDao.getEntryById(id)
        assertNull(deletedEntry)
    }

    @Test
    fun testGetEntriesWithValues() = runBlocking {
        // Insert entry
        val entry = HealthEntry(0, Date().time, "Test entry")
        val entryId = healthEntryDao.insert(entry)
        
        // Insert metric values for the entry
        val weightValue = MetricValue(0, entryId, "weight", "75.5", "kg")
        val waistValue = MetricValue(0, entryId, "waist", "85", "cm")
        metricValueDao.insert(weightValue)
        metricValueDao.insert(waistValue)
        
        // Get entries with values
        val entriesWithValues = healthEntryDao.getEntriesWithValues().first()
        
        assertEquals(1, entriesWithValues.size)
        val retrievedEntry = entriesWithValues[0]
        assertEquals(entryId, retrievedEntry.entry.id)
        assertEquals(2, retrievedEntry.values.size)
        
        val retrievedValues = retrievedEntry.values.sortedBy { it.metricType }
        assertEquals("waist", retrievedValues[0].metricType)
        assertEquals("weight", retrievedValues[1].metricType)
    }
}
