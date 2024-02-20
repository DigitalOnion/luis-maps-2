package com.udacity.project4.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udacity.project4.R
import com.udacity.project4.theme.LuisMaps2Theme
import kotlinx.coroutines.delay

@Composable
fun maps2SplashScreen() {
    LuisMaps2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val showWaitMessage = remember { mutableStateOf(false) }

            LaunchedEffect(showWaitMessage) {
                delay(500)
                showWaitMessage.value = true
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .paint(
                    painterResource(id = R.drawable.map_picture_2),
                    contentScale = ContentScale.Crop
                ),
                contentAlignment = Alignment.Center
            ) {
                if (showWaitMessage.value) {
                    Card (
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.6F))
                    ) {
                        Text(text = stringResource(R.string.wait_to_load),
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(16.dp, 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun maps2SplashPreview() {
    maps2SplashScreen()
}
