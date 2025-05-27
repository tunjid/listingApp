package com.tunjid.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "first_name")
    val firstName: String,
    @ColumnInfo(name = "about")
    var about: String,
    @ColumnInfo(name = "picture_url")
    val pictureUrl: String,
    @ColumnInfo(name = "is_super_host")
    val isSuperHost: Boolean,
    @ColumnInfo(name = "member_since")
    val memberSince: String,
)
