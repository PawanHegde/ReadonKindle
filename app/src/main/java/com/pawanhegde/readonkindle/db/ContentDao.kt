package com.pawanhegde.readonkindle.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import com.pawanhegde.readonkindle.entities.Content

@Dao
interface ContentDao {
    @Query("select * from content_table where uuid = :uuid")
    fun getByUuid(uuid: String): Content?

    @Query("select * from content_table")
    fun getAll(): List<Content>

    @Insert
    fun insert(content: Content)
}
