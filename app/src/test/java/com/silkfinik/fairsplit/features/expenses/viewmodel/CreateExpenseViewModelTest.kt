package com.silkfinik.fairsplit.features.expenses.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.common.util.UiEvent
import com.silkfinik.fairsplit.core.common.util.UiText
import com.silkfinik.fairsplit.core.domain.usecase.expense.PrepareCreateExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SaveExpenseUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.enums.ExpenseCategory
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

import com.silkfinik.fairsplit.core.model.enums.SplitType

@OptIn(ExperimentalCoroutinesApi::class)
class CreateExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CreateExpenseViewModel
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
    private val prepareCreateExpenseUseCase: PrepareCreateExpenseUseCase = mockk()
    private val saveExpenseUseCase: SaveExpenseUseCase = mockk()

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
        coEvery { prepareCreateExpenseUseCase(testGroupId, null) } returns Result.Success(
            PrepareCreateExpenseUseCase.EditorData(
                currency = testGroup.currency,
                members = testMembers,
                currentUserId = "user1",
                isEditing = false,
                isReadOnly = false,
                description = "",
                amount = "",
                category = ExpenseCategory.OTHER,
                payerId = testMember1.id,
                splitType = SplitType.EQUAL,
                splits = emptyMap(),
                splitData = emptyMap(),
                selectedMemberIds = setOf(testMember1.id, testMember2.id)
            )
        )
    }

    private fun createViewModel() {
        viewModel = CreateExpenseViewModel(
            savedStateHandle = savedStateHandle,
            prepareCreateExpenseUseCase = prepareCreateExpenseUseCase,
            saveExpenseUseCase = saveExpenseUseCase
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
        assertEquals(testMember1.id, state.payerId)
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
        assertTrue(state.amountError is UiText.StringResource)
        assertEquals(com.silkfinik.fairsplit.R.string.error_invalid_amount, (state.amountError as UiText.StringResource).resId)
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
            saveExpenseUseCase(match { params: SaveExpenseUseCase.Params ->
                params.amount == 100.0 && 
                params.description == "Dinner" &&
                params.splits.size == 2
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
        
        assertTrue(viewModel.uiState.value.descriptionError is UiText.StringResource)
        assertEquals(R.string.error_enter_description, (viewModel.uiState.value.descriptionError as UiText.StringResource).resId)
        coVerify(exactly = 0) { saveExpenseUseCase(any()) }
    }

    @Test
    fun `recalculateSplits EXACT works correctly`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("100")
        viewModel.onSplitTypeChange(SplitType.EXACT)
        viewModel.onSplitDataChange(testMember1.id, "40")
        viewModel.onSplitDataChange(testMember2.id, "60")

        val state = viewModel.uiState.value
        assertEquals(40.0, state.splits[testMember1.id])
        assertEquals(60.0, state.splits[testMember2.id])
        assertNull(state.splitError)
    }

    @Test
    fun `recalculateSplits PERCENT works correctly`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("200")
        viewModel.onSplitTypeChange(SplitType.PERCENT)
        viewModel.onSplitDataChange(testMember1.id, "50")
        viewModel.onSplitDataChange(testMember2.id, "50")

        val state = viewModel.uiState.value
        assertEquals(100.0, state.splits[testMember1.id])
        assertEquals(100.0, state.splits[testMember2.id])
        assertNull(state.splitError)
    }

    @Test
    fun `recalculateSplits SHARES works correctly`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onAmountChange("300")
        viewModel.onSplitTypeChange(SplitType.SHARES)
        viewModel.onSplitDataChange(testMember1.id, "1")
        viewModel.onSplitDataChange(testMember2.id, "2")

        val state = viewModel.uiState.value
        assertEquals(100.0, state.splits[testMember1.id])
        assertEquals(200.0, state.splits[testMember2.id])
        assertNull(state.splitError)
    }

    @Test
    fun `onSplitDataChange updates splitData`() = runTest {
        createViewModel()
        advanceUntilIdle()

        viewModel.onSplitDataChange(testMember1.id, "10.5")
        
        val state = viewModel.uiState.value
        assertEquals(10.5, state.splitData[testMember1.id])
    }
}
