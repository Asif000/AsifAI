package com.example.vagabound

import org.junit.Test
import org.junit.Assert.*

class FactRepositoryTest {

    @Test
    fun getFactsNearLocation_returnsMockData() {
        // Parameters are ignored by the current mock implementation
        val facts = FactRepository.getFactsNearLocation(0.0, 0.0, 1.0)
        
        // Check that the list is not empty
        assertTrue("Fact list should not be empty", facts.isNotEmpty()) 
    }

    @Test
    fun getFactsNearLocation_mockDataHasExpectedContent() {
        val facts = FactRepository.getFactsNearLocation(0.0, 0.0, 1.0)
        
        // Assuming there's at least one fact and checking its properties
        if (facts.isNotEmpty()) {
            val firstFact = facts[0]
            assertEquals("fact_sydney_opera", firstFact.id)
            assertEquals("Sydney Opera House", firstFact.title) 
            // Check details for the first fact as an example
            assertEquals("A multi-venue performing arts centre at Sydney Harbour located in Sydney, New South Wales, Australia.", firstFact.details)
            // Check coordinates for the first fact
            assertEquals(-33.8568, firstFact.latitude, 0.0001) // Delta for double comparison
            assertEquals(151.2153, firstFact.longitude, 0.0001) // Delta for double comparison

            // Optionally, check properties of other facts if desired
            if (facts.size >= 2) {
                val secondFact = facts[1]
                assertEquals("fact_harbour_bridge", secondFact.id)
                assertEquals("Sydney Harbour Bridge", secondFact.title)
            }

        } else {
            fail("Fact list was empty, cannot check content.")
        }
    }

    @Test
    fun getFactsNearLocation_returnsExpectedNumberOfFacts() {
        val facts = FactRepository.getFactsNearLocation(0.0, 0.0, 1.0)
        assertEquals("Should return 3 mock facts", 3, facts.size)
    }
}
