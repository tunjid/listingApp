package com.tunjid.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite",
    foreignKeys = [
        ForeignKey(
            entity = ListingEntity::class,
            parentColumns = ["id"],
            childColumns = ["listing_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "listing_id")
    val listingId: String,
    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean,
)
