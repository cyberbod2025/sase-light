package com.example.ui.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaseColors
import com.example.ui.theme.SaseDimensions

@Composable
fun SaseSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = SaseDimensions.ButtonHeight),
        shape = shape,
        border = BorderStroke(1.dp, SaseColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SaseColors.TextPrimary,
            disabledContentColor = SaseColors.TextDisabled
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
