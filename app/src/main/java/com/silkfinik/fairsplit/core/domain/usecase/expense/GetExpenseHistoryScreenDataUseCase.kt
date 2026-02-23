package com.silkfinik.fairsplit.core.domain.usecase.expense

import com.silkfinik.fairsplit.core.domain.repository.ExpenseRepository
import com.silkfinik.fairsplit.core.domain.repository.GroupRepository
import com.silkfinik.fairsplit.core.domain.repository.MemberRepository
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.HistoryItem
import com.silkfinik.fairsplit.core.model.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetExpenseHistoryScreenDataUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) {
    sealed interface HistoryDateHeader {
        data object Today : HistoryDateHeader
        data object Yesterday : HistoryDateHeader
        data class SpecificDate(val formattedDate: String) : HistoryDateHeader
    }

    data class HistoryCreateUI(
        val description: String?,
        val amount: Double?,
        val payers: Map<String, Double>,
        val splits: Map<String, Double>
    )

    sealed interface HistoryChangeUI {
        data class Amount(val from: Double?, val to: Double?) : HistoryChangeUI
        data class Description(val from: String?, val to: String?) : HistoryChangeUI
        data class Category(val fromId: String?, val toId: String?) : HistoryChangeUI
        data class Payers(val from: Map<String, Double>, val to: Map<String, Double>) : HistoryChangeUI
        data class Splits(val from: Map<String, Double>, val to: Map<String, Double>) : HistoryChangeUI
    }

    data class HistoryItemUI(
        val id: String,
        val timestamp: Long,
        val isMathValid: Boolean,
        val isCreate: Boolean,
        val createData: HistoryCreateUI?,
        val updateChanges: List<HistoryChangeUI>
    )

    data class HistoryDateGroup(
        val dateHeader: HistoryDateHeader,
        val items: List<HistoryItemUI>
    )

    data class ScreenData(
        val historyGroups: List<HistoryDateGroup>,
        val members: Map<String, Member>,
        val currency: Currency?
    )

    operator fun invoke(groupId: String, expenseId: String): Flow<ScreenData> {
        return combine(
            expenseRepository.getExpenseHistory(groupId, expenseId),
            memberRepository.getMembers(groupId),
            groupRepository.getGroup(groupId)
        ) { history, members, group ->
            val now = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val itemCalendar = Calendar.getInstance()
            
            val dateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

            val grouped = history.map { item -> 
                mapToUI(item)
            }.groupBy { itemUI ->
                itemCalendar.timeInMillis = itemUI.timestamp
                when {
                    isSameDay(now, itemCalendar) -> HistoryDateHeader.Today
                    isSameDay(yesterday, itemCalendar) -> HistoryDateHeader.Yesterday
                    else -> HistoryDateHeader.SpecificDate(dateFormatter.format(Date(itemUI.timestamp)))
                }
            }.map { (header, items) ->
                HistoryDateGroup(dateHeader = header, items = items)
            }

            ScreenData(
                historyGroups = grouped,
                members = members.associateBy { it.id },
                currency = group?.currency
            )
        }
    }

    private fun mapToUI(item: HistoryItem): HistoryItemUI {
        val isCreate = item.action == "CREATE" || item.changes.containsKey("_event")
        
        var createData: HistoryCreateUI? = null
        val updateChanges = mutableListOf<HistoryChangeUI>()

        try {
            if (isCreate) {
                val description = item.changes["description"] as? String
                val amount = (item.changes["amount"] as? Number)?.toDouble()
                val payers = parseMap(item.changes["payers"])
                val splits = parseMap(item.changes["splits"])
                createData = HistoryCreateUI(description, amount, payers, splits)
            } else {
                item.changes.forEach { (field, changeValue) ->
                    if (field == "_event" || field == "is_math_valid" || field == "server_validated_at") return@forEach

                    val changeMap = changeValue as? Map<*, *> ?: return@forEach
                    val from = changeMap["from"]
                    val to = changeMap["to"]

                    when (field) {
                        "amount" -> updateChanges.add(HistoryChangeUI.Amount((from as? Number)?.toDouble(), (to as? Number)?.toDouble()))
                        "description" -> updateChanges.add(HistoryChangeUI.Description(from?.toString(), to?.toString()))
                        "category" -> updateChanges.add(HistoryChangeUI.Category(from?.toString(), to?.toString()))
                        "payers" -> updateChanges.add(HistoryChangeUI.Payers(parseMap(from), parseMap(to)))
                        "splits" -> updateChanges.add(HistoryChangeUI.Splits(parseMap(from), parseMap(to)))
                    }
                }
            }
        } catch (e: Exception) {
            // Log or ignore mapping errors securely
        }

        return HistoryItemUI(
            id = item.id,
            timestamp = item.timestamp,
            isMathValid = item.isMathValid,
            isCreate = isCreate,
            createData = createData,
            updateChanges = updateChanges
        )
    }

    private fun parseMap(data: Any?): Map<String, Double> {
        val map = data as? Map<*, *> ?: return emptyMap()
        val result = mutableMapOf<String, Double>()
        map.forEach { (k, v) ->
            val keyStr = k?.toString()
            val valDouble = (v as? Number)?.toDouble()
            if (keyStr != null && valDouble != null) {
                result[keyStr] = valDouble
            }
        }
        return result
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}