package com.healthtracker.data

import org.junit.Test
import org.junit.Assert.*

class MetricTypeTest {
    
    @Test
    fun `test MetricType creation with valid data`() {
        val metricType = MetricType(
            id = "weight",
            name = "Body Weight",
            unit = "kg",
            description = "Body weight measurement",
            minValue = 0.0,
            maxValue = 500.0,
            stepSize = 0.1
        )
        
        assertEquals("weight", metricType.id)
        assertEquals("Body Weight", metricType.name)
        assertEquals("kg", metricType.unit)
        assertEquals("Body weight measurement", metricType.description)
        assertEquals(0.0, metricType.minValue!!, 0.001)
        assertEquals(500.0, metricType.maxValue!!, 0.001)
        assertEquals(0.1, metricType.stepSize!!, 0.001)
    }
    
    @Test
    fun `test MetricType with null optional fields`() {
        val metricType = MetricType(
            id = "note",
            name = "Health Note"
        )
        
        assertEquals("note", metricType.id)
        assertEquals("Health Note", metricType.name)
        assertNull(metricType.unit)
        assertNull(metricType.description)
        assertNull(metricType.minValue)
        assertNull(metricType.maxValue)
        assertNull(metricType.stepSize)
    }
    
    @Test
    fun `test MetricType equality`() {
        val type1 = MetricType("weight", "Body Weight", "kg")
        val type2 = MetricType("weight", "Body Weight", "kg")
        val type3 = MetricType("height", "Height", "cm")
        
        assertEquals(type1, type2)
        assertNotEquals(type1, type3)
    }
    
    @Test
    fun `test MetricType copy`() {
        val type = MetricType(
            id = "weight",
            name = "Body Weight",
            unit = "kg",
            description = "Original description",
            minValue = 0.0,
            maxValue = 200.0,
            stepSize = 0.1
        )
        
        val copiedType = type.copy(
            name = "Weight",
            description = "Updated description",
            maxValue = 300.0
        )
        
        assertEquals(type.id, copiedType.id)
        assertEquals("Weight", copiedType.name)
        assertEquals(type.unit, copiedType.unit)
        assertEquals("Updated description", copiedType.description)
        assertEquals(type.minValue!!, copiedType.minValue!!, 0.001)
        assertEquals(300.0, copiedType.maxValue!!, 0.001)
        assertEquals(type.stepSize!!, copiedType.stepSize!!, 0.001)
    }
}
