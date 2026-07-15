package com.example.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDiagDao {
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM dtcs WHERE code = :code")
    suspend fun getDtc(code: String): Dtc?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDtcs(dtcs: List<Dtc>)

    @Query("UPDATE vehicles SET isActive = 0")
    suspend fun clearActiveStatus()

    @Query("UPDATE vehicles SET isActive = 1 WHERE id = :vehicleId")
    suspend fun setActive(vehicleId: Int)

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    fun getActiveVehicle(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVehicleSync(): Vehicle?

    @Query("SELECT * FROM vehicles WHERE vin = :vin LIMIT 1")
    suspend fun getVehicleByVin(vin: String): Vehicle?

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()
}
