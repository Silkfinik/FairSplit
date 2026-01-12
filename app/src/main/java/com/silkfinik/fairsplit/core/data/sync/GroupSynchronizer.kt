package com.silkfinik.fairsplit.core.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.silkfinik.fairsplit.core.common.di.ApplicationScope
import com.silkfinik.fairsplit.core.data.repository.AuthRepository
import com.silkfinik.fairsplit.core.database.dao.GroupDao
import com.silkfinik.fairsplit.core.database.entity.GroupEntity
import com.silkfinik.fairsplit.core.database.util.Converters
import com.silkfinik.fairsplit.core.network.model.GroupDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupSynchronizer @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao,
    private val authRepository: AuthRepository,
    @ApplicationScope private val externalScope: CoroutineScope
) {

    private var groupListener: ListenerRegistration? = null
    private val converters = Converters()

    suspend fun syncLocalChanges() {
        val userId = authRepository.getUserId() ?: return

        // Берем все группы из базы (Flow -> List)
        // В идеале в DAO нужен метод getDirtyGroups(), но пока возьмем все и отфильтруем
        val allGroups = groupDao.getGroups().first()
        val dirtyGroups = allGroups.filter { it.is_dirty }

        if (dirtyGroups.isEmpty()) return

        val batch = firestore.batch()
        val groupsCollection = firestore.collection("groups")

        dirtyGroups.forEach { entity ->
            val docRef = groupsCollection.document(entity.id)
            val dto = mapEntityToDto(entity)

            // Используем merge, чтобы не затереть данные, если они изменились параллельно
            // (хотя LWW требует более сложной логики, для старта хватит SetOptions.merge())
            batch.set(docRef, dto, SetOptions.merge())
        }

        try {
            batch.commit().await()
            Log.d("Sync", "Успешно отправлено ${dirtyGroups.size} групп")

            // После успеха снимаем флаг is_dirty
            dirtyGroups.forEach { group ->
                groupDao.insertGroup(group.copy(is_dirty = false)) // insert = replace/update
            }
        } catch (e: Exception) {
            Log.e("Sync", "Ошибка синхронизации: ${e.message}")
        }
    }

    fun startListening() {
        val userId = authRepository.getUserId() ?: return

        groupListener?.remove()

        // Слушаем только свои группы
        val query = firestore.collection("groups")
            .whereEqualTo("owner_id", userId)

        groupListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Sync", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val dtos = snapshot.toObjects(GroupDto::class.java)
                // Обработку данных выносим в корутину, чтобы не грузить UI поток
                externalScope.launch {
                    saveServerDataToLocal(dtos)
                }
            }
        }
    }

    fun stopListening() {
        groupListener?.remove()
        groupListener = null
    }

    private suspend fun saveServerDataToLocal(dtos: List<GroupDto>) {
        dtos.forEach { dto ->
            val localEntity = groupDao.getGroupById(dto.id)

            // Логика разрешения конфликтов (LWW - Last Write Wins)
            val shouldSave = if (localEntity == null) {
                // 1. У нас нет такой группы -> сохраняем смело
                true
            } else if (!localEntity.is_dirty) {
                // 2. У нас есть группа, и она синхронизирована (чистая) -> обновляем смело
                true
            } else {
                // 3. У нас есть группа, и она "грязная" (мы что-то меняли оффлайн)
                // Сравниваем время: если сервер новее, то затираем наши правки.
                // Если наши правки новее - не трогаем (потом PUSH их отправит).
                dto.updatedAt > localEntity.updated_at
            }

            if (shouldSave) {
                groupDao.insertGroup(mapDtoToEntity(dto))
            }
        }
    }

    private fun mapEntityToDto(entity: GroupEntity): GroupDto {
        return GroupDto(
            id = entity.id,
            name = entity.name,
            currency = entity.currency.name,
            ownerId = entity.owner_id,
            createdAt = entity.created_at,
            updatedAt = entity.updated_at
        )
    }

    private fun mapDtoToEntity(dto: GroupDto): GroupEntity {
        return GroupEntity(
            id = dto.id,
            name = dto.name,
            currency = converters.toCurrency(dto.currency),
            owner_id = dto.ownerId,
            created_at = dto.createdAt,
            updated_at = dto.updatedAt,
            is_dirty = false // Пришло с сервера -> значит чистое
        )
    }
}