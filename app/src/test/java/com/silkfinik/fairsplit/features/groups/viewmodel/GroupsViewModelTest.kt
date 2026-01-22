package com.silkfinik.fairsplit.features.groups.viewmodel

import com.silkfinik.fairsplit.core.domain.usecase.group.GetGroupsUseCase
import com.silkfinik.fairsplit.core.model.Currency
import com.silkfinik.fairsplit.core.model.Group
import com.silkfinik.fairsplit.core.testing.MainDispatcherRule
import com.silkfinik.fairsplit.features.groups.ui.GroupsUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getGroupsUseCase: GetGroupsUseCase = mockk()
    private lateinit var viewModel: GroupsViewModel

    @Test
    fun `uiState emits loaded groups`() = runTest {
        val groups = listOf(
            Group("1", "Group 1", Currency.USD),
            Group("2", "Group 2", Currency.EUR)
        )
        every { getGroupsUseCase() } returns flowOf(groups)

        viewModel = GroupsViewModel(getGroupsUseCase)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(groups, state.groups)
        
        collectJob.cancel()
    }
}
