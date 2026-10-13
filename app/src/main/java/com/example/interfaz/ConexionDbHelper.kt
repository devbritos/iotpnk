package com.example.interfaz

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ConexionDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val sqlCreate = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "lastname TEXT, " +
                "email TEXT COLLATE NOCASE UNIQUE, " +
                "password TEXT, " +
                "status TEXT DEFAULT 'active', " +
                "sync_state INTEGER DEFAULT 0)"
        db.execSQL(sqlCreate)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "iotpnk.db"
        private const val DATABASE_VERSION = 5

        // Sync states:
        const val STATE_SYNCED = 0
        const val STATE_PENDING_INSERT = 1
        const val STATE_PENDING_UPDATE = 2
        const val STATE_PENDING_DELETE = 3
        const val STATE_SYNCING = 4
    }
}
