package com.lucrasports.sdk.app.fake_resources

import com.lucrasports.sdk.core.reward.LucraReward

internal var fakeLucraRewards = listOf(
    LucraReward(
        rewardId = "reward_001",
        title = "Client Appetizer",
        descriptor = "10% off",
        iconUrl = "https://picsum.photos/200",
        bannerIconUrl = "https://picsum.photos/200",
        disclaimer = "*Can only be redeemed once per week",
        metadata = mapOf(
            "custom_data" to "{\"type\":\"food\",\"expiry\":\"2024-12-31\"}",
            "simple_data" to "primitive_type_to_string"
        )
    ),
    LucraReward(
        rewardId = "reward_008",
        title = "Client Appetizer",
        descriptor = "10% off",
        iconUrl = "https://picsum.photos/200",
        bannerIconUrl = "https://picsum.photos/200",
        disclaimer = "*Offer valid during happy hours only",
        metadata = mapOf(
            "custom_data" to "{\"type\":\"food\",\"expiry\":\"2024-12-31\"}",
            "simple_data" to "primitive_type_to_string"
        )
    ),
    LucraReward(
        rewardId = "reward_002",
        title = "Free Dessert",
        descriptor = "Get a free dessert with any meal. Get a free dessert with any meal. Get a free dessert with any meal.",
        iconUrl = "https://picsum.photos/200",
        bannerIconUrl = "https://picsum.photos/200",
        disclaimer = "Valid for dine-in only.",
        metadata = mapOf("expiry" to "2024-12-31", "terms" to "One per customer")
    ),
    LucraReward(
        rewardId = "reward_003",
        title = "Free Parking",
        descriptor = "Free parking for one day.",
        iconUrl = "https://example.com/icons/parking.png",
        bannerIconUrl = "https://example.com/banners/parking.png",
        disclaimer = "Not applicable on weekends.",
        metadata = mapOf("expiry" to "2024-12-31", "terms" to "One per customer")
    ),
    LucraReward(
        rewardId = "reward_004",
        title = "Free Gym Class",
        descriptor = "Attend one free gym class.",
        iconUrl = "https://example.com/icons/gym_class.png",
        bannerIconUrl = "https://example.com/banners/gym_class.png",
        disclaimer = "Must book in advance.",
        metadata = mapOf("expiry" to "2024-12-31", "terms" to "One per customer")
    ),
    LucraReward(
        rewardId = "reward_005",
        title = "Free T-Shirt",
        descriptor = "Get a free branded T-shirt.",
        iconUrl = "https://example.com/icons/tshirt.png",
        bannerIconUrl = "https://example.com/banners/tshirt.png",
        disclaimer = "Sizes subject to availability.",
        metadata = mapOf("expiry" to "2024-12-31", "terms" to "One per customer")
    ),
    LucraReward(
        rewardId = "reward_006",
        title = "Free Coffee Mug",
        descriptor = "Get a free coffee mug.",
        iconUrl = "https://example.com/icons/mug.png",
        bannerIconUrl = "https://example.com/banners/mug.png",
        disclaimer = "Limited to the first 50 customers.",
        metadata = mapOf("expiry" to "2024-12-31", "terms" to "One per customer")
    ),
    LucraReward(
        rewardId = "reward_007",
        title = "Free Headphones",
        descriptor = "Get a free pair of headphones.",
        iconUrl = "https://example.com/icons/headphones.png",
        bannerIconUrl = "https://example.com/banners/headphones.png",
        disclaimer = "Available while supplies last.",
        metadata = mapOf("expiry" to "2024-12-31", "terms" to "One per customer")
    )
)