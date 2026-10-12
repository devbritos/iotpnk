package com.example.interfaz

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ConexionDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val sqlCreate = "CREATE TABLE IF NOT EXISTS users (" +
            "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT, " +
            "lastname TEXT, " +
            "email TEXT UNIQUE, " +
            "password TEXT)"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(sqlCreate)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "iotpnk"
        private const val DATABASE_VERSION = 1
    }
}