package com.example.ui.components.buttons

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaseColors
import com.example.ui.theme.SaseDimensions

@Composable
fun SasePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    containerColor: Color = SaseColors.InstitutionalBlue,
    contentColor: Color = SaseColors.TextOnPrimary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = SaseDimensions.ButtonHeight),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SaseColors.BorderDisabled,
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
