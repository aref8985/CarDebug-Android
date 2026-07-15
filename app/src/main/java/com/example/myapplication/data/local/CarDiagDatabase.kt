package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Vehicle::class, Dtc::class], version = 7, exportSchema = false)
abstract class CarDiagDatabase : RoomDatabase() {
    abstract fun carDiagDao(): CarDiagDao

    companion object {
        @Volatile
        private var INSTANCE: CarDiagDatabase? = null

        fun getDatabase(context: Context): CarDiagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarDiagDatabase::class.java,
                    "car_diag_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
