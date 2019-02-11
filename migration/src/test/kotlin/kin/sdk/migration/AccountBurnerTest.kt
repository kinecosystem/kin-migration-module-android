package kin.sdk.migration

import kin.sdk.migration.bi.IMigrationEventsListener
import kin.sdk.migration.common.interfaces.ITransactionId
import kin.sdk.migration.internal.core_related.KinAccountCoreImpl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations


class AccountBurnerTest {

    @Mock
    private lateinit var eventsNotifier: MigrationEventsNotifier

    @Mock
    private lateinit var kinAccount: KinAccountCoreImpl


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @After
    fun tearDown() {

    }

    @Test
    fun startBurnAccountProcess_accountNotYetBurned_success() {
        // Given
        `when`(kinAccount.isAccountBurned).thenReturn(false)
        `when`(kinAccount.sendBurnTransactionSync(ArgumentMatchers.anyString())).thenReturn(ITransactionId { "id" })
        `when`(kinAccount.publicAddress).thenReturn("public_address")

        // When
        val accountBurner = AccountBurner(eventsNotifier)
        val burnReason = accountBurner.start(kinAccount)

        // Then
        verify(kinAccount, times(1)).sendBurnTransactionSync(ArgumentMatchers.anyString())
        assertThat(burnReason, equalTo(IMigrationEventsListener.BurnReason.BURNED))
    }

    @Test
    fun startBurnAccountProcess_accountAlreadyBurned_success() {
        // Given
        `when`(kinAccount.isAccountBurned).thenReturn(true)
        `when`(kinAccount.publicAddress).thenReturn("public_address")

        // When
        val accountBurner = AccountBurner(eventsNotifier)
        val burnReason = accountBurner.start(kinAccount)

        // Then
        verify(kinAccount, times(0)).sendBurnTransactionSync(ArgumentMatchers.anyString())
        assertThat(burnReason, equalTo(IMigrationEventsListener.BurnReason.ALREADY_BURNED))
    }


}