package com.example.budgettracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgettracker.data.local.AppDatabase
import com.example.budgettracker.data.local.dao.AccountDao
import com.example.budgettracker.data.local.dao.TransactionDao
import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.local.entity.TransactionEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.data.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScopedAutocompleteTest {

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: BudgetRepository
    private var accountId: Long = 0L

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        accountDao = database.accountDao()
        transactionDao = database.transactionDao()
        repository = BudgetRepository(
            accountDao,
            transactionDao,
            database.loanDetailsDao(),
            database.installmentPlanDao(),
            database.savingsDetailsDao(),
            database.billDetailsDao()
        )

        accountId = accountDao.insert(
            AccountEntity(name = "Test Account", type = AccountType.BANK, initialBalance = 100_000L)
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testCategoryAutocompleteStrictlyIsolatedByTransactionType() = runBlocking {
        val now = System.currentTimeMillis()

        // Insert EXPENSE transaction
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1500L,
                accountId = accountId,
                category = "Food & Dining",
                title = "Jollibee Lunch",
                timestamp = now
            )
        )

        // Insert INCOME transaction
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 50000L,
                accountId = accountId,
                category = "Salary",
                title = "Biweekly Pay",
                timestamp = now + 1
            )
        )

        // Insert TRANSFER transaction
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 2000L,
                accountId = accountId,
                toAccountId = accountId,
                category = "Savings Transfer",
                title = "Emergency Fund",
                timestamp = now + 2
            )
        )

        // Insert INSTALLMENT transaction
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INSTALLMENT,
                amount = 500L,
                accountId = accountId,
                category = "Reconciliation",
                title = "Interest",
                timestamp = now + 3
            )
        )

        // Verify EXPENSE categories
        val expenseCategories = repository.getDistinctCategories(TransactionType.EXPENSE).first()
        assertEquals(listOf("Food & Dining"), expenseCategories)
        assertFalse(expenseCategories.contains("Salary"))
        assertFalse(expenseCategories.contains("Savings Transfer"))

        // Verify INCOME categories
        val incomeCategories = repository.getDistinctCategories(TransactionType.INCOME).first()
        assertEquals(listOf("Salary"), incomeCategories)
        assertFalse(incomeCategories.contains("Food & Dining"))

        // Verify TRANSFER categories
        val transferCategories = repository.getDistinctCategories(TransactionType.TRANSFER).first()
        assertEquals(listOf("Savings Transfer"), transferCategories)

        // Verify INSTALLMENT categories
        val installmentCategories = repository.getDistinctCategories(TransactionType.INSTALLMENT).first()
        assertEquals(listOf("Reconciliation"), installmentCategories)
    }

    @Test
    fun testTitleAutocompleteStrictlyIsolatedByTransactionTypeAndCategory() = runBlocking {
        val now = System.currentTimeMillis()

        // Insert EXPENSE entries under "Transportation"
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 5000L,
                accountId = accountId,
                category = "Transportation",
                title = "Grab Ride to Work",
                timestamp = now
            )
        )
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 2000L,
                accountId = accountId,
                category = "Transportation",
                title = "MRT Fare",
                timestamp = now + 1
            )
        )

        // Insert INCOME entry under "Freelance"
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 100000L,
                accountId = accountId,
                category = "Freelance",
                title = "Website Project",
                timestamp = now + 2
            )
        )

        // Verify titles for EXPENSE
        val expenseTitles = repository.getDistinctTitles(TransactionType.EXPENSE).first()
        assertEquals(2, expenseTitles.size)
        assertTrue(expenseTitles.contains("Grab Ride to Work"))
        assertTrue(expenseTitles.contains("MRT Fare"))
        assertFalse(expenseTitles.contains("Website Project"))

        // Verify titles for INCOME
        val incomeTitles = repository.getDistinctTitles(TransactionType.INCOME).first()
        assertEquals(listOf("Website Project"), incomeTitles)

        // Verify titles for EXPENSE scoped by category "Transportation"
        val transportTitles = repository.getDistinctTitlesByCategory(TransactionType.EXPENSE, "Transportation").first()
        assertEquals(2, transportTitles.size)

        // Verify titles for EXPENSE scoped by non-existent category
        val otherTitles = repository.getDistinctTitlesByCategory(TransactionType.EXPENSE, "Utilities").first()
        assertTrue(otherTitles.isEmpty())
    }

    @Test
    fun testEmptyCategoryAndTitleExcludedFromAutocomplete() = runBlocking {
        transactionDao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 500L,
                accountId = accountId,
                category = "",
                title = "",
                timestamp = System.currentTimeMillis()
            )
        )

        val categories = repository.getDistinctCategories(TransactionType.EXPENSE).first()
        assertTrue(categories.isEmpty())

        val titles = repository.getDistinctTitles(TransactionType.EXPENSE).first()
        assertTrue(titles.isEmpty())
    }
}
