package cn.svecri.feedive.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.viewmodel.compose.viewModel

data class ResourceRowData(
    var resourceName: MutableState<String> = mutableStateOf(""),
    var isChecked: MutableState<Boolean> = mutableStateOf(false),
    var isEnabled: MutableState<Boolean> = mutableStateOf(true),
) {
    constructor(resourceName: String, isEnabled: Boolean) : this() {
        this.resourceName = mutableStateOf(resourceName)
        this.isEnabled = mutableStateOf(isEnabled)
        this.isChecked = mutableStateOf(false)
    }
}


@Composable
fun ResourceRow(
    rowData: ResourceRowData,
    onCheckedChange: (ResourceRowData) -> Unit,
    onEnableChange: (ResourceRowData) -> Unit,
    onNameChanged: (ResourceRowData) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(1f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = rowData.isChecked.value,
                onCheckedChange = { onCheckedChange(rowData) })
            Text(text = rowData.resourceName.value)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.width(150.dp)

        ) {
            IconButton(onClick = {
                onNameChanged(rowData)
            }) {
                Icon(Icons.Filled.Edit, contentDescription = "EditName")
            }
            Switch(checked = rowData.isEnabled.value, onCheckedChange = { onEnableChange(rowData) })
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
        }

    }
}

@Composable
fun NameChangeDialog(
    showDialog: Boolean,
    setShowDialog: (Boolean) -> Unit,
    nameText: String,
    setNameText: (String) -> Unit,
    updateResourceName: () -> Unit
) {
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
                    text = "Change name",
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 10.dp),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = setNameText,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }

                Row(
                    Modifier.fillMaxWidth(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = { setShowDialog(false) },
                        colors = ButtonDefaults.buttonColors(Color.White),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.dp, Color.White)
                    )
                    {
                        Text(text = "Cancel", color = MaterialTheme.colors.primary)
                    }
                    Button(
                        onClick = {
                            updateResourceName()
                            setShowDialog(false)
                        },
                        colors = ButtonDefaults.buttonColors(Color.White),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.dp, Color.White)
                    ) {
                        Text(text = "OK", color = MaterialTheme.colors.primary)
                    }
                }
            }
        }
    }

}

@Preview
@Composable
fun ResourceList(vm: ResourceManagerViewModel = viewModel()) {

    val resourceList by remember { vm.allResources() }.collectAsState(initial = listOf())

    val onSwitchChecked = vm::switchChecked
    NameChangeDialog(
        showDialog = vm.isShowNameChangeDialog.value,
        setShowDialog = vm::setIsShowNameChangeDialog,
        nameText = vm.curResourceName.value,
        setNameText = vm::changeCurrentEditingResourceName,
        updateResourceName = vm::updateResourceName
    )

    LazyColumn {
        items(items = resourceList) { feed ->
            ResourceRow(
                rowData = ResourceRowData(
                    resourceName = feed.feedName,
                    isEnabled = feed.isEnable
                ),
                onCheckedChange = onSwitchChecked,
                onEnableChange = vm::switchEnable,
                onNameChanged = vm::openChangeNameDialog
            )
            Divider()
        }
    }

}


@Composable
fun ResourceManagerBottomBar() {
}


