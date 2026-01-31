package com.silkfinik.fairsplit.core.data.datasource

import com.google.firebase.functions.FirebaseFunctions
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.asSafeMap
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CloudFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {

    suspend fun claimGhost(groupId: String, ghostId: String): Result<Unit> {
        return try {
            val data = hashMapOf(
                "groupId" to groupId,
                "ghostId" to ghostId
            )
            functions
                .getHttpsCallable("claimGhost")
                .call(data)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun joinByInviteCode(code: String): Result<String> {
        return try {
            val result = functions
                .getHttpsCallable("joinByInviteCode")
                .call(mapOf("code" to code))
                .await()

            val data = result.data.asSafeMap()
            val groupId = data["groupId"] as? String

            if (groupId != null) {
                Result.Success(groupId)
            } else {
                Result.Error("Не удалось получить ID группы")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка при вступлении в группу", e)
        }
    }

    suspend fun createInviteCode(groupId: String): Result<String> {
        return try {
            val result = functions
                .getHttpsCallable("createInviteCode")
                .call(mapOf("groupId" to groupId))
                .await()

            val data = result.data.asSafeMap()
            val code = data["code"] as? String

            if (code != null) {
                Result.Success(code)
            } else {
                Result.Error("Не удалось создать код приглашения")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка генерации кода", e)
        }
    }
}
