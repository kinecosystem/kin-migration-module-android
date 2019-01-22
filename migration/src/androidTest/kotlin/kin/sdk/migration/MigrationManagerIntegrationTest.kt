package kin.sdk.migration

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import android.support.test.filters.LargeTest
import kin.sdk.migration.MigrationManager.KIN_MIGRATION_COMPLETED_KEY
import kin.sdk.migration.MigrationManager.KIN_MIGRATION_MODULE_PREFERENCE_FILE_KEY
import kin.sdk.migration.bi.IMigrationEventsListener
import kin.sdk.migration.core_related.KinAccountCoreImpl
import kin.sdk.migration.exception.FailedToResolveSdkVersionException
import kin.sdk.migration.exception.MigrationInProcessException
import kin.sdk.migration.interfaces.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.instanceOf
import org.junit.*
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail


@Suppress("FunctionName")
class MigrationManagerIntegrationTest {

    private val sdkTestNetworkUrl = "http://horizon-testnet.kininfrastructure.com/"
    private val sdkTestNetworkId = "Kin Testnet ; December 2018"
    private val coreTestNetworkUrl = "https://horizon-playground.kininfrastructure.com/"
    private val coreTestNetworkId = "Kin Playground Network ; June 2018"
    private val coreIssuer = "GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7"
    private val fundKinAmount = 10
    private val urlFund = "http://faucet-playground.kininfrastructure.com/fund?account=%s&amount=$fundKinAmount"

    // Op Line Full occurs when a payment would cause a destination account to
    // exceed their declared trust limit for the asset being sent.
    private val LINE_FULL_RESULT_CODE = "op_line_full"

