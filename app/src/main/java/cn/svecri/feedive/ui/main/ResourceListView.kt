package cn.svecri.feedive.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class ResourceRowData(
    var resourceName: MutableState<String> = mutableStateOf(""),
    var isChecked: MutableState<Boolean> = mutableStateOf(false),
    var isEnabled: MutableState<Boolean> = mutableStateOf(true),
)

class ResourceListViewModel : ViewModel() {
    var resourceList = mutableStateListOf(
        ResourceRowData(mutableStateOf("1")),
        ResourceRowData(mutableStateOf("2"))
    )

    fun switchChecked(resourceData: ResourceRowData) {
        resourceData.isChecked.value = !resourceData.isChecked.value
    }

    fun changeName(resourceData: ResourceRowData, newName: String) {
        resourceData.resourceName.value = newName
    }

    fun switchEnable(resourceData: ResourceRowData) {
        resourceData.isEnabled.value = !resourceData.isEnabled.value
    }
}

@Composable
fun ResourceRow(
    rowData: ResourceRowData,
    onCheckedChange: (ResourceRowData) -> Unit,
    onEnableChange: (ResourceRowData) -> Unit,
    onNameChanged: (ResourceRowData, String) -> Unit
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
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Filled.Edit, contentDescription = "EditName")
            }
            Switch(checked = rowData.isEnabled.value, onCheckedChange = { onEnableChange(rowData) })
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
        }

    }
}

@Preview
@Composable
fun ResourceList(vm: ResourceListViewModel = viewModel()) {
    val resourceList = vm.resourceList
    val onSwitchChecked = vm::switchChecked
    LazyColumn {
        items(items = resourceList) { item ->
            ResourceRow(
                rowData = item,
                onCheckedChange = onSwitchChecked,
                onEnableChange = vm::switchEnable,
                onNameChanged = vm::changeName
            )
            Divider()
        }
    }

}


@Composable
fun ResourceManagerBottomBar() {
}


