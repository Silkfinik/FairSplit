package com.silkfinik.fairsplit.features.groupdetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.usecase.auth.GetCurrentUserIdUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.DeleteExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SyncGroupExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.testing.MainDispatcherRule
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
    private val getGroupUseCase: GetGroupUseCase = mockk()
    private val getExpensesUseCase: GetExpensesUseCase = mockk()
    private val getMembersUseCase: GetMembersUseCase = mockk()
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase = mockk()
    private val addGhostMemberUseCase: AddGhostMemberUseCase = mockk()
    private val deleteExpenseUseCase: DeleteExpenseUseCase = mockk()
    private val syncGroupExpensesUseCase: SyncGroupExpensesUseCase = mockk(relaxed = true)

    private lateinit var viewModel: GroupDetailsViewModel

    private val groupId = "group1"
    private val testGroup = Group(groupId, "Test Group", Currency.USD)
    private val testMember1 = Member("u1", groupId, "User 1", null, false, 0, 0)
    private val testMember2 = Member("u2", groupId, "User 2", null, false, 0, 0)
    private val testMembers = listOf(testMember1, testMember2)
    private val currentUserId = "u1"

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>("groupId") } returns groupId
        every { getGroupUseCase(groupId) } returns flowOf(testGroup)
        every { getMembersUseCase(groupId) } returns flowOf(testMembers)
        every { getCurrentUserIdUseCase() } returns flowOf(currentUserId)

        every { getExpensesUseCase(groupId) } returns flowOf(emptyList())
    }

    private fun createViewModel() {
        viewModel = GroupDetailsViewModel(
            savedStateHandle,
            getGroupUseCase,
            getExpensesUseCase,
            getMembersUseCase,
            getCurrentUserIdUseCase,
            addGhostMemberUseCase,
            deleteExpenseUseCase,
            syncGroupExpensesUseCase
        )
    }

    @Test
    fun `init starts sync`() = runTest {
        createViewModel()
        verify { syncGroupExpensesUseCase.start(groupId) }
    }

    @Test
    fun `uiState Success contains correct data`() = runTest {
        createViewModel()
        
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }
        
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GroupDetailsUiState.Success)
        val successState = state as GroupDetailsUiState.Success
        
        assertEquals(testGroup, successState.group)
        assertEquals(testMembers, successState.members)
        assertEquals(currentUserId, successState.currentUserId)
        assertTrue(successState.expenses.isEmpty())
        assertTrue(successState.balances.isEmpty())
        
        collectJob.cancel()
    }

    @Test
    fun `calculateBalances works correctly`() = runTest {
        val expense = Expense(
            id = "e1",
            groupId = groupId,
            description = "Lunch",
            amount = 100.0,
            currency = Currency.USD,
            date = 0,
            creatorId = "u1",
            payers = mapOf("u1" to 100.0),
            splits = mapOf("u1" to 50.0, "u2" to 50.0),
            createdAt = 0,
            updatedAt = 0
        )
        every { getExpensesUseCase(groupId) } returns flowOf(listOf(expense))

        createViewModel()
        
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }
        
        advanceUntilIdle()

        val state = viewModel.uiState.value as GroupDetailsUiState.Success

        assertEquals(50.0, state.balances["u1"])

        assertEquals(-50.0, state.balances["u2"])
        
        collectJob.cancel()
    }

    @Test
    fun `addGhostMember calls use case`() = runTest {
        createViewModel()
        coEvery { addGhostMemberUseCase(any(), any()) } returns Result.Success(Unit)

        viewModel.addGhostMember("Ghost")
        advanceUntilIdle()

        coVerify { addGhostMemberUseCase(groupId, "Ghost") }
    }

    @Test
    fun `deleteExpense calls use case`() = runTest {
        createViewModel()
        coEvery { deleteExpenseUseCase(any()) } returns Result.Success(Unit)

        viewModel.deleteExpense("e1")
        advanceUntilIdle()

        coVerify { deleteExpenseUseCase("e1") }
    }
}