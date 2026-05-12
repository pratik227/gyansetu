package com.gyansetu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.ui.theme.GyanColors

/** The chunky 5-px-shadow button shared across screens. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    contentColor: Color = GyanColors.Ink,
    padding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
) {
    Box(
        modifier = modifier
            .shadow(0.dp)
            .background(background, RoundedCornerShape(20.dp))
            .border(BorderStroke(3.dp, GyanColors.Ink), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}
