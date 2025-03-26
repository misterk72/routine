package com.healthtracker.data

import org.junit.Test
import org.junit.Assert.*

class MetricValueTest {
    
    @Test
    fun `test MetricValue creation with valid data`() {
        val metricValue = MetricValue(
            id = 1,
            entryId = 100,
            metricType = "weight",
            value = 75.5,
            unit = "kg",
            notes = "Test note"
        )
        
        assertEquals(1, metricValue.id)
        assertEquals(100, metricValue.entryId)
        assertEquals("weight", metricValue.metricType)
        assertEquals(75.5, metricValue.value, 0.001)
        assertEquals("kg", metricValue.unit)
        assertEquals("Test note", metricValue.notes)
    }
    
    @Test
    fun `test MetricValue with null optional fields`() {
        val metricValue = MetricValue(
            id = 1,
            entryId = 100,
            metricType = "weight",
            value = 75.5,
            unit = null,
            notes = null
        )
        
        assertNull(metricValue.unit)
        assertNull(metricValue.notes)
        assertEquals(75.5, metricValue.value, 0.001)
    }
    
    @Test
    fun `test MetricValue equality`() {
        val value1 = MetricValue(1, 100, "weight", 75.5, "kg")
        val value2 = MetricValue(1, 100, "weight", 75.5, "kg")
        val value3 = MetricValue(2, 100, "weight", 75.5, "kg")
        
        assertEquals(value1, value2)
        assertNotEquals(value1, value3)
    }
    
    @Test
    fun `test MetricValue copy`() {
        val value = MetricValue(1, 100, "weight", 75.5, "kg", "Original note")
        val copiedValue = value.copy(value = 76.0, notes = "Updated note")
        
        assertEquals(value.id, copiedValue.id)
        assertEquals(value.entryId, copiedValue.entryId)
        assertEquals(value.metricType, copiedValue.metricType)
        assertEquals(76.0, copiedValue.value, 0.001)
        assertEquals(value.unit, copiedValue.unit)
        assertEquals("Updated note", copiedValue.notes)
    }
}
