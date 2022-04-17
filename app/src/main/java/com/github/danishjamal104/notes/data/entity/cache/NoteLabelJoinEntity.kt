package com.github.danishjamal104.notes.data.entity.cache

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "note_label_join",
    primaryKeys = ["noteId", "labelId"],
    foreignKeys = [ForeignKey(
        entity = NoteCacheEntity::class,
        parentColumns = ["id"],
        childColumns = ["noteId"]
    ),
        ForeignKey(
            entity = LabelCacheEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"]
        )
    ]

)
class NoteLabelJoinEntity{

    var noteId: Int = 0
    var labelId: Int = 0
}