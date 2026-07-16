package com.example.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaseColors

@Composable
fun SaseSectionTabs(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = SaseColors.InstitutionalNavy,
    inactiveColor: Color = Color.White.copy(alpha = 0.66f),
    activeTextColor: Color = Color.White,
    inactiveTextColor: Color = SaseColors.TextPrimary,
    borderColor: Color = SaseColors.Border,
    shape: RoundedCornerShape = RoundedCornerShape(999.dp)
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { title ->
            val selected = selectedTab == title
            val bgColor by animateColorAsState(
                targetValue = if (selected) activeColor else inactiveColor,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 300f),
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) activeTextColor else inactiveTextColor,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 300f),
                label = "tabText"
            )
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bgColor)
                    .border(1.dp, if (selected) activeColor else borderColor, shape)
                    .clickable { onTabSelected(title) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .semantics {
                        contentDescription = "Tab $title"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
