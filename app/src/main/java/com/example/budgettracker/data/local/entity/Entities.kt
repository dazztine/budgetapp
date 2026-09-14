package com.example.budgettracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isActive", "displayOrder"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val presetId: String? = null,
    val initialBalance: Long = 0L,
    val isActive: Boolean = true,
    val includeInNetWorth: Boolean = (type != AccountType.BILL),
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loan_details",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LoanAccountDetailsEntity(
    @PrimaryKey
    val accountId: Long,
    val cycleDay1: Int,
    val cycleDay2: Int? = null,
    val minimumAmountDue: Long = 0L,
    val totalRemainingBalance: Long = 0L,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 3
)

enum class BillAmountType {
    FIXED,
    ESTIMATED
}

@Entity(
    tableName = "savings_account_details",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SavingsAccountDetailsEntity(
    @PrimaryKey
    val accountId: Long,
    val interestRate: Double? = null,
    val goalAmount: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bill_account_details",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BillAccountDetailsEntity(
    @PrimaryKey
    val accountId: Long,
    val dueDay: Int,
    val amountDue: Long? = null,
    val amountType: BillAmountType = BillAmountType.FIXED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "installment_plans",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["accountId"])
    ]
)
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val title: String,
    val category: String,
    val totalPurchaseAmount: Long,
    val totalInstallments: Int,
    val installmentsPaid: Int = 0,
    val remainingBalance: Long,
    val monthlyPaymentAmount: Long,
    val purchaseDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = InstallmentPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentPlanId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["installmentPlanId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type", "category"]),
        Index(value = ["type", "title"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val isAdjustment: Boolean = false,
    val accountId: Long,
    val toAccountId: Long? = null,
    val installmentPlanId: Long? = null,
    val category: String,
    val title: String,
    val timestamp: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AccountWithBalance(
    val id: Long,
    val name: String,
    val type: AccountType,
    val presetId: String?,
    val initialBalance: Long,
    val isActive: Boolean,
    val includeInNetWorth: Boolean = true,
    val displayOrder: Int,
    val currentBalance: Long
)

data class AccountWithLoanDetails(
    @Embedded
    val account: AccountEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "accountId"
    )
    val loanDetails: LoanAccountDetailsEntity?
)

data class AccountWithBillDetails(
    @Embedded
    val account: AccountEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "accountId"
    )
    val billDetails: BillAccountDetailsEntity?
)
