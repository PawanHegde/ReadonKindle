package com.pawanhegde.readonkindle.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_table")
class Content(@PrimaryKey val uuid: String,
              val resolvedUrl: String,
              val title: String,
              val originalContent: String,
              val simplifiedContent: String)