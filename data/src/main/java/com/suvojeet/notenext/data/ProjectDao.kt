package com.suvojeet.notenext.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("SELECT * FROM projects WHERE parentId IS NULL ORDER BY orderIndex ASC, createdAt DESC")
    fun getRootProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE parentId = :parentId ORDER BY orderIndex ASC, createdAt DESC")
    fun getSubProjects(parentId: Int): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY orderIndex ASC, createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: Int)

    @Query("UPDATE notes SET projectId = NULL WHERE projectId IN (:projectIds)")
    suspend fun clearProjectFromNotes(projectIds: List<Int>)

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Int): Project?

    @Query("""
        SELECT projectId, COUNT(*) AS noteCount FROM notes
        WHERE projectId IS NOT NULL AND isArchived = 0 AND isBinned = 0 AND isDecoy = 0
        GROUP BY projectId
    """)
    fun getProjectNoteCounts(): Flow<List<ProjectNoteCount>>

    @Query("SELECT * FROM projects WHERE parentId IS NULL ORDER BY orderIndex ASC, createdAt DESC")
    suspend fun getRootProjectsSync(): List<Project>

    @Query("SELECT * FROM projects WHERE parentId = :parentId ORDER BY orderIndex ASC, createdAt DESC")
    suspend fun getSubProjectsSync(parentId: Int): List<Project>

    @Query("UPDATE projects SET orderIndex = :orderIndex WHERE id = :projectId")
    suspend fun updateProjectOrder(projectId: Int, orderIndex: Int)

    @Query("UPDATE projects SET parentId = :newParentId WHERE id = :projectId")
    suspend fun moveProjectToParent(projectId: Int, newParentId: Int?)

    @Query("""
        WITH RECURSIVE project_tree(id, name, description, createdAt, parentId, orderIndex, color, depth, sortPath) AS (
            SELECT id, name, description, createdAt, parentId, orderIndex, color, 0 as depth, 
                   printf('%010d', orderIndex) as sortPath
            FROM projects 
            WHERE parentId IS NULL
            UNION ALL
            SELECT p.id, p.name, p.description, p.createdAt, p.parentId, p.orderIndex, p.color, 
                   pt.depth + 1,
                   pt.sortPath || '/' || printf('%010d', p.orderIndex)
            FROM projects p
            INNER JOIN project_tree pt ON p.parentId = pt.id
        )
        SELECT id, name, description, createdAt, parentId, orderIndex, color FROM project_tree ORDER BY sortPath ASC
    """)
    @Suppress("RoomWarnings.QUERY_MISMATCH")
    fun getProjectHierarchy(): Flow<List<Project>>
}
