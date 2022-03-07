package cn.svecri.feedive.ui.main


import android.app.Application
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.svecri.feedive.data.AppDatabase
import cn.svecri.feedive.data.Feed
import cn.svecri.feedive.ui.theme.FeediveTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


data class GroupRow(
    var isChecked: MutableState<Boolean> = mutableStateOf(false),
    var groupName: String = ""
)

class GroupsViewModel : ViewModel() {
    /*TODO: read from sql*/
    var groups = mutableStateListOf(
        GroupRow(mutableStateOf(false), "group1"),
        GroupRow(mutableStateOf(false), "group2")
    )

    fun switchCheckState(row: GroupRow) {
        row.isChecked.value = !row.isChecked.value
    }

    fun save2db() {
        /*TODO*/
    }
}

class ResourceManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getInstance(application)
    private val feedDao = appDatabase.feedDao()

    fun insertFeed(feed: Feed) {
        viewModelScope.launch(Dispatchers.IO) {
            feedDao.insertFeed(feed = feed)
        }
    }
}

@Composable
fun MultiSelectRow(groupRow: GroupRow, onSwitchCheckBox: (GroupRow) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = groupRow.isChecked.value,
            onCheckedChange = {
                onSwitchCheckBox(groupRow)
            }
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = groupRow.groupName)
    }
}

@Composable
fun GroupList(
    groupsViewModel: GroupsViewModel = viewModel()
) {
    val groups = groupsViewModel.groups
    val onSwitchChecked = groupsViewModel::switchCheckState
    LazyColumn {
        items(items = groups) { row ->
            MultiSelectRow(groupRow = row, onSwitchCheckBox = { onSwitchChecked(row) })
        }
    }
}

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

                val groupsViewModel = GroupsViewModel()
                GroupList(groupsViewModel)

                Button(
                    onClick = { /*TODO into edit group page*/ },
                    colors = buttonColors(Color.White),
                    elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.dp, Color.White),
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Edit groups")
                }
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
                        Text(text = "Cancel", color = MaterialTheme.colors.primary)
                    }
                    Button(
                        onClick = {
                            setShowDialog(false)
                            groupsViewModel.save2db()
                        },
                        colors = buttonColors(Color.White),
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


@Preview(showBackground = true)
@Composable
fun ResourceAddViewPreviewer() = ResourceAddView()


@Composable
fun ResourceAddView(vm: ResourceManagerViewModel = viewModel()) {
    val isShowing = remember {
        mutableStateOf(true)
    }

    FeediveTheme {
        Box {
            val (showSelectDialog, setShowSelectDialog) = remember { mutableStateOf(false) }
            Column(
                Modifier
                    .fillMaxWidth(1f)
                    .height(800.dp)
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
                var nameText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Name") }
                )
                var urlText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("URL") }
                )
                var feedType by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = feedType,
                    onValueChange = { feedType = it },
                    label = { Text("Type") }
                )
                var feedPriority by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = feedPriority,
                    onValueChange = { feedPriority = it },
                    label = { Text("Priority") }
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
                        onClick = { isShowing.value = false },
                        colors = buttonColors(Color.White),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.dp, Color.White)
                    )
                    {
                        Text(text = "Cancel", color = MaterialTheme.colors.primary)
                    }
                    Button(
                        onClick = {
                            isShowing.value = false
                            vm.insertFeed(
                                Feed(
                                    0,
                                    feedName = nameText,
                                    feedType = feedType,
                                    feedUrl = urlText,
                                    feedPriority = feedPriority.toInt()
                                )
                            )
                        },
                        colors = buttonColors(Color.White),
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