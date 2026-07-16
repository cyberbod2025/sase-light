package com.example.ui.components.fields

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SaseColors

@Composable
fun SaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconContentDescription: String? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    trailingIconContentDescription: String? = null,
    singleLine: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = SaseColors.TextPrimary,
        unfocusedTextColor = SaseColors.TextPrimary,
        disabledTextColor = SaseColors.TextDisabled,
        errorTextColor = SaseColors.Error,
        focusedLabelColor = SaseColors.InstitutionalBlue,
        unfocusedLabelColor = SaseColors.TextSecondary,
        disabledLabelColor = SaseColors.TextDisabled,
        errorLabelColor = SaseColors.Error,
        focusedPlaceholderColor = SaseColors.Placeholder,
        unfocusedPlaceholderColor = SaseColors.Placeholder,
        disabledPlaceholderColor = SaseColors.TextDisabled,
        cursorColor = SaseColors.InstitutionalBlue,
        errorCursorColor = SaseColors.Error,
        focusedBorderColor = SaseColors.BorderFocus,
        unfocusedBorderColor = SaseColors.Border,
        disabledBorderColor = SaseColors.BorderDisabled,
        errorBorderColor = SaseColors.Error,
        focusedContainerColor = SaseColors.Surface,
        unfocusedContainerColor = SaseColors.Surface,
        disabledContainerColor = SaseColors.SurfaceVariant,
        errorContainerColor = SaseColors.ErrorBackground,
        focusedSupportingTextColor = SaseColors.TextSecondary,
        unfocusedSupportingTextColor = SaseColors.TextSecondary,
        disabledSupportingTextColor = SaseColors.TextDisabled,
        errorSupportingTextColor = SaseColors.Error
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        label = if (label != null) ({ Text(label) }) else null,
        placeholder = if (placeholder != null) ({ Text(placeholder) }) else null,
        supportingText = if (supportingText != null) ({ Text(supportingText) }) else null,
        leadingIcon = if (leadingIcon != null) ({
            Icon(leadingIcon, contentDescription = leadingIconContentDescription)
        }) else null,
        trailingIcon = if (trailingIcon != null && onTrailingIconClick != null) ({
            IconButton(onClick = onTrailingIconClick) {
                Icon(trailingIcon, contentDescription = trailingIconContentDescription)
            }
        }) else if (trailingIcon != null) ({
            Icon(trailingIcon, contentDescription = trailingIconContentDescription)
        }) else null,
        singleLine = singleLine,
        shape = shape,
        colors = colors
    )
}
