package kin.sdk.migration

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import kin.sdk.migration.IntegConsts.TEST_CORE_URL_FUND
import kin.sdk.migration.IntegConsts.TEST_SDK_URL_CREATE_ACCOUNT
import kin.sdk.migration.MigrationManager.KIN_MIGRATION_COMPLETED_KEY
import kin.sdk.migration.MigrationManager.KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY
import kin.sdk.migration.bi.IMigrationEventsListener
import kin.sdk.migration.common.exception.AccountNotFoundException
import kin.sdk.migration.common.exception.FailedToResolveSdkVersionException
import kin.sdk.migration.common.exception.MigrationInProcessException
import kin.sdk.migration.common.exception.TransactionFailedException
import kin.sdk.migration.common.interfaces.*
import kin.sdk.migration.internal.core_related.KinAccountCoreImpl
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail


@Suppress("FunctionName")
class MigrationManagerIntegrationTest {

    private val coreIssuer = "GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7"
    private val migrationServiceUrl = "https://migration-devplatform-playground.developers.kinecosystem.com/migrate?address="
    private val fundKinAmount = 10

    // Op Line Full occurs when a payment would cause a destination account to
    // exceed their declared trust limit for the asset being sent.
    private val LINE_FULL_RESULT_CODE = "op_line_full"

    private val timeoutDurationSecondsLong: Long = 20
    private val timeoutDurationSecondsShort: Long = 10
    private val timeoutDurationSecondsVeryShort: Long = 2
    private lateinit var migrationManagerOldKin: MigrationManager
    private lateinit var migrationManagerNewKin: MigrationManager
    private val networkInfo = MigrationNetworkInfo(IntegConsts.TEST_CORE_NETWORK_URL, IntegConsts.TEST_CORE_NETWORK_ID, IntegConsts.TEST_SDK_NETWORK_URL,
            IntegConsts.TEST_SDK_NETWORK_ID, coreIssuer, migrationServiceUrl)

    @Mock
    lateinit var eventListener: IMigrationEventsListener

    @Rule
    @JvmField
    val expectedEx: ExpectedException = ExpectedException.none()

