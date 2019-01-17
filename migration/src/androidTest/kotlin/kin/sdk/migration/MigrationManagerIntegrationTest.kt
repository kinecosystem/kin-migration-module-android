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
import org.junit.*
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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

    private val timeoutDurationSecondsLong: Long = 50 //TODO need to change to a normal number, maybe 15 seconds
    private val timeoutDurationSecondsShort: Long = 10 //TODO need to change to a normal number, maybe 5 seconds
    private val timeoutDurationSecondsVeryShort: Long = 2
    private lateinit var migrationManagerOldKin: MigrationManager
    private lateinit var migrationManagerNewKin: MigrationManager
    private val networkInfo = MigrationNetworkInfo(coreTestNetworkUrl, coreTestNetworkId, sdkTestNetworkUrl,
            sdkTestNetworkId, coreIssuer)

    @Mock
    lateinit var eventListener: IMigrationEventsListener

    @Rule
    @JvmField
    val expectedEx: ExpectedException = ExpectedException.none()

    private val migrationManagerListener = object : IMigrationManagerCallbacks {
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
        val latch = CountDownLatch(1)
        // getting a kin client just in order to use it for creating a local account and then start the migration.
        val oldKinClient = getKinClientOnOldKinBlockchain()
        // add account only locally but not in the blockchain
        oldKinClient?.addAccount()
        // starting the migration with a user that only have a local account
        val newKinClient = getKinClientOnNewKinBlockchain()
        val newAccount = newKinClient?.getAccount(newKinClient.accountCount - 1)
        assertEquals(newAccount?.kinSdkVersion, KinSdkVersion.NEW_KIN_SDK)
        latch.countDown()
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
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
            val latch = CountDownLatch(1)
            val migrationManager = getNewMigrationManager(IKinVersionProvider {
                // waiting so it will be in the middle of the migration process while starting another one.
                latch.await(timeoutDurationSecondsShort, TimeUnit.SECONDS)
                KinSdkVersion.OLD_KIN_SDK
            })
            migrationManager.start(migrationManagerListener)
            // waiting a bit so the started migration will be in the process.
            Thread.sleep(2000)
            migrationManager.start(migrationManagerListener)
            latch.countDown()
        }
    }

    @Test
    @LargeTest
    fun start_NeedToMigrate_getNewMigratedKinClient() {
        val latch = CountDownLatch(1)
        val kinClient = getKinClientOnOldKinBlockchain()
        var migrationStarted = false
        createAccountActivateAndMigrate(kinClient, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                migrationStarted = true
            }

            override fun onReady(kinClient: IKinClient) {
                assertTrue(migrationStarted)
                assertEquals(kinClient.getAccount(kinClient.accountCount - 1).getKinSdkVersion(), KinSdkVersion.NEW_KIN_SDK)
                assertTrue(isAccountAlreadyMigrated())
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
    }


    @Test
    @LargeTest
    fun start_MigrationAlreadyCompleted_getNewKinClient() {
        val latch = CountDownLatch(1)
        getNewKinClientAfterMigration()// blocking call
        assertTrue(isAccountAlreadyMigrated())
        val migrationManager = getNewMigrationManager(IKinVersionProvider { KinSdkVersion.NEW_KIN_SDK })
        migrationManager.start(object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                fail("not supposed to reach onMigrationStart")
            }

            override fun onReady(kinClient: IKinClient) {
                assertEquals(kinClient.getAccount(kinClient.accountCount - 1).getKinSdkVersion(), KinSdkVersion.NEW_KIN_SDK)
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }
        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
    }

    @Test
    @LargeTest
    fun start_NoVersionReceived_FailedToResolveSdkVersionException() {
        val latch = CountDownLatch(1)
        val oldKinClient = getKinClientOnOldKinBlockchain()
        val account = oldKinClient?.addAccount()
        createAccount(account)
        val migrationManager = getNewMigrationManager(IKinVersionProvider { throw FailedToResolveSdkVersionException() })
        migrationManager.start(object : IMigrationManagerCallbacks {
            override fun onMigrationStart() {
                fail("not supposed to reach onMigrationStart")
            }

            override fun onReady(kinClient: IKinClient) {
                fail("not supposed to reach onReady")
            }

            override fun onError(e: java.lang.Exception) {
                assertTrue(e is FailedToResolveSdkVersionException)
                latch.countDown()
            }

        })
        assertTrue(latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS))
    }

    @Test
    @LargeTest
    fun start_AlreadyMigratedButNotSavedLocally_ZeroBalance_AccountNotActivated_getNewKinClient() {
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
        getNewKinClientAfterMigration()// blocking call
        teardown()
        fail("remove this fail after finished with the TODO")//TODO  add funding with faucet
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
        val latch = CountDownLatch(1)
        var migratedKinClient: IKinClient? = null
        val kinClient = getKinClientOnOldKinBlockchain()
        var startMigration = false
        createAccountActivateAndMigrate(kinClient, object : IMigrationManagerCallbacks {

            override fun onMigrationStart() {
                startMigration = true
            }

            override fun onReady(kinClient: IKinClient) {
                assertTrue(startMigration)
                migratedKinClient = kinClient
                latch.countDown()
            }

            override fun onError(e: Exception) {
                fail("not supposed to reach onError with this exception: $e")
            }

        })
        latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS)
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

    // Activate account on the blockchain and when activation is complete then the method will end.
    private fun activateAccount(account: IKinAccount?) {
        val latch = CountDownLatch(1)
        // run it on a background thread because this method is called from the main ui thread.
        Thread(Runnable {
            account?.activateSync()
            latch.countDown()
        }).start()
        assertTrue(latch.await(timeoutDurationSecondsShort, TimeUnit.SECONDS))
    }

    // Create account on the blockchain and when creation complete then the method will end.
    private fun createAccount(account: IKinAccount?) {
        val latch = CountDownLatch(1)
        // run it on a background thread because this method is called from the main ui thread.
        Thread(Runnable {
            fakeKinIssuer.createAccount(account?.publicAddress.orEmpty())
        }).start()

        var listenerRegistration: IListenerRegistration? = null
        listenerRegistration = account?.addAccountCreationListener {
            listenerRegistration?.remove()
            latch.countDown()
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS))
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
        latch.await(timeoutDurationSecondsLong, TimeUnit.SECONDS)
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
        latch.await(timeoutDurationSecondsVeryShort, TimeUnit.SECONDS)
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

}