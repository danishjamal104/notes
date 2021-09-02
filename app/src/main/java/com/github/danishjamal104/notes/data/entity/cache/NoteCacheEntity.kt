package com.github.danishjamal104.notes.data.entity.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
class NoteCacheEntity (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int,

    @ColumnInfo(name = "userId")
    var userId: String,

    @ColumnInfo(name = "value")
    var value: String,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long
)