package com.example.ui.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaseColors

enum class SaseStatusVariant {
    SUCCESS,
    WARNING,
    ERROR,
    INFORMATION,
    NEUTRAL
}

data class SaseStatusColors(
    val background: Color,
    val text: Color,
    val border: Color? = null
)

fun SaseStatusVariant.colors(): SaseStatusColors = when (this) {
    SaseStatusVariant.SUCCESS -> SaseStatusColors(
        background = SaseColors.SuccessBackground,
        text = SaseColors.Success
    )
    SaseStatusVariant.WARNING -> SaseStatusColors(
        background = SaseColors.WarningBackground,
        text = SaseColors.Warning
    )
    SaseStatusVariant.ERROR -> SaseStatusColors(
        background = SaseColors.ErrorBackground,
        text = SaseColors.Error
    )
    SaseStatusVariant.INFORMATION -> SaseStatusColors(
        background = SaseColors.InformationBackground,
        text = SaseColors.Information
    )
    SaseStatusVariant.NEUTRAL -> SaseStatusColors(
        background = SaseColors.NeutralBackground,
        text = SaseColors.Neutral
    )
}

@Composable
fun SaseStatusChip(
    label: String,
    variant: SaseStatusVariant,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val colors = variant.colors()
    val alpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .heightIn(min = 24.dp)
            .clip(RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(enabled = enabled) { onClick() } else Modifier)
            .background(colors.background.copy(alpha = alpha))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.text.copy(alpha = alpha),
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = colors.text.copy(alpha = alpha),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
