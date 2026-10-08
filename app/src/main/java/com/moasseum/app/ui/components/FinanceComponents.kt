package com.moasseum.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.formatSignedWon
import com.moasseum.app.domain.formatWon
import com.moasseum.app.domain.formatDate
import com.moasseum.app.ui.theme.CategoryFood
import com.moasseum.app.ui.theme.CategoryCafe
import com.moasseum.app.ui.theme.CategoryHealth
import com.moasseum.app.ui.theme.CategoryLeisure
import com.moasseum.app.ui.theme.CategoryLiving
import com.moasseum.app.ui.theme.CategoryOther
import com.moasseum.app.ui.theme.CategoryShopping
import com.moasseum.app.ui.theme.CategoryTransport
import com.moasseum.app.ui.theme.CategoryHousing
import com.moasseum.app.ui.theme.CategoryCommunication
import com.moasseum.app.ui.theme.CategoryEducation
import com.moasseum.app.ui.theme.CategoryCulture
import com.moasseum.app.ui.theme.CategoryTravel
import com.moasseum.app.ui.theme.CategoryGift
import com.moasseum.app.ui.theme.CategoryBeauty
import com.moasseum.app.ui.theme.CategoryPet
import com.moasseum.app.ui.theme.CategorySubscription
import com.moasseum.app.ui.theme.CategoryInsurance
import com.moasseum.app.ui.theme.CategoryFinance
import com.moasseum.app.ui.theme.LocalFinanceColors
import com.moasseum.app.ui.theme.LocalFinanceMotion

data class CategorySpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

val CategorySpecs = listOf(
    CategorySpec("FOOD", "식비", Icons.Rounded.Restaurant, CategoryFood),
    CategorySpec("CAFE", "카페", Icons.Rounded.LocalCafe, CategoryCafe),
    CategorySpec("TRANSPORT", "교통", Icons.Rounded.DirectionsBus, CategoryTransport),
    CategorySpec("SHOPPING", "쇼핑", Icons.Rounded.ShoppingBag, CategoryShopping),
    CategorySpec("HOUSING", "주거", Icons.Rounded.Home, CategoryHousing),
    CategorySpec("COMMUNICATION", "통신", Icons.Rounded.PhoneAndroid, CategoryCommunication),
    CategorySpec("HEALTH", "의료", Icons.Rounded.LocalHospital, CategoryHealth),
    CategorySpec("EDUCATION", "교육", Icons.Rounded.School, CategoryEducation),
    CategorySpec("CULTURE", "문화", Icons.Rounded.TheaterComedy, CategoryCulture),
    CategorySpec("LEISURE", "여가", Icons.Rounded.Movie, CategoryLeisure),
    CategorySpec("TRAVEL", "여행", Icons.Rounded.Flight, CategoryTravel),
    CategorySpec("GIFT", "경조사", Icons.Rounded.CardGiftcard, CategoryGift),
    CategorySpec("BEAUTY", "미용", Icons.Rounded.Face, CategoryBeauty),
    CategorySpec("PET", "반려동물", Icons.Rounded.Pets, CategoryPet),
    CategorySpec("SUBSCRIPTION", "구독", Icons.Rounded.Subscriptions, CategorySubscription),
    CategorySpec("INSURANCE", "보험", Icons.Rounded.Security, CategoryInsurance),
    CategorySpec("FINANCE", "금융", Icons.Rounded.AccountBalance, CategoryFinance),
    CategorySpec("LIVING", "생활", Icons.Rounded.Home, CategoryLiving),
    CategorySpec("OTHER", "기타", Icons.Rounded.MoreHoriz, CategoryOther),
)

@Composable
fun categoryLabel(key: String): String =
    LocalCategoryLabels.current[key]
        ?.takeIf(String::isNotBlank)
        ?: CategorySpecs.firstOrNull { it.key == key }?.label
        ?: "기타"

@Composable
fun allCategorySpecs(): List<CategorySpec> {
    val labels = LocalCategoryLabels.current
    val order = LocalCategoryOrder.current
    val builtInKeys = CategorySpecs.mapTo(mutableSetOf(), CategorySpec::key)
    val custom = labels.entries
        .filter { (key, _) -> key !in builtInKeys }
        .map { (key, label) -> CategorySpec(key, label, Icons.Rounded.Category, CategoryOther) }
    return (CategorySpecs + custom).sortedBy { spec -> order.indexOf(spec.key).takeIf { it >= 0 } ?: Int.MAX_VALUE }
}

val LocalCategoryLabels = compositionLocalOf<Map<String, String>> { emptyMap() }
val LocalCategoryOrder = compositionLocalOf { CategorySpecs.map(CategorySpec::key) }

fun categoryColor(key: String): Color =
    CategorySpecs.firstOrNull { it.key == key }?.color ?: CategoryOther

fun categoryIcon(key: String): ImageVector =
    CategorySpecs.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Category

@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) colors.accentSoft else colors.surfaceRaised,
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.7.dp, colors.divider.copy(alpha = if (highlighted) 0.65f else 0.45f),
        ),
    ) {
        content()
    }
}

@Composable
fun CategoryIcon(
    categoryKey: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified,
) {
    val color = categoryColor(categoryKey)
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.18f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(categoryKey),
            contentDescription = categoryLabel(categoryKey),
            tint = if (iconTint == Color.Unspecified) color else iconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun TypeBadge(type: TransactionType) {
    val colors = LocalFinanceColors.current
    val tint = when (type) { TransactionType.EXPENSE -> colors.expense; TransactionType.INCOME -> colors.income; TransactionType.TRANSFER -> colors.textSecondary }
    val icon = if (type == TransactionType.EXPENSE) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
    Row(
        modifier = Modifier
            .background(tint.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(
            text = when (type) { TransactionType.EXPENSE -> "지출"; TransactionType.INCOME -> "수입"; TransactionType.TRANSFER -> "이체" },
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun AmountText(
    amount: Long,
    type: TransactionType,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
) {
    val colors = LocalFinanceColors.current
    Text(
        text = formatSignedWon(amount, type),
        color = when (type) { TransactionType.EXPENSE -> colors.expense; TransactionType.INCOME -> colors.income; TransactionType.TRANSFER -> colors.textSecondary },
        style = style,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Fit the entire amount on one line without cutting off financially significant digits. */
@Composable
fun FittedAmountText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color = LocalFinanceColors.current.textPrimary,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val available = with(density) { maxWidth.toPx() }
        val natural = measurer.measure(text, style = style, softWrap = false).size.width.coerceAtLeast(1)
        val scale = (available / natural).coerceIn(0.1f, 1f)
        Text(text, style = style.copy(fontSize = (style.fontSize.value * scale).sp), color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        CategoryIcon(transaction.categoryKey, modifier = Modifier.size(38.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(transaction.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = listOfNotNull(categoryLabel(transaction.categoryKey), transaction.paymentMethod.takeIf { it.isNotBlank() }).joinToString(" · "),
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(modifier = Modifier.widthIn(max = 148.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AmountText(transaction.amount, transaction.type)
            Text(formatDate(transaction.occurredDate), color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    addExpanded: Boolean = false,
    onAdd: () -> Unit,
) {
    val colors = LocalFinanceColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            color = colors.surfaceRaised,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem(
                    label = "대시보드",
                    icon = Icons.Rounded.Home,
                    selected = currentRoute == ROUTE_HOME,
                    onClick = { onNavigate(ROUTE_HOME) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    label = "소비내역",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    selected = currentRoute == ROUTE_HISTORY,
                    onClick = { onNavigate(ROUTE_HISTORY) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(58.dp))
                BottomNavItem(
                    label = "함께",
                    icon = Icons.Rounded.Group,
                    selected = currentRoute == ROUTE_TOGETHER,
                    onClick = { onNavigate(ROUTE_TOGETHER) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    label = "관리",
                    icon = Icons.Rounded.Payments,
                    selected = currentRoute == ROUTE_MANAGE,
                    onClick = { onNavigate(ROUTE_MANAGE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(Modifier.align(Alignment.TopCenter)) {
            AddFloatingActionButton(expanded = addExpanded, onClick = onAdd)
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val colors = LocalFinanceColors.current
    val motion = LocalFinanceMotion.current
    val tint by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.textSecondary,
        animationSpec = tween(motion.fast),
        label = "bottomNavTint",
    )
    val highlight by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(motion.fast),
        label = "bottomNavHighlight",
    )
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = highlight,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            BadgedBox(badge = {
                if (badgeCount > 0) Badge { Text(if (badgeCount > 99) "99+" else "$badgeCount") }
            }) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(1.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AddFloatingActionButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val motion = LocalFinanceMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(motion.standard),
        label = "addFabRotation",
    )
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        containerColor = LocalFinanceColors.current.accent,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = if (expanded) "추가 메뉴 닫기" else "거래 추가",
            modifier = Modifier
                .size(30.dp)
                .rotate(rotation),
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = colors.accent)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (message.isNotBlank()) Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            androidx.compose.material3.TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun TrendLabel(
    amount: Long,
    positiveIsGood: Boolean = true,
) {
    val colors = LocalFinanceColors.current
    val isPositive = amount >= 0
    val tint = when {
        isPositive && positiveIsGood -> colors.success
        !isPositive && positiveIsGood -> colors.expense
        isPositive -> colors.expense
        else -> colors.success
    }
    val icon = if (isPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            text = if (isPositive) "지난달보다 ${formatWon(amount)}" else "지난달보다 ${formatWon(-amount)}",
            color = tint,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

const val ROUTE_HOME = "home"
const val ROUTE_HISTORY = "history"
// Keep the old destination ID so updating from the former Together tab restores safely.
const val ROUTE_TOGETHER = "couple"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_LEGACY_TOGETHER = "together"
const val ROUTE_MANAGE = "manage"
