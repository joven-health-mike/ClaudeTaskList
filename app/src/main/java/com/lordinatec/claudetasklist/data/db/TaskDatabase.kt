package com.lordinatec.claudetasklist.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lordinatec.claudetasklist.data.dao.TagDao
import com.lordinatec.claudetasklist.data.dao.TaskDao
import com.lordinatec.claudetasklist.data.model.TagEntity
import com.lordinatec.claudetasklist.data.model.TaskEntity
import com.lordinatec.claudetasklist.data.model.TaskTagCrossRef

@Database(
    entities = [TaskEntity::class, TagEntity::class, TaskTagCrossRef::class],
    version = 1,
    exportSchema = true
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao

    companion object {
        private val PREDEFINED_TAGS = listOf("Finances", "Health", "Other", "Personal", "Shopping", "Work")

        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                PREDEFINED_TAGS.forEach { name ->
                    db.execSQL("INSERT INTO tags (name) VALUES (?)", arrayOf(name))
                }
            }
        }
    }
}
