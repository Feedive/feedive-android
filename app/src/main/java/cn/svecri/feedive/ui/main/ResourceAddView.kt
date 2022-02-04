package cn.svecri.feedive.ui.main

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import cn.svecri.feedive.ui.theme.FeediveTheme
import cn.svecri.feedive.ui.theme.Purple200
import cn.svecri.feedive.ui.theme.Purple500

@Preview(showBackground = true)
@Composable
fun ResourceAddView() {
    val state = remember {
        mutableStateOf(true)
    }
    FeediveTheme {
        Dialog(
            onDismissRequest = { state.value = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                securePolicy = SecureFlagPolicy.SecureOff
            )
        ) {
            Column(
                Modifier
                    .fillMaxWidth(1f)
                    .height(Dp(180f))
                    .background(Color.White)
                    .padding(horizontal = 15.dp),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Text(
                    text = "Add resource",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                var text by remember { mutableStateOf("") }
                Row(
                    Modifier.fillMaxWidth(0.9f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("URL") }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(0.9f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { state.value = false },
                        colors = buttonColors(Color.White),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.dp, Color.White)
                    )
                    {
                        Text(text = "Cancel", color = Purple500)
                    }
                    Button(
                        onClick = { state.value = false /*TODO*/ },
                        colors = buttonColors(Color.White),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.dp, Color.White)
                    ) {
                        Text(text = "OK", color = Purple500)
                    }
                }
            }
        }
    }
}
