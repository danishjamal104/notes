package com.github.danishjamal104.notes.data.entity.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labels")
class LabelCacheEntity (

    @ColumnInfo(name = "userId")
    var userId: String,

    @ColumnInfo(name = "value")
    var value: String

) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

}