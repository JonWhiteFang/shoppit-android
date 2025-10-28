package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shoppit.app.data.local.entity.ShoppingTemplateEntity
import com.shoppit.app.data.local.entity.TemplateItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    
    @Query("SELECT * FROM shopping_templates ORDER BY last_used_at DESC, created_at DESC")
    fun getAllTemplates(): Flow<List<ShoppingTemplateEntity>>
    
    @Query("SELECT * FROM shopping_templates WHERE id = :templateId")
    fun getTemplateById(templateId: Long): Flow<ShoppingTemplateEntity?>
    
    @Query("SELECT * FROM template_items WHERE template_id = :templateId")
    fun getTemplateItems(templateId: Long): Flow<List<TemplateItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ShoppingTemplateEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateItems(items: List<TemplateItemEntity>)
    
    @Update
    suspend fun updateTemplate(template: ShoppingTemplateEntity)
    
    @Query("UPDATE shopping_templates SET last_used_at = :timestamp WHERE id = :templateId")
    suspend fun updateLastUsed(templateId: Long, timestamp: Long)
    
    @Query("DELETE FROM shopping_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: Long)
    
    @Query("DELETE FROM template_items WHERE template_id = :templateId")
    suspend fun deleteTemplateItems(templateId: Long)
    
    @Transaction
    suspend fun deleteTemplateWithItems(templateId: Long) {
        deleteTemplateItems(templateId)
        deleteTemplate(templateId)
    }
    
    @Transaction
    suspend fun insertTemplateWithItems(template: ShoppingTemplateEntity, items: List<TemplateItemEntity>): Long {
        val templateId = insertTemplate(template)
        val itemsWithTemplateId = items.map { it.copy(templateId = templateId) }
        insertTemplateItems(itemsWithTemplateId)
        return templateId
    }
}
