package cn.svecri.feedive.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DialogBottomButtons(setCancelOnClick: () -> Unit, setOkOnclick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Button(
            onClick = {
                setCancelOnClick()
            },
            colors = ButtonDefaults.buttonColors(Color.White),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
            border = BorderStroke(0.dp, Color.White)
        )
        {
            Text(text = "Cancel", color = MaterialTheme.colors.primary)
        }
        Button(
            onClick = {
                setOkOnclick()

            },
            colors = ButtonDefaults.buttonColors(Color.White),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
            border = BorderStroke(0.dp, Color.White)
        ) {
            Text(text = "OK", color = MaterialTheme.colors.primary)
        }
    }
}