package com.example

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository

object AppContainer {
    private var database: AppDatabase? = null
    var repository: AppRepository? = null
        private set

    fun init(context: Context) {
        if (database == null) {
            val db = AppDatabase.getDatabase(context)
            database = db
            repository = AppRepository(
                chatMessageDao = db.chatMessageDao(),
                loraConfigDao = db.loraConfigDao(),
                automationRuleDao = db.automationRuleDao()
            )
        }
    }
}
