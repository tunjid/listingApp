package com.tunjid.data.favorite.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.tunjid.data.listing.database.model.ListingEntity

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