    private val timeoutDurationSecondsLong: Long = 50 //TODO need to change to a normal number, maybe 15 seconds
    private val timeoutDurationSecondsShort: Long = 10 //TODO need to change to a normal number, maybe 5 seconds
    private val timeoutDurationSecondsVeryShort: Long = 5 //TODO need to change to a normal number, maybe 2 seconds
    private lateinit var migrationManagerOldKin: MigrationManager
    private lateinit var migrationManagerNewKin: MigrationManager
    private val networkInfo = MigrationNetworkInfo(coreTestNetworkUrl, coreTestNetworkId, sdkTestNetworkUrl,
            sdkTestNetworkId, coreIssuer)

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
        migrationManagerOldKin = getNewMigrationManager(IKinVersionProvider { KinSdkVersion.OLD_KIN_SDK })
        migrationManagerNewKin = getNewMigrationManager(IKinVersionProvider { KinSdkVersion.NEW_KIN_SDK })
    }

    @After
    fun teardown() {
        removeData()
    }

    private fun removeData() {
        val sharedPreferences = getSharedPreferences()
        sharedPreferences.edit().remove(KIN_MIGRATION_COMPLETED_KEY).apply()
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
        assertEquals(newAccount?.kinSdkVersion, KinSdkVersion.NEW_KIN_SDK)
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
        assertEquals(newAccount?.kinSdkVersion, KinSdkVersion.NEW_KIN_SDK)
    }

    @Test
    @LargeTest
    fun start_StartWhileInProcess_MigrationInProcessException() {
        assertFailsWith(MigrationInProcessException::class) {
            val migrationManager = getNewMigrationManager(IKinVersionProvider {
                // waiting so it will be in the middle of the migration process while starting another one.
                Thread.sleep(3000)
                KinSdkVersion.OLD_KIN_SDK
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

        val latch = CountDownLatch(1)
        val kinClient = getKinClientOnOldKinBlockchain()
        var migrationStarted = false
        createAccountActivateAndMigrate(kinClient, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                migrationStarted = true
            }

            override fun onReady(kinClient: IKinClient) {
                migrationDidStart.set(migrationStarted)
                isNewSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == KinSdkVersion.NEW_KIN_SDK)
                isAccountAlreadyMigrated.set(isAccountAlreadyMigrated())
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(migrationDidStart.get())
        assertTrue(isNewSdk.get())
        assertTrue(isAccountAlreadyMigrated.get())
    }


    @Test
    @LargeTest
    fun start_MigrationAlreadyCompleted_getNewKinClient() {
        val migrationDidStart = AtomicBoolean()
        val isNewSdk = AtomicBoolean()
        val gotError = AtomicBoolean()
        val latch = CountDownLatch(1)
        getNewKinClientAfterMigration()// blocking call
        assertTrue(isAccountAlreadyMigrated())
        val migrationManager = getNewMigrationManager(IKinVersionProvider { KinSdkVersion.NEW_KIN_SDK })
        migrationManager.start(object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                isNewSdk.set(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion == KinSdkVersion.NEW_KIN_SDK)
                latch.countDown()
            }

            override fun onError(e: Exception) {
                gotError.set(true)
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(!migrationDidStart.get())
        assertTrue(isNewSdk.get())
        assertTrue(!gotError.get())
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
        migrationManager.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
                migrationDidStart.set(true)
            }

            override fun onReady(kinClient: IKinClient) {
                gotClient.set(true)
            }

            override fun onError(e: java.lang.Exception) {
                error = MigrationManagerIntegrationTest.Error(e)
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
    fun start_AlreadyMigratedButNotSavedLocally_ZeroBalance_AccountNotActivated_getNewKinClient() {
        //TODO keep testings from here
        val latch = CountDownLatch(1)
        getNewKinClientAfterMigration()// blocking call
        teardown()
        val migrationManager = getNewMigrationManager(IKinVersionProvider { KinSdkVersion.NEW_KIN_SDK })
        var migrationStarted = false
        migrationManager.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
                migrationStarted = true
            }

            override fun onReady(kinClient: IKinClient) {
                assertTrue(migrationStarted)
                assertEquals(kinClient.getAccount(kinClient.accountCount - 1).getKinSdkVersion(), KinSdkVersion.NEW_KIN_SDK)
                latch.countDown()
            }

            override fun onError(e: java.lang.Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
    }

    @Test
    @LargeTest
    fun start_AlreadyMigratedButNotSavedLocally_WithBalance_getNewKinClient() {
        val latch = CountDownLatch(1)
        val kinClient = getNewKinClientAfterMigration()// blocking call
        teardown()
        fakeKinIssuer.fundWithKin(String.format(urlFund, kinClient?.getAccount(kinClient.accountCount)))

        val migrationManager = getNewMigrationManager(IKinVersionProvider { KinSdkVersion.NEW_KIN_SDK })
        migrationManager.start(object : IMigrationManagerCallbacks {
            var migrationStarted = false
            override fun onMigrationStart() {
                migrationStarted = true
            }

            override fun onReady(kinClient: IKinClient) {
                assertTrue(migrationStarted)
                assertEquals(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion, KinSdkVersion.NEW_KIN_SDK)
                latch.countDown()
            }

            override fun onError(e: java.lang.Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
    }

    @Test
    @LargeTest
    fun start_AlreadyBurnedAccountButNotMigrated_ZeroBalance_AccountNotActivated_getNewKinClient() {
        val oldKinClient = getKinClientOnOldKinBlockchain()
        val account = oldKinClient?.addAccount()
        createAccount(account)
        activateAccount(account)
        if (account is KinAccountCoreImpl) {
            val latch = CountDownLatch(1)
            account.sendBurnTransactionSync(account.publicAddress.orEmpty())
            var migrationStarted = false
            migrationManagerNewKin.start(object : IMigrationManagerCallbacks {
                override fun onMigrationStart() {
                    migrationStarted = true
                }

                override fun onReady(kinClient: IKinClient) {
                    assertTrue(migrationStarted)
                    assertEquals(kinClient.getAccount(kinClient.accountCount - 1).kinSdkVersion, KinSdkVersion.NEW_KIN_SDK)
                    latch.countDown()
                }

                override fun onError(e: java.lang.Exception) {
                    fail("not supposed to reach onError with this exception: $e")
                }

            })
            assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        } else {
            fail("oldKinAccount should be from type KinAccountCoreImpl in order to burn the account")
        }
    }

    @Test
    @LargeTest
    fun start_AlreadyBurnedAccountButNotMigrated_WithBalance_MigrateAndGetNewKinClient() {
        //TODO need to implement after we check how to fund the account with the current issuer and the random issuer because then migration could not succeed
    }

    //TODO should we test all kind of scenarios like it was burned but then network or something fail and he didn't migrate
    //TODO and then in a different time he tries again?
    //TODO maybe test also if account is burned and migrated or burned but not migrated

    private fun getNewKinClientAfterMigration(): IKinClient? {
        val migrationDidStart = AtomicBoolean()
        val latch = CountDownLatch(1)
        var migratedKinClient: IKinClient? = null
        val kinClient = getKinClientOnOldKinBlockchain()
        var startMigration = false
        createAccountActivateAndMigrate(kinClient, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                startMigration = true
            }

            override fun onReady(kinClient: IKinClient) {
                migrationDidStart.set(startMigration)
                migratedKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
        assertTrue(migrationDidStart.get())
        return migratedKinClient
    }

    private fun createAccountActivateAndMigrate(kinClient: IKinClient?, migrationManagerCallbacks: IMigrationManagerCallbacks) {
        val account = kinClient?.addAccount()
        // Create account on old kin blockchain.
        createAccount(account)
        // Activate the account.
        activateAccount(account)
        // starting the migration with a user that have an account which is also activated so migration should succeed.
        migrationManagerNewKin.start(migrationManagerCallbacks)
    }

    private fun isAccountAlreadyMigrated(): Boolean {
        val sharedPreferences = getSharedPreferences()
        return sharedPreferences.getBoolean(KIN_MIGRATION_COMPLETED_KEY, false)
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
        val latch = CountDownLatch(1)
        var newKinClient: IKinClient? = null
        migrationManagerNewKin.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {

            }

            override fun onReady(kinClient: IKinClient) {
                newKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))

        return newKinClient
    }

    private fun getKinClientOnOldKinBlockchain(): IKinClient? {
        val latch = CountDownLatch(1)
        var oldKinClient: IKinClient? = null
        migrationManagerOldKin.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {

            }

            override fun onReady(kinClient: IKinClient) {
                oldKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsVeryShort, TimeUnit.SECONDS))

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