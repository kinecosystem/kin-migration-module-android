package kin.sdk.migration

import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import kin.base.Memo
import kin.base.MemoText
import kin.base.Server
import kin.sdk.Environment
import kin.sdk.migration.IntegConsts.TEST_SDK_NETWORK_URL
import kin.sdk.migration.common.exception.AccountNotFoundException
import kin.sdk.migration.common.exception.InsufficientKinException
import kin.sdk.migration.common.interfaces.*
import kin.sdk.migration.internal.sdk_related.KinClientSdkImpl
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.rules.ExpectedException
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("FunctionName")
class KinSdkAccountIntegrationTest {

    private val appId = "1a2c"
    private val appIdVersionPrefix = "1"
    private val timeoutDurationSeconds: Long = 15
    private val timeoutDurationSecondsLong: Long = 20

    private lateinit var kinClient: IKinClient

    @Rule
    @JvmField
    val expectedEx: ExpectedException = ExpectedException.none()

    private val environment: Environment = Environment(TEST_SDK_NETWORK_URL, IntegConsts.TEST_SDK_NETWORK_ID)

    @Before
    @Throws(IOException::class)
    fun setup() {
        kinClient = KinClientSdkImpl(InstrumentationRegistry.getTargetContext(), environment, appId)
        kinClient.clearAllAccounts()
    }

    @After
    fun teardown() {
        if (::kinClient.isInitialized) {
            kinClient.clearAllAccounts()
        }
    }

    @Test
    @LargeTest
    fun getBalanceSync_AccountNotCreated_AccountNotFoundException() {
        val kinAccount = kinClient.addAccount()

        expectedEx.expect(AccountNotFoundException::class.java)
        expectedEx.expectMessage(kinAccount.publicAddress.orEmpty())
        kinAccount.balanceSync
    }

    @Test
    @LargeTest
    fun getStatusSync_AccountNotCreated_StatusNotCreated() {
        val kinAccount = kinClient.addAccount()

        val status = kinAccount.statusSync
        assertThat(status, equalTo(IAccountStatus.NOT_CREATED))
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun getBalanceSync_FundedAccount_GotBalance() {
        val kinAccount = kinClient.addAccount()

        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = kinAccount.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }
        fakeKinSdkOnBoard.createAccount(kinAccount.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))

        assertThat(kinAccount.balanceSync.value(), equalTo(BigDecimal("0.00000")))
        fakeKinSdkOnBoard.fundWithKin(kinAccount.publicAddress.orEmpty(), "3.14159")
        assertThat(kinAccount.balanceSync.value(), equalTo(BigDecimal("3.14159")))

    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun getStatusSync_CreateAccount_StatusCreated() {
        val kinAccount = kinClient.addAccount()

        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = kinAccount.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }
        fakeKinSdkOnBoard.createAccount(kinAccount.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))

        assertThat(kinAccount.balanceSync.value(), equalTo(BigDecimal("0.00000")))
        val status = kinAccount.statusSync
        assertThat(status, equalTo(IAccountStatus.CREATED))
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendTransaction_WithMemo() {
        val (kinAccountSender, kinAccountReceiver) = onboardAccounts(senderFundAmount = 100)

        val memo = "fake memo"
        val expectedMemo = addAppIdToMemo(memo)

        val latch = CountDownLatch(1)
        val listenerRegistration = kinAccountReceiver.addPaymentListener { _ -> latch.countDown() }

        val transactionId = kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(),
                BigDecimal("21.123"), WhitelistServiceForTest(), memo)
        assertThat(kinAccountSender.balanceSync.value(), equalTo(BigDecimal("78.87700")))
        assertThat(kinAccountReceiver.balanceSync.value(), equalTo(BigDecimal("21.12300")))

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))
        listenerRegistration.remove()

        val server = Server(TEST_SDK_NETWORK_URL)
        val transactionResponse = server.transactions().transaction(transactionId.id())
        val actualMemo = transactionResponse.memo
        assertThat<Memo>(actualMemo, `is`<Memo>(instanceOf<Memo>(MemoText::class.java)))
        assertThat((actualMemo as MemoText).text, equalTo(expectedMemo))
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendTransaction_ReceiverAccountNotCreated_AccountNotFoundException() {
        val kinAccountSender = kinClient.addAccount()
        val kinAccountReceiver = kinClient.addAccount()

        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = kinAccountSender.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }

        fakeKinSdkOnBoard.createAccount(kinAccountSender.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))

        expectedEx.expect(AccountNotFoundException::class.java)
        expectedEx.expectMessage(kinAccountReceiver.publicAddress)
        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("21.123"), WhitelistServiceForTest())

    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendTransaction_SenderAccountNotCreated_AccountNotFoundException() {
        val kinAccountSender = kinClient.addAccount()
        val kinAccountReceiver = kinClient.addAccount()

        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = kinAccountReceiver.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }

        fakeKinSdkOnBoard.createAccount(kinAccountReceiver.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))

        expectedEx.expect(AccountNotFoundException::class.java)
        expectedEx.expectMessage(kinAccountSender.publicAddress)
        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("21.123"), WhitelistServiceForTest())
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendWhitelistTransaction_ReceiverAccountNotCreated_AccountNotFoundException() {
        val kinAccountSender = kinClient.addAccount()
        val kinAccountReceiver = kinClient.addAccount()

        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = kinAccountSender.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }

        fakeKinSdkOnBoard.createAccount(kinAccountSender.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))

        expectedEx.expect(AccountNotFoundException::class.java)
        expectedEx.expectMessage(kinAccountReceiver.publicAddress)

        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("21.123"), WhitelistServiceForTest())

    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendWhitelistTransaction_SenderAccountNotCreated_AccountNotFoundException() {
        val kinAccountSender = kinClient.addAccount()
        val kinAccountReceiver = kinClient.addAccount()

        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = kinAccountReceiver.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }

        fakeKinSdkOnBoard.createAccount(kinAccountReceiver.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))

        expectedEx.expect(AccountNotFoundException::class.java)
        expectedEx.expectMessage(kinAccountSender.publicAddress)

        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("21.123"), WhitelistServiceForTest())
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendWhitelistTransaction_Success() {
        val (kinAccountSender, kinAccountReceiver) = onboardAccounts(senderFundAmount = 100)

        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("20"), WhitelistServiceForTest())
        assertThat(kinAccountSender.balanceSync.value(), equalTo(BigDecimal("80.00000")))
        assertThat(kinAccountReceiver.balanceSync.value(), equalTo(BigDecimal("20.00000")))
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun createPaymentListener_ListenToReceiver_PaymentEvent() {
        listenToPayments(false)
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun createPaymentListener_ListenToSender_PaymentEvent() {
        listenToPayments(true)
    }

    @Throws(Exception::class)
    private fun listenToPayments(sender: Boolean) {
        //create and sets 2 accounts (receiver/sender), fund one account, and then
        //send transaction from the funded account to the other, observe this transaction using listeners
        val fundingAmount = BigDecimal("100")
        val transactionAmount = BigDecimal("21.123")

        val (kinAccountSender, kinAccountReceiver) = onboardAccounts(0, 0)

        //register listeners for testing
        val actualPaymentsResults = ArrayList<IPaymentInfo>()
        val actualBalanceResults = ArrayList<IBalance>()
        val accountToListen = if (sender) kinAccountSender else kinAccountReceiver

        val eventsCount = if (sender) 4 else 2 ///in case of observing the sender we'll get 2 events (1 for funding 1 for the
        //transaction) in case of receiver - only 1 event. multiply by 2, as we 2 listeners (balance and payment)
        val latch = CountDownLatch(eventsCount)
        val paymentListener = accountToListen.addPaymentListener { data ->
            actualPaymentsResults.add(data)
            latch.countDown()
        }
        val balanceListener = accountToListen.addBalanceListener { data ->
            actualBalanceResults.add(data)
            latch.countDown()
        }

        //send the transaction we want to observe
        fakeKinSdkOnBoard.fundWithKin(kinAccountSender.publicAddress.orEmpty(), "100")
        val memo = "memo"
        val expectedMemo = addAppIdToMemo(memo)
        val expectedTransactionId = kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(),
                transactionAmount, WhitelistServiceForTest(), memo)

        //verify data notified by listeners
        val transactionIndex = if (sender) 1 else 0 //in case of observing the sender we'll get 2 events (1 for funding 1 for the
        //transaction) in case of receiver - only 1 event
        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))
        paymentListener.remove()
        balanceListener.remove()
        val paymentInfo = actualPaymentsResults[transactionIndex]
        assertThat(paymentInfo.amount(), equalTo(transactionAmount))
        assertThat(paymentInfo.destinationPublicKey(), equalTo(kinAccountReceiver.publicAddress))
        assertThat(paymentInfo.sourcePublicKey(), equalTo(kinAccountSender.publicAddress))
        assertThat(paymentInfo.memo(), equalTo(expectedMemo))
        assertThat(paymentInfo.hash().id(), equalTo(expectedTransactionId.id()))

        val balance = actualBalanceResults[transactionIndex]
        assertThat(balance.value(),
                equalTo(if (sender) fundingAmount.subtract(transactionAmount) else transactionAmount))
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun createPaymentListener_RemoveListener_NoEvents() {
        val (kinAccountSender, kinAccountReceiver) = onboardAccounts(senderFundAmount = 100)

        val latch = CountDownLatch(1)
        val listenerRegistration = kinAccountReceiver.addPaymentListener {
            fail("should not get eny event!")
        }
        listenerRegistration.remove()

        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("21.123"), WhitelistServiceForTest())
        latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS)
    }

    @Test
    @LargeTest
    @Throws(Exception::class)
    fun sendTransaction_NotEnoughKin_InsufficientKinException() {
        val (kinAccountSender, kinAccountReceiver) = onboardAccounts()

        expectedEx.expect(InsufficientKinException::class.java)
        kinAccountSender.sendTransactionSync(kinAccountReceiver.publicAddress.orEmpty(), BigDecimal("21.123"), WhitelistServiceForTest())
    }

    private fun onboardAccounts(senderFundAmount: Int = 0,
                                receiverFundAmount: Int = 0): Pair<IKinAccount, IKinAccount> {
        val kinAccountSender = kinClient.addAccount()
        val kinAccountReceiver = kinClient.addAccount()
        onboardSingleAccount(kinAccountSender, senderFundAmount)
        onboardSingleAccount(kinAccountReceiver, receiverFundAmount)
        return Pair(kinAccountSender, kinAccountReceiver)
    }

    private fun onboardSingleAccount(account: IKinAccount, fundAmount: Int) {
        val latch = CountDownLatch(1)

        var listenerRegistration : IListenerRegistration? = null
        listenerRegistration = account.addAccountCreationListener {
            listenerRegistration?.remove()
            if (fundAmount > 0) {
                fakeKinSdkOnBoard.fundWithKin(account.publicAddress.orEmpty(), fundAmount.toString())
            }
            latch.countDown()
        }

        fakeKinSdkOnBoard.createAccount(account.publicAddress.orEmpty())

        assertTrue(latch.await(timeoutDurationSeconds, TimeUnit.SECONDS))
    }

    private fun addAppIdToMemo(memo: String): String {
        return appIdVersionPrefix.plus("-").plus(appId).plus("-").plus(memo)
    }

    companion object {

        private lateinit var fakeKinSdkOnBoard: FakeKinSdkOnBoard

        @BeforeClass
        @JvmStatic
        @Throws(IOException::class)
        fun setupKinOnBoard() {
            fakeKinSdkOnBoard = FakeKinSdkOnBoard()
        }
    }
}