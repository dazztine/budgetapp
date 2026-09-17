package com.example.budgettracker.ui.transaction.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.budgettracker.data.model.TransactionType

data class CategoryItem(
    val name: String,
    val iconName: String,
    val isCustom: Boolean = false
)

object CategoryIconMapper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "restaurant", "food" -> Icons.Outlined.Restaurant
            "fastfood" -> Icons.Outlined.Fastfood
            "shopping_cart", "groceries" -> Icons.Outlined.ShoppingCart
            "shopping_bag", "shopping" -> Icons.Outlined.ShoppingBag
            "directions_car", "transport", "transportation" -> Icons.Outlined.DirectionsCar
            "directions_bus" -> Icons.Outlined.DirectionsBus
            "local_gas_station" -> Icons.Outlined.LocalGasStation
            "receipt", "bills" -> Icons.Outlined.Receipt
            "receipt_long" -> Icons.Outlined.ReceiptLong
            "medical_services", "health" -> Icons.Outlined.MedicalServices
            "movie", "entertainment" -> Icons.Outlined.Movie
            "sports_esports" -> Icons.Outlined.SportsEsports
            "school", "education" -> Icons.Outlined.School
            "menu_book" -> Icons.Outlined.MenuBook
            "home", "rent" -> Icons.Outlined.Home
            "subscriptions" -> Icons.Outlined.Subscriptions
            "face", "personal_care" -> Icons.Outlined.Face
            "card_giftcard", "gifts" -> Icons.Outlined.CardGiftcard
            "credit_card", "fees" -> Icons.Outlined.CreditCard
            "work", "salary" -> Icons.Outlined.Work
            "computer", "freelance" -> Icons.Outlined.Computer
            "attach_money", "allowance" -> Icons.Outlined.AttachMoney
            "savings" -> Icons.Outlined.Savings
            "trending_up", "investments" -> Icons.Outlined.TrendingUp
            "undo", "refunds" -> Icons.Outlined.Undo
            "swap_horiz", "transfer" -> Icons.Outlined.SwapHoriz
            "account_balance_wallet" -> Icons.Outlined.AccountBalanceWallet
            "devices", "gadgets" -> Icons.Outlined.Devices
            "kitchen", "appliances" -> Icons.Outlined.Kitchen
            "weekend", "furniture" -> Icons.Outlined.Weekend
            "checkroom", "fashion" -> Icons.Outlined.Checkroom
            "flight", "travel" -> Icons.Outlined.Flight
            "fitness_center" -> Icons.Outlined.FitnessCenter
            "pets" -> Icons.Outlined.Pets
            "local_cafe", "coffee" -> Icons.Outlined.LocalCafe
            "build" -> Icons.Outlined.Build
            "star" -> Icons.Outlined.Star
            "bookmark" -> Icons.Outlined.Bookmark
            else -> Icons.Outlined.Category
        }
    }

    val AVAILABLE_CUSTOM_ICONS = listOf(
        "restaurant",
        "shopping_cart",
        "shopping_bag",
        "directions_car",
        "receipt",
        "medical_services",
        "movie",
        "school",
        "home",
        "subscriptions",
        "face",
        "card_giftcard",
        "credit_card",
        "work",
        "computer",
        "trending_up",
        "devices",
        "flight",
        "fitness_center",
        "pets",
        "local_cafe",
        "sports_esports",
        "build",
        "star"
    )

    fun getPresetCategories(type: TransactionType): List<CategoryItem> {
        return when (type) {
            TransactionType.EXPENSE -> listOf(
                CategoryItem("Food & Dining", "restaurant"),
                CategoryItem("Groceries", "shopping_cart"),
                CategoryItem("Transportation", "directions_car"),
                CategoryItem("Bills & Utilities", "receipt"),
                CategoryItem("Shopping", "shopping_bag"),
                CategoryItem("Health & Wellness", "medical_services"),
                CategoryItem("Entertainment", "movie"),
                CategoryItem("Education", "school"),
                CategoryItem("Rent", "home"),
                CategoryItem("Subscriptions", "subscriptions"),
                CategoryItem("Personal Care", "face"),
                CategoryItem("Gifts & Donations", "card_giftcard"),
                CategoryItem("Fees & Charges", "credit_card"),
                CategoryItem("Others", "category")
            )
            TransactionType.INCOME -> listOf(
                CategoryItem("Salary", "work"),
                CategoryItem("Freelance/Business", "computer"),
                CategoryItem("Allowance", "attach_money"),
                CategoryItem("Gifts Received", "card_giftcard"),
                CategoryItem("Refunds", "undo"),
                CategoryItem("Investments", "trending_up"),
                CategoryItem("Others", "category")
            )
            TransactionType.TRANSFER -> listOf(
                CategoryItem("General Transfer", "swap_horiz"),
                CategoryItem("Savings Transfer", "savings"),
                CategoryItem("Bill Payment", "receipt_long"),
                CategoryItem("Credit Card / Loan Payment", "credit_card"),
                CategoryItem("Cash Out / In", "account_balance_wallet"),
                CategoryItem("Others", "category")
            )
            TransactionType.INSTALLMENT -> listOf(
                CategoryItem("Gadgets & Tech", "devices"),
                CategoryItem("Appliances", "kitchen"),
                CategoryItem("Furniture", "weekend"),
                CategoryItem("Fashion & Apparel", "checkroom"),
                CategoryItem("Vehicle & Auto", "directions_car"),
                CategoryItem("Travel", "flight"),
                CategoryItem("Others", "category")
            )
        }
    }
}
