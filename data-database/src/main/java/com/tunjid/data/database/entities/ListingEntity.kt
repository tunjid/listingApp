package com.tunjid.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "listings",
)
data class ListingEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "hostId")
    val hostId: String,
    @ColumnInfo(name = "price")
    val price: String,
    @ColumnInfo(name = "description")
    val address: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "property_type")
    val propertyType: String,
)
