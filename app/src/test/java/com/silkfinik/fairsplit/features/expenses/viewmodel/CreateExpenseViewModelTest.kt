package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.domain.repository.AuthRepository
import com.silkfinik.fairsplit.core.domain.usecase.expense.GetExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.GetMembersUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CreateExpenseViewModel
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
    private val getGroupUseCase: GetGroupUseCase = mockk()
    private val getMembersUseCase: GetMembersUseCase = mockk()
    private val getExpenseUseCase: GetExpenseUseCase = mockk()
    private val saveExpenseUseCase: SaveExpenseUseCase = mockk()
    private val authRepository: AuthRepository = mockk()

    private val testGroupId = "group1"
    private val testGroup = Group(id = testGroupId, name = "Test Group", currency = Currency.USD)
    private val testMember1 = Member(
        id = "user1", 
        name = "User 1", 
        groupId = testGroupId,
        isGhost = false,
        createdAt = 1000L,
        updatedAt = 1000L
    )
    private val testMember2 = Member(
        id = "user2", 
        name = "User 2", 
        groupId = testGroupId,
        isGhost = false,
        createdAt = 1000L,
        updatedAt = 1000L
    )
    private val testMembers = listOf(testMember1, testMember2)

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>("groupId") } returns testGroupId
        every { savedStateHandle.get<String>("expenseId") } returns null
        every { authRepository.getUserId() } returns "user1"

        coEvery { getGroupUseCase(testGroupId) } returns flowOf(testGroup)
        coEvery { getMembersUseCase(testGroupId) } returns flowOf(testMembers)
    }

    private fun createViewModel() {
        viewModel = CreateExpenseViewModel(
            savedStateHandle,
            getGroupUseCase,
            getMembersUseCase,
            getExpenseUseCase,
            saveExpenseUseCase,
            authRepository
        )
    }

    @Test
    fun `init loads group and members successfully`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(testGroup.currency, state.currency)
        assertEquals(testMembers, state.members)
        assertEquals(testMember1.id, state.payerId) // Default payer is first member
    }

    @Test
    fun `onAmountChange updates amount and recalculates splits`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("100")
        
        val state = viewModel.uiState.value
        assertEquals("100", state.amount)
        assertNull(state.amountError)

        assertEquals(2, state.splits.size)
        assertEquals(50.0, state.splits[testMember1.id])
        assertEquals(50.0, state.splits[testMember2.id])
    }

    @Test
    fun `onAmountChange with invalid amount shows error`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("-10")
        
        val state = viewModel.uiState.value
        assertEquals("-10", state.amount)
        assertEquals("Некорректная сумма", state.amountError)
    }

    @Test
    fun `onSplitMemberToggle updates selection and recalculates splits`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("100")

        viewModel.onSplitMemberToggle(testMember2.id)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.splits.size)
        assertEquals(100.0, state.splits[testMember1.id])

        viewModel.onSplitMemberToggle(testMember2.id)
        val state2 = viewModel.uiState.value
        assertEquals(50.0, state2.splits[testMember1.id])
    }

    @Test
    fun `onSaveClick with valid data calls use case`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onDescriptionChange("Dinner")
        viewModel.onAmountChange("100")
        
        coEvery { saveExpenseUseCase(any()) } returns Result.Success(Unit)

        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify { 
            saveExpenseUseCase(match { 
                it.amount == 100.0 && 
                it.description == "Dinner" &&
                it.splits.size == 2
            }) 
        }
        
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `onSaveClick with invalid description shows error`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("100")

        viewModel.onSaveClick()
        
        assertEquals("Введите описание", viewModel.uiState.value.descriptionError)
        coVerify(exactly = 0) { saveExpenseUseCase(any()) }
    }
}
