package com.lucrasports.sdk.app.utils

/**
 * Data class representing a location with its ID and display name.
 */
internal data class Location(
    val id: String,
    val displayName: String
) {
    /**
     * Returns a formatted version of the display name for UI presentation.
     */
    fun getFormattedName(): String {
        return displayName.replace("_", " ").split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }
}

/**
 * Centralized location data for the sample app.
 */
internal object LocationData {
    
    /**
     * List of all available locations.
     */
    val ALL_LOCATIONS = listOf(
        Location("020bac16-2e4e-40f8-8a89-07d2b318b25e", "addison_circle_park"),
        Location("22c67186-e7da-4e81-a57b-203e1f5536dc", "village_on_the_parkway"),
        Location("7a1fde39-3d96-4251-a31d-e6bba28b755a", "centennial_olympic_park"),
        Location("38801f23-d047-47c9-a441-645fbc22bc4d", "piedmont_park"),
        Location("a86df7df-7b67-406b-977a-298321130e06", "georgia_aquarium"),
        Location("ce602b78-a5d4-4384-8f53-5f274ee8dc80", "fenway_park"),
        Location("1b291a79-9529-4fc7-ae2a-5ba0ccf3cf20", "boston_common"),
        Location("e5075473-bf43-4533-8b47-c5a1da920b56", "faneuil_hall_marketplace"),
        Location("8bf404ec-8731-4398-970a-f2774244406", "millennium_park"),
        Location("f2708938-2517-46fb-a639-229f4d2ca6c7", "navy_pier"),
        Location("30dae061-6949-4829-90a4-556bff9947f5", "art_institute_of_chicago"),
        Location("21d58655-88dd-4268-a2fc-ec5884c2bc08", "red_rocks_amphitheatre"),
        Location("2d22ff7b-e2f8-4ffc-9141-d09b34246ed9", "denver_botanic_gardens"),
        Location("2bfe0c37-87a9-42d6-bd0c-aadcaf8f6d2c", "larimer_square"),
        Location("8d7f3b2c-d093-4e4a-8e40-281a6af97d30", "centennial_lakes_park"),
        Location("9aec66d0-6dba-45e7-87a4-687cef2e4091", "galleria_edina"),
        Location("94f69060-46f3-4b9e-a281-87a0ebe5e462", "hermann_park"),
        Location("144d307b-b495-4808-9c0d-36b6ba5a53f6", "space_center_houston"),
        Location("169f29bf-3904-42b2-b661-e030ff607439", "museum_district"),
        Location("7cfad8c5-9f54-4dfc-8ed3-c655771125ac", "Downtown"),
        Location("7cfad7c5-9f54-4dfc-8ed3-c655771125ac", "Midtown"),
        Location("7cfad8c5-9f54-4dfc-8ed3-c665771125ac", "Uptown"),
        Location("544a2c00-de38-4efe-9673-ceef92baf045", "backyard_baseball")
    )
}