    private val migrationManagerCallbacks = object : IMigrationManagerCallbacks {
        override fun onMigrationStart() {
        }

        override fun onReady(kinClient: IKinClient) {
        }

        override fun onError(e: Exception) {
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        removeData()
        migrationManagerOldKin = getNewMigrationManager(IKinVersionProvider { kin.sdk.migration.common.KinSdkVersion.OLD_KIN_SDK })
        migrationManagerNewKin = getNewMigrationManager(IKinVersionProvider { kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK })
    }

    @After
    fun teardown() {
        removeData()
    }

    private fun removeData() {
        val sharedPreferences = getSharedPreferences()
        sharedPreferences.edit().clear().apply()
    }

    @Test
    @LargeTest
    fun start_OldKinBlockchain_getOldKinClient() {
        val oldKinClient = getKinClientOnOldKinBlockchain()
        // add account only locally but not in the blockchain
        oldKinClient?.addAccount()
        val oldKinAccount = oldKinClient?.getAccount(oldKinClient.accountCount - 1)
        assertEquals(oldKinAccount?.kinSdkVersion, kin.sdk.migration.common.KinSdkVersion.OLD_KIN_SDK)
    }

    @Test
    @LargeTest
    fun start_NewKinBlockchain_AccountNotFound_getNewKinClient() {
        // getting a kin client just in order to use it for creating a local account and then start the migration.
        val oldKinClient = getKinClientOnOldKinBlockchain()
        // add account only locally but not in the blockchain
        oldKinClient?.addAccount()
        // starting the migration with a user that only have a local account
        val newKinClient = getKinClientOnNewKinBlockchain()
        val newAccount = newKinClient?.getAccount(newKinClient.accountCount - 1)
        assertEquals(newAccount?.kinSdkVersion, kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
    }

    @Test
    @LargeTest
    fun start_NewKinBlockchain_AccountNotActivated_getNewKinClient() {
        // getting a kin client just in order to use it for creating a local account and then start the migration.
        val oldKinClient = getKinClientOnOldKinBlockchain()
        // add account only locally but not in the blockchain
        val account = oldKinClient?.addAccount()
        // create account on old kin blockchain but not activate it.
        createAccount(account)
        // starting the migration with a user that have an account also on the blockchain but it is not activated
        val newKinClient = getKinClientOnNewKinBlockchain()
        val newAccount = newKinClient?.getAccount(newKinClient.accountCount - 1)
        assertEquals(newAccount?.kinSdkVersion, kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
    }

    @Test
    @LargeTest
    fun start_StartWhileInProcess_MigrationInProcessException() {
        assertFailsWith(MigrationInProcessException::class) {
            val migrationManager = getNewMigrationManager(IKinVersionProvider {
                // waiting so it will be in the middle of the migration process while starting another one.
                Thread.sleep(3000)
                kin.sdk.migration.common.KinSdkVersion.OLD_KIN_SDK
            })
            migrationManager.start(migrationManagerCallbacks)
            // waiting a bit so the started migration will be in the process.
            Thread.sleep(1000)
            migrationManager.start(migrationManagerCallbacks)
        }
    }

    @Test
    @LargeTest
    fun start_NeedToMigrate_getNewMigratedKinClient() {
        val migrationDidStart = AtomicBoolean()
        val isNewSdk = AtomicBoolean()
        val isAccountAlreadyMigrated = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        val kinClient = getKinClientOnOldKinBlockchain()
        createAccountActivateAndMigrate(kinClient, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                val account = kinClient.getAccount(kinClient.accountCount - 1)
                isNewSdk.set(account.kinSdkVersion == kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
                isAccountAlreadyMigrated.set(isAccountAlreadyMigrated(account.publicAddress))
                latch.countDown()
            }

            override fun onError(e: Exception) {
                error = Error(e)
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(migrationDidStart.get())
        assertTrue(isNewSdk.get())
        assertTrue(isAccountAlreadyMigrated.get())
        assertThat(error?.exception, Matchers.nullValue())
    }


    @Test
    @LargeTest
    fun start_MigrationAlreadyCompleted_getNewKinClient() {
        val migrationDidStart = AtomicBoolean()
        val isNewSdk = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        val newKinClient = getNewKinClientAfterMigration()
        val publicAddress = newKinClient?.getAccount(newKinClient.accountCount - 1)?.publicAddress
        assertTrue(isAccountAlreadyMigrated(publicAddress))
        val migrationManager = getNewMigrationManager(IKinVersionProvider { kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK })
        migrationManager.start(publicAddress, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                isNewSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
                latch.countDown()
            }

            override fun onError(e: Exception) {
                error = Error(e)
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(!migrationDidStart.get())
        assertTrue(isNewSdk.get())
        assertThat(error?.exception, Matchers.nullValue())
    }

    @Test
    @LargeTest
    fun start_NoVersionReceived_FailedToResolveSdkVersionException() {
        val migrationDidStart = AtomicBoolean()
        val gotClient = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        val oldKinClient = getKinClientOnOldKinBlockchain()
        val account = oldKinClient?.addAccount()
        createAccount(account)
        val migrationManager = getNewMigrationManager(IKinVersionProvider { throw FailedToResolveSdkVersionException() })
        migrationManager.start(account?.publicAddress, object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                gotClient.set(true)
            }

            override fun onError(e: java.lang.Exception) {
                error = Error(e)
                assertTrue(e is FailedToResolveSdkVersionException)
                latch.countDown()
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(!migrationDidStart.get())
        assertTrue(!gotClient.get())
        assertThat(error?.exception, Matchers.`is`(instanceOf(FailedToResolveSdkVersionException::class.java)))
    }

    @Test
    @LargeTest
    fun backupOldKinAccount_CreateNewKinAccountAndMigrate_ImportAccountAndMigrate_success() {
        // create account which we will backup
        val accountToBackup: KinAccountCoreImpl = createActivateAndFundOldKinAccount() as KinAccountCoreImpl
        val accountToBackupBalance = accountToBackup.balanceSync
        val passphrase = "qwerty123"
        val export = accountToBackup.export(passphrase)
        // create regular old account which will now be the active account.
        val oldAccountToMigrate = createActivateAndFundOldKinAccount()
        // migrate this account
        val kinClientAfterMigration = getKinClientOnNewKinBlockchain()
        // verify that the migrated account is indeed the current active account
        assertThat(oldAccountToMigrate?.publicAddress, `is`(equalTo(kinClientAfterMigration?.getAccount(kinClientAfterMigration.accountCount - 1)?.publicAddress)))
        val importAccount = kinClientAfterMigration?.importAccount(export, passphrase)
        // verify that the after the restore then the backup account is now indeed the current active account
        assertThat(accountToBackup.publicAddress, `is`(equalTo(kinClientAfterMigration?.getAccount(kinClientAfterMigration.accountCount - 1)?.publicAddress)))
        assertFailsWith(AccountNotFoundException::class) {
            importAccount?.balanceSync
        }
        // migrate backup account
        getKinClientOnNewKinBlockchain()
        val balanceSync = importAccount?.balanceSync
        // verify that after migration the balance of the backup account is equal to the balance he had before restore and migrate
        assertEquals(balanceSync?.value()?.compareTo(accountToBackupBalance.value()), 0)
    }

    @Test
    @LargeTest
    fun start_AlreadyMigrated_ClearMigratedFlag_ZeroBalance_AccountNotActivated_getNewKinClient() {
        start_AlreadyMigrated_ClearMigratedFlag_getNewKinClient(false)
    }

    //TODO differentiate between those 2 tests using the events, meaning add verify to events.

    @Test
    @LargeTest
    fun start_AlreadyMigratedBut_ClearMigratedFlag_WithBalance_getNewKinClient() {
        start_AlreadyMigrated_ClearMigratedFlag_getNewKinClient(true)
    }

    private fun start_AlreadyMigrated_ClearMigratedFlag_getNewKinClient(withBalance: Boolean) {
        val migrationDidStart = AtomicBoolean()
        val isNewSdk = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        val kinClient = getNewKinClientAfterMigration()
        removeData()
        val account = kinClient?.getAccount(kinClient.accountCount)
        if (withBalance) { //TODO do i really want to fund the new migrated account???
            fakeKinIssuer.fundWithKin(String.format(TEST_SDK_URL_CREATE_ACCOUNT + fundKinAmount, account))
        }

        val migrationManager = getNewMigrationManager(IKinVersionProvider { kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK })
        migrationManager.start(account?.publicAddress, object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                isNewSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
                latch.countDown()
            }

            override fun onError(e: java.lang.Exception) {
                error = Error(e)
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(migrationDidStart.get())
        assertTrue(isNewSdk.get())
        assertThat(error?.exception, Matchers.nullValue())
    }

    @Test
    @LargeTest
    fun start_AlreadyBurnedAccountButNotMigrated_ZeroBalance_AccountNotActivated_getNewKinClient() {
        start_AlreadyBurnedAccountButNotMigrated_getNewKinClient(false)
    }

    @Test
    @LargeTest
    fun start_AlreadyBurnedAccountButNotMigrated_WithBalance_MigrateAndGetNewKinClient() {
        start_AlreadyBurnedAccountButNotMigrated_getNewKinClient(true)
    }

    private fun start_AlreadyBurnedAccountButNotMigrated_getNewKinClient(withBalance: Boolean) {
        val migrationDidStart = AtomicBoolean()
        val isNewSdk = AtomicBoolean()
        var error: Error? = null
        val oldKinClient = getKinClientOnOldKinBlockchain()
        val account = oldKinClient?.addAccount()
        createAccount(account)
        activateAccount(account)
        if (withBalance) {
            fakeKinIssuer.fundWithKin(String.format(TEST_SDK_URL_CREATE_ACCOUNT + fundKinAmount, account?.publicAddress))
        }
        if (account is KinAccountCoreImpl) {
            val latch = CountDownLatch(1)
            account.sendBurnTransactionSync(account.publicAddress.orEmpty())
            migrationManagerNewKin.start(account?.publicAddress, object : IMigrationManagerCallbacks {
                override fun onMigrationStart() {
                    migrationDidStart.set(true)
                }

                override fun onReady(kinClient: IKinClient) {
                    isNewSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
                    latch.countDown()
                }

                override fun onError(e: java.lang.Exception) {
                    error = Error(e)
                }

            })
            assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        } else {
            fail("oldKinAccount should be from type KinAccountCoreImpl in order to burn the account")
        }
        assertTrue(migrationDidStart.get())
        assertTrue(isNewSdk.get())
        assertThat(error?.exception, Matchers.nullValue())
    }

    @Test
    @LargeTest
    fun start_burn_sendKinTBurnedAccount_TransactionFailedException() {
        val assertFailsWith = assertFailsWith(TransactionFailedException::class) {
            val oldAccount1 = createActivateAndFundOldKinAccount()
            getKinClientOnNewKinBlockchain()
            removeData()
            val oldAccount2 = createActivateAndFundOldKinAccount()
            oldAccount2?.sendTransactionSync(oldAccount1?.publicAddress.orEmpty(), BigDecimal(10), null)
        }
        assertThat(assertFailsWith.message, containsString(LINE_FULL_RESULT_CODE))

    }

    private fun createActivateAndFundOldKinAccount(): IKinAccount? {
        val oldKinClient = getKinClientOnOldKinBlockchain()
        val account = oldKinClient?.addAccount()
        createAccount(account)
        activateAccount(account)
        fakeKinIssuer.fundWithKin(String.format(TEST_CORE_URL_FUND + fundKinAmount, account?.publicAddress))
        return account
    }

    private fun getNewKinClientAfterMigration(): IKinClient? {
        val migrationDidStart = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        var migratedKinClient: IKinClient? = null
        val kinClient = getKinClientOnOldKinBlockchain()
        createAccountActivateAndMigrate(kinClient, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                migratedKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                error = MigrationManagerIntegrationTest.Error(e)
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(migrationDidStart.get())
        assertThat(error?.exception, Matchers.nullValue())

        return migratedKinClient
    }

    private fun createAccountActivateAndMigrate(kinClient: IKinClient?, migrationManagerCallbacks: IMigrationManagerCallbacks) {
        val account = kinClient?.addAccount()
        // Create account on old kin blockchain.
        createAccount(account)
        // Activate the account.
        activateAccount(account)
        // starting the migration with a user that have an account which is also activated so migration should succeed.
        migrationManagerNewKin.start(account?.publicAddress, migrationManagerCallbacks)
    }

    private fun isAccountAlreadyMigrated(publicAddress: String?): Boolean {
        val sharedPreferences = getSharedPreferences()
        return sharedPreferences.getBoolean(KIN_MIGRATION_COMPLETED_KEY + publicAddress, false)
    }

    // Activate account on the blockchain.
    // Note that this method is blocking.
    private fun activateAccount(account: IKinAccount?) {
        account?.activateSync()
    }

    // Create account on the blockchain and when creation complete then the method will end.
    private fun createAccount(account: IKinAccount?) {
        // run it on a background thread because this method is called from the main ui thread.
        fakeKinIssuer.createAccount(account?.publicAddress.orEmpty())

        val latch = CountDownLatch(1)
        var listenerRegistration: IListenerRegistration? = null
        listenerRegistration = account?.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }
        assertTrue(latch.await(timeoutDurationSecondsShort, TimeUnit.SECONDS))
    }

    private fun getNewMigrationManager(kinVersionProvider: IKinVersionProvider): MigrationManager {
        return MigrationManager(InstrumentationRegistry.getTargetContext(), "test",
                networkInfo, kinVersionProvider, eventListener)
    }

    private fun getKinClientOnNewKinBlockchain(): IKinClient? {
        val isNewSdk = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        var newKinClient: IKinClient? = null
        migrationManagerNewKin.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
            }

            override fun onReady(kinClient: IKinClient) {
                isNewSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == kin.sdk.migration.common.KinSdkVersion.NEW_KIN_SDK)
                newKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                error = Error(e)
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(isNewSdk.get())
        assertThat(error?.exception, Matchers.nullValue())
        return newKinClient
    }

    private fun getKinClientOnOldKinBlockchain(): IKinClient? {
        val migrationDidStart = AtomicBoolean()
        val isOldSdk = AtomicBoolean()
        var error: Error? = null
        val latch = CountDownLatch(1)
        var oldKinClient: IKinClient? = null
        migrationManagerOldKin.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                if (kinClient.hasAccount()) {
                    isOldSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == kin.sdk.migration.common.KinSdkVersion.OLD_KIN_SDK)
                }
                oldKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                error = Error(e)
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsVeryShort, TimeUnit.SECONDS))
        assertTrue(!migrationDidStart.get())

        if (oldKinClient?.hasAccount() == true) {
            assertTrue(isOldSdk.get())
        }

        assertThat(error?.exception, Matchers.nullValue())
        return oldKinClient
    }

    private fun getSharedPreferences(): SharedPreferences {
        return InstrumentationRegistry.getTargetContext().getSharedPreferences(KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
    }

    companion object {

        private lateinit var fakeKinIssuer: FakeKinCoreIssuer

        @BeforeClass
        @JvmStatic
        @Throws(IOException::class)
        fun setupKinIssuer() {
            fakeKinIssuer = FakeKinCoreIssuer()
        }

    }

    class Error(val exception: java.lang.Exception, val message: String = "")

}