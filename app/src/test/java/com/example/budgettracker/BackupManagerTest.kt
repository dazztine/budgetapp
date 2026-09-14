package com.example.budgettracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.backup.BackupData
import com.example.budgettracker.data.backup.BackupManager
import com.example.budgettracker.data.backup.RestoreResult
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.BillAccountDetailsEntity
import com.example.budgettracker.data.local.entity.BillAmountType
import com.example.budgettracker.data.local.entity.InstallmentPlanEntity
import com.example.budgettracker.data.local.entity.LoanAccountDetailsEntity
import com.example.budgettracker.data.local.entity.SavingsAccountDetailsEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = BudgetRepository(
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            loanDetailsDao = database.loanDetailsDao(),
            installmentPlanDao = database.installmentPlanDao(),
            savingsDetailsDao = database.savingsDetailsDao(),
            billDetailsDao = database.billDetailsDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testExportAndImportJsonSerialization() {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "BDO Savings", type = AccountType.BANK, initialBalance = 500000L, displayOrder = 1),
            AccountEntity(id = 2L, name = "Maya Credit", type = AccountType.LOAN, initialBalance = 0L, displayOrder = 2)
        )

        val loanDetails = listOf(
            LoanAccountDetailsEntity(accountId = 2L, cycleDay1 = 15, cycleDay2 = 30, minimumAmountDue = 250000L)
        )

        val installmentPlans = listOf(
            InstallmentPlanEntity(
                id = 1L,
                accountId = 2L,
                title = "iPhone 15",
                category = "Electronics",
                totalPurchaseAmount = 6000000L,
                totalInstallments = 12,
                installmentsPaid = 2,
                remainingBalance = 5000000L,
                monthlyPaymentAmount = 500000L
            )
        )

        val transactions = listOf(
            TransactionEntity(
                id = 10L,
                accountId = 1L,
                toAccountId = null,
                type = TransactionType.EXPENSE,
                amount = 15050L,
                category = "Food & Dining",
                title = "Jollibee",
                timestamp = 1700000000000L
            )
        )

        val savingsDetails = listOf(
            SavingsAccountDetailsEntity(accountId = 1L, interestRate = 3.5, goalAmount = 10000000L)
        )
        val billDetails = listOf(
            BillAccountDetailsEntity(accountId = 2L, dueDay = 25, amountDue = 150000L, amountType = BillAmountType.FIXED)
        )

        val jsonString = BackupManager.exportToJson(accounts, loanDetails, installmentPlans, transactions, savingsDetails, billDetails)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("BDO Savings"))
        assertTrue(jsonString.contains("iPhone 15"))
        assertTrue(jsonString.contains("savingsDetails"))
        assertTrue(jsonString.contains("billDetails"))

        val importedBackup = BackupManager.importFromJson(jsonString)
        assertEquals(BackupData.CURRENT_SCHEMA_VERSION, importedBackup.version)
        assertEquals(2, importedBackup.accounts.size)
        assertEquals(true, importedBackup.accounts[0].includeInNetWorth)
        assertEquals(1, importedBackup.loanDetails.size)
        assertEquals(1, importedBackup.savingsDetails.size)
        assertEquals(1, importedBackup.billDetails.size)
        assertEquals(1, importedBackup.installmentPlans.size)
        assertEquals(1, importedBackup.transactions.size)
    }

    @Test
    fun testImportFromV3JsonDefaultsIncludeInNetWorth() {
        val v3Json = """
            {
              "version": 3,
              "exportTimestamp": 1700000000000,
              "accounts": [
                {
                  "id": 1,
                  "name": "BDO Bank",
                  "type": "BANK",
                  "initialBalance": 100000,
                  "isActive": true,
                  "displayOrder": 0,
                  "createdAt": 1700000000000,
                  "updatedAt": 1700000000000
                },
                {
                  "id": 2,
                  "name": "Meralco",
                  "type": "BILL",
                  "initialBalance": 0,
                  "isActive": true,
                  "displayOrder": 1,
                  "createdAt": 1700000000000,
                  "updatedAt": 1700000000000
                }
              ],
              "loanDetails": [],
              "savingsDetails": [],
              "billDetails": [],
              "installmentPlans": [],
              "transactions": []
            }
        """.trimIndent()

        val imported = BackupManager.importFromJson(v3Json)
        assertEquals(2, imported.accounts.size)
        assertEquals(true, imported.accounts[0].includeInNetWorth)
        assertEquals(false, imported.accounts[1].includeInNetWorth)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testImportInvalidVersionThrowsException() {
        val invalidJson = """
            {
              "version": 99,
              "accounts": [],
              "loanDetails": [],
              "installmentPlans": [],
              "transactions": []
            }
        """.trimIndent()

        BackupManager.importFromJson(invalidJson)
    }

    @Test
    fun testPerformRestoreRoundtripInDatabase() = runBlocking {
        val acc1Id = repository.insertAccount(AccountEntity(name = "GCash", type = AccountType.E_WALLET, initialBalance = 100000L))
        val acc2Id = repository.insertAccount(AccountEntity(name = "BPI Credit", type = AccountType.BNPL, initialBalance = 0L))

        repository.insertLoanDetails(LoanAccountDetailsEntity(accountId = acc2Id, cycleDay1 = 10))
        repository.insertSavingsDetails(SavingsAccountDetailsEntity(accountId = acc1Id, interestRate = 4.0, goalAmount = 5000000L))
        repository.insertBillDetails(BillAccountDetailsEntity(accountId = acc2Id, dueDay = 15, amountDue = 200000L, amountType = BillAmountType.FIXED))
        repository.insertTransaction(
            TransactionEntity(
                accountId = acc1Id,
                type = TransactionType.INCOME,
                amount = 2500000L,
                category = "Salary",
                title = "Monthly Paycheck",
                timestamp = System.currentTimeMillis()
            )
        )

        val accountsBefore = repository.getAllAccountsDirect()
        val loanDetailsBefore = repository.getAllLoanDetailsDirect()
        val savingsDetailsBefore = repository.getAllSavingsDetailsDirect()
        val billDetailsBefore = repository.getAllBillDetailsDirect()
        val installmentPlansBefore = repository.getAllInstallmentPlansDirect()
        val transactionsBefore = repository.getAllTransactionsDirect()

        val jsonBackup = BackupManager.exportToJson(
            accountsBefore,
            loanDetailsBefore,
            installmentPlansBefore,
            transactionsBefore,
            savingsDetailsBefore,
            billDetailsBefore
        )
        val backupData = BackupManager.importFromJson(jsonBackup)

        val restoreResult = BackupManager.performRestore(repository, backupData)
        assertTrue(restoreResult is RestoreResult.Success)

        val restoredAccounts = repository.getAllAccountsDirect()
        assertEquals(2, restoredAccounts.size)
        val restoredSavings = repository.getAllSavingsDetailsDirect()
        assertEquals(1, restoredSavings.size)
        assertEquals(4.0, restoredSavings[0].interestRate!!, 0.001)
        val restoredBills = repository.getAllBillDetailsDirect()
        assertEquals(1, restoredBills.size)
        assertEquals(15, restoredBills[0].dueDay)
    }
}
