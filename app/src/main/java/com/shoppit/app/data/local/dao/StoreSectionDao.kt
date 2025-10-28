package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppit.app.data.local.entity.StoreSectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSectionDao {
    
    @Query("SELECT * FROM store_sections ORDER BY display_order ASC")
    fun getAllSections(): Flow<List<StoreSectionEntity>>
    
    @Query("SELECT * FROM store_sections WHERE id = :sectionId")
    suspend fun getSectionById(sectionId: Long): StoreSectionEntity?
    
    @Query("SELECT * FROM store_sections WHERE name = :name")
    suspend fun getSectionByName(name: String): StoreSectionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: StoreSectionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<StoreSectionEntity>)
    
    @Update
    suspend fun updateSection(section: StoreSectionEntity)
    
    @Query("UPDATE store_sections SET is_collapsed = :isCollapsed WHERE id = :sectionId")
    suspend fun updateCollapsedState(sectionId: Long, isCollapsed: Boolean)
    
    @Query("UPDATE store_sections SET display_order = :order WHERE id = :sectionId")
    suspend fun updateDisplayOrder(sectionId: Long, order: Int)
    
    @Query("DELETE FROM store_sections WHERE id = :sectionId")
    suspend fun deleteSection(sectionId: Long)
    
    @Query("SELECT COUNT(*) FROM store_sections")
    suspend fun getSectionCount(): Int
}
