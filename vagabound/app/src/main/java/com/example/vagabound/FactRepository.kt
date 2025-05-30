package com.example.vagabound

object FactRepository {

    fun getFactsNearLocation(latitude: Double, longitude: Double, radiusMiles: Double): List<InterestingFact> {
        // For now, ignore parameters and return mock data.
        // In a real app, this would query a database or API.
        // Coordinates are generally around Sydney for consistency with map default.
        return listOf(
            InterestingFact(
                id = "fact_sydney_opera",
                title = "Sydney Opera House",
                details = "A multi-venue performing arts centre at Sydney Harbour located in Sydney, New South Wales, Australia.",
                latitude = -33.8568, 
                longitude = 151.2153
            ),
            InterestingFact(
                id = "fact_harbour_bridge",
                title = "Sydney Harbour Bridge",
                details = "A steel through arch bridge across Sydney Harbour that carries rail, vehicular, bicycle, and pedestrian traffic.",
                latitude = -33.8523, 
                longitude = 151.2108
            ),
            InterestingFact(
                id = "fact_botanic_garden",
                title = "Royal Botanic Garden",
                details = "A major botanical garden located in the heart of Sydney, New South Wales, Australia.",
                latitude = -33.8642,
                longitude = 151.2165
            )
        )
    }
}
