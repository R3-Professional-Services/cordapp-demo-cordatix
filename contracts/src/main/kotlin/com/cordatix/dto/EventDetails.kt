package com.cordatix.dto

import java.time.Instant

data class EventDetails(val eventName : String,
                        val eventType : String,
                        val eventVenue: String,
                        val eventVenueCapacity: Int,
                        val eventCity: String,
                        val eventStartTime: Instant,
                        val eventEndTime: Instant,
                        val eventDescription: String)
