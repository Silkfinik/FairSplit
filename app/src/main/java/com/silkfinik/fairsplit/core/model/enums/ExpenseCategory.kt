package com.silkfinik.fairsplit.core.model.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(
    val id: String,
    val displayName: String,
    val icon: ImageVector
) {
    GROCERIES("groceries", "Продукты", Icons.Default.LocalGroceryStore),
    EATING_OUT("eating_out", "Кафе и рестораны", Icons.Default.LocalDining),
    TRANSPORT("transport", "Транспорт", Icons.Default.DirectionsCar),
    HOUSING("housing", "Жилье", Icons.Default.Apartment),
    TRAVEL("travel", "Путешествия", Icons.Default.Flight),
    ENTERTAINMENT("entertainment", "Развлечения", Icons.Default.Movie),
    HEALTH("health", "Здоровье", Icons.Default.HealthAndSafety),
    SHOPPING("shopping", "Шопинг", Icons.Default.ShoppingBag),
    GIFTS("gifts", "Подарки", Icons.Default.CardGiftcard),
    OTHER("other", "Другое", Icons.Default.Category);

    companion object {
        fun fromId(id: String?): ExpenseCategory = entries.find { it.id == id } ?: OTHER
    }
}
