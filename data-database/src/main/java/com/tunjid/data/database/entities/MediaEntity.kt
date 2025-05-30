package com.tunjid.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = ListingEntity::class,
            parentColumns = ["id"],
            childColumns = ["listing_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class MediaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "listing_id")
    val listingId: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "ally_content_description")
    val a11yContentDescription: String
)
