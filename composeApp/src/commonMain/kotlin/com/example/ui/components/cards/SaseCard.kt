package com.example.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SaseColors
import com.example.ui.theme.SaseShapes

enum class SaseCardStyle {
    NORMAL,
    SELECTED,
    EMPHASIZED
}

@Composable
fun SaseCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SaseColors.Surface,
    borderColor: Color = SaseColors.Border,
    shape: RoundedCornerShape = SaseShapes.Large,
    elevation: androidx.compose.ui.unit.Dp = 2.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    style: SaseCardStyle = SaseCardStyle.NORMAL,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedContainer = when (style) {
        SaseCardStyle.SELECTED -> SaseColors.InstitutionalBlueLight
        SaseCardStyle.EMPHASIZED -> SaseColors.SurfaceVariant
        SaseCardStyle.NORMAL -> containerColor
    }
    val resolvedBorder = when (style) {
        SaseCardStyle.SELECTED -> SaseColors.BorderFocus
        SaseCardStyle.EMPHASIZED -> SaseColors.BorderStrong
        SaseCardStyle.NORMAL -> borderColor
    }

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = resolvedContainer),
        border = BorderStroke(1.dp, resolvedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
