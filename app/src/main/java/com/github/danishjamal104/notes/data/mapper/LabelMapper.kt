package com.github.danishjamal104.notes.data.mapper

import com.github.danishjamal104.notes.data.entity.cache.LabelCacheEntity
import com.github.danishjamal104.notes.data.mapper.generic.AbstractMapper
import com.github.danishjamal104.notes.data.model.Label
import javax.inject.Inject

class LabelMapper
@Inject
constructor(): AbstractMapper<LabelCacheEntity, Label>() {

    override fun mapFromEntity(entity: LabelCacheEntity): Label {
        return Label(entity.id, entity.userId, entity.value)
    }

    override fun mapToEntity(domainModel: Label): LabelCacheEntity {
        return LabelCacheEntity(domainModel.userId, domainModel.value).apply {
            id = domainModel.id
        }
    }
}