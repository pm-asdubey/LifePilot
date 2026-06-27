package com.lifepilot.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DriveEta
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

fun domainIcon(domain: String): ImageVector = when (domain.lowercase()) {
    "identity" -> Icons.Outlined.Badge
    "career" -> Icons.Outlined.Work
    "property", "home" -> Icons.Outlined.Home
    "vehicle", "transport" -> Icons.Outlined.DriveEta
    "finance" -> Icons.Outlined.AccountBalance
    "education" -> Icons.Outlined.School
    "health" -> Icons.Outlined.Favorite
    "legal" -> Icons.Outlined.Gavel
    "travel" -> Icons.Outlined.Flight
    "insurance" -> Icons.Outlined.Shield
    else -> Icons.Outlined.FolderOpen
}

fun objectTypeIcon(icon: String): ImageVector = when (icon.lowercase()) {
    "badge" -> Icons.Outlined.Badge
    "work" -> Icons.Outlined.Work
    "home" -> Icons.Outlined.Home
    "directions_car", "driveeta" -> Icons.Outlined.DriveEta
    "shield" -> Icons.Outlined.Shield
    "account_balance" -> Icons.Outlined.AccountBalance
    "bolt" -> Icons.Outlined.Bolt
    "flight" -> Icons.Outlined.Flight
    "gavel" -> Icons.Outlined.Gavel
    "receipt" -> Icons.Outlined.Receipt
    "savings" -> Icons.Outlined.Savings
    "subscriptions" -> Icons.Outlined.Subscriptions
    "trendingup", "trending_up" -> Icons.Outlined.TrendingUp
    "favorite" -> Icons.Outlined.Favorite
    "payments" -> Icons.Outlined.Payments
    "school" -> Icons.Outlined.School
    "local_hospital", "localhospital" -> Icons.Outlined.LocalHospital
    else -> Icons.Outlined.FolderOpen
}
