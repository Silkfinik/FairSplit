package com.silkfinik.fairsplit.features.groupdetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.silkfinik.fairsplit.core.common.util.Result
import com.silkfinik.fairsplit.core.domain.usecase.expense.DeleteExpenseUseCase
import com.silkfinik.fairsplit.core.domain.usecase.expense.SyncGroupExpensesUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GenerateInviteCodeUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupDetailsScreenDataUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.UpdateGroupAvatarUseCase
import com.silkfinik.fairsplit.core.domain.usecase.group.UpdateGroupNameUseCase
import com.silkfinik.fairsplit.core.domain.usecase.member.AddGhostMemberUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.SyncGroupPaymentsUseCase
import com.silkfinik.fairsplit.core.domain.usecase.payment.UpdatePaymentStatusUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupDetailsScreenDataUseCase.ScreenData
import com.silkfinik.fairsplit.core.model.Expense
import com.silkfinik.fairsplit.core.model.Member
import com.silkfinik.fairsplit.core.model.Payment
import com.silkfinik.fairsplit.core.testing.MainDispatcherRule
import com.silkfinik.fairsplit.features.groupdetails.ui.GroupDetailsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
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
    private val getGroupDetailsScreenDataUseCase: GetGroupDetailsScreenDataUseCase = mockk()
    private val addGhostMemberUseCase: AddGhostMemberUseCase = mockk()
    private val deleteExpenseUseCase: DeleteExpenseUseCase = mockk()
    private val syncGroupExpensesUseCase: SyncGroupExpensesUseCase = mockk(relaxed = true)
    private val syncGroupPaymentsUseCase: SyncGroupPaymentsUseCase = mockk(relaxed = true)
    private val generateInviteCodeUseCase: GenerateInviteCodeUseCase = mockk()
    private val updatePaymentStatusUseCase: UpdatePaymentStatusUseCase = mockk()
    private val updateGroupNameUseCase: UpdateGroupNameUseCase = mockk()
    private val updateGroupAvatarUseCase: UpdateGroupAvatarUseCase = mockk()

    private lateinit var viewModel: GroupDetailsViewModel

    private val groupId = "group1"
    private val testGroup = Group(groupId, "Test Group", Currency.USD)
    private val currentUserId = "u1"

    private val testScreenData = ScreenData(
        group = testGroup,
        members = emptyList<Member>(),
        currentUserId = currentUserId,
        expenses = emptyList<Expense>(),
        payments = emptyList<Payment>(),
        balances = emptyMap<String, Double>()
    )

    @Before
    fun setUp() {
        every { savedStateHandle.get<String>("groupId") } returns groupId
        every { getGroupDetailsScreenDataUseCase(groupId) } returns flowOf(testScreenData)
    }

    private fun createViewModel() {
        viewModel = GroupDetailsViewModel(
            savedStateHandle = savedStateHandle,
            getGroupDetailsScreenDataUseCase = getGroupDetailsScreenDataUseCase,
            addGhostMemberUseCase = addGhostMemberUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            syncGroupExpensesUseCase = syncGroupExpensesUseCase,
            syncGroupPaymentsUseCase = syncGroupPaymentsUseCase,
            generateInviteCodeUseCase = generateInviteCodeUseCase,
            updatePaymentStatusUseCase = updatePaymentStatusUseCase,
            updateGroupNameUseCase = updateGroupNameUseCase,
            updateGroupAvatarUseCase = updateGroupAvatarUseCase
        )
    }

    @Test
    fun `init starts sync`() = runTest {
        createViewModel()
        verify { syncGroupExpensesUseCase.start(groupId) }
        verify { syncGroupPaymentsUseCase.start(groupId) }
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
        assertEquals(emptyList<Any>(), successState.members)
        assertEquals(currentUserId, successState.currentUserId)
        assertTrue(successState.expenses.isEmpty())
        assertTrue(successState.balances.isEmpty())
        
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