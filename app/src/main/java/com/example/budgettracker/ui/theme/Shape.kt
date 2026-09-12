package com.example.budgettracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ZincCornerRadius = 10.dp
val ZincSoftCornerRadius = 16.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(ZincCornerRadius),
    small = RoundedCornerShape(ZincCornerRadius),
    medium = RoundedCornerShape(ZincSoftCornerRadius),
    large = RoundedCornerShape(ZincSoftCornerRadius),
    extraLarge = RoundedCornerShape(ZincSoftCornerRadius)
)
