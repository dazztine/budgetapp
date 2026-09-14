package com.example.budgettracker.data.local

import androidx.room.TypeConverter
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromBillAmountType(value: com.example.budgettracker.data.local.entity.BillAmountType?): String? = value?.name

    @TypeConverter
    fun toBillAmountType(value: String?): com.example.budgettracker.data.local.entity.BillAmountType? =
        value?.let { com.example.budgettracker.data.local.entity.BillAmountType.valueOf(it) }
}
