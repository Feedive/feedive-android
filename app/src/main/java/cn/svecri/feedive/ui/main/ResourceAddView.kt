package cn.svecri.feedive.ui.main


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import cn.svecri.feedive.ui.theme.FeediveTheme
import cn.svecri.feedive.ui.theme.Purple500


@Composable
fun GroupSelectDialog(showDialog: Boolean, setShowDialog: (Boolean) -> Unit) {
    if (showDialog) {
        Dialog(
            onDismissRequest = {
                setShowDialog(false)
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                securePolicy = SecureFlagPolicy.SecureOff
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 15.dp)
                    .background(
                        Color.White, shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select group",
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 10.dp),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Divider(modifier = Modifier.height(0.5.dp))
                Row(
                    Modifier.fillMaxWidth(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = { setShowDialog(false) },
                        colors = buttonColors(Color.White),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.dp, Color.White)
                    )
                    {
                        Text(text = "Cancel", color = Purple500)
                    }
                    Button(
                        onClick = { setShowDialog(false) /*TODO*/ },
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


@Preview(showBackground = true)
@Composable
fun ResourceAddView() {
    val state = remember {
        mutableStateOf(true)
    }
    FeediveTheme {
        Box() {
            val (showSelectDialog, setShowSelectDialog) = remember { mutableStateOf(false) }
            Column(
                Modifier
                    .fillMaxWidth(1f)
                    .height(Dp(240f))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Text(
                    text = "Add resource",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                var text by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("URL") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Group: ")
                    Spacer(Modifier.size(16.dp))
                    Button(onClick = { setShowSelectDialog(true) }) {
                        Text(text = "choose...")
                    }
                    GroupSelectDialog(
                        showDialog = showSelectDialog,
                        setShowDialog = setShowSelectDialog
                    )
                }
                Row(
                    Modifier.fillMaxWidth(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
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