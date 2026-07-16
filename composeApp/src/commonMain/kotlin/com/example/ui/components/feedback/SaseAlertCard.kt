package com.example.ui.components.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaseColors

enum class SaseAlertVariant {
    SUCCESS,
    WARNING,
    ERROR,
    INFORMATION
}

data class SaseAlertColors(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val textColor: Color
)

fun SaseAlertVariant.colors(): SaseAlertColors = when (this) {
    SaseAlertVariant.SUCCESS -> SaseAlertColors(
        background = SaseColors.SuccessBackground,
        border = SaseColors.Success.copy(alpha = 0.35f),
        titleColor = SaseColors.Success,
        textColor = SaseColors.TextPrimary
    )
    SaseAlertVariant.WARNING -> SaseAlertColors(
        background = SaseColors.WarningBackground,
        border = SaseColors.Warning.copy(alpha = 0.35f),
        titleColor = SaseColors.Warning,
        textColor = SaseColors.TextPrimary
    )
    SaseAlertVariant.ERROR -> SaseAlertColors(
        background = SaseColors.ErrorBackground,
        border = SaseColors.Error.copy(alpha = 0.35f),
        titleColor = SaseColors.Error,
        textColor = SaseColors.TextPrimary
    )
    SaseAlertVariant.INFORMATION -> SaseAlertColors(
        background = SaseColors.InformationBackground,
        border = SaseColors.Information.copy(alpha = 0.35f),
        titleColor = SaseColors.Information,
        textColor = SaseColors.TextPrimary
    )
}

@Composable
fun SaseAlertCard(
    title: String,
    description: String,
    variant: SaseAlertVariant,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actions: @Composable (() -> Unit)? = null
) {
    val colors = variant.colors()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.titleColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    color = colors.titleColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
            Text(
                text = description,
                color = colors.textColor,
                fontSize = 11.sp
            )
            if (actions != null) {
                Spacer(modifier = Modifier.height(4.dp))
                actions()
            }
        }
    }
}
