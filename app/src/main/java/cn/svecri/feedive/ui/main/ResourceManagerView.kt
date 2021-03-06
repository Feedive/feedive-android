package cn.svecri.feedive.ui.main

import android.app.Application
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cn.svecri.feedive.data.AppDatabase
import cn.svecri.feedive.data.Feed
import cn.svecri.feedive.ui.theme.FeediveTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResourceManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getInstance(application)
    private val feedDao = appDatabase.feedDao()

    var isAddViewShow by mutableStateOf(false)

    var isShowNameChangeDialog = mutableStateOf(false)

    var curResourceName = mutableStateOf("")

    private var curResourceRowData = ResourceRowData()

    fun allResources() = run {
        feedDao.getAllFeedsFlow()
    }

    fun changeCurrentEditingResourceName(newName: String) {
        curResourceName.value = newName
    }

    fun switchChecked(resourceData: ResourceRowData) {
        resourceData.isChecked.value = !resourceData.isChecked.value
    }

    fun openChangeNameDialog(resourceData: ResourceRowData) {
        setIsShowNameChangeDialog(true)
        curResourceRowData = resourceData
        curResourceName = mutableStateOf(resourceData.resourceName.value)
    }

    fun updateResourceName() {
        val initName = curResourceRowData.resourceName.value
        curResourceRowData.resourceName = curResourceName
        viewModelScope.launch {
            val feed: Feed = feedDao.getFeedByName(initName)
            feed.feedName = curResourceName.value
            feedDao.updateFeed(feed = feed)
        }
    }

    fun switchEnable(resourceData: ResourceRowData) {
        resourceData.isEnabled.value = !resourceData.isEnabled.value
        viewModelScope.launch {
            val feed: Feed = feedDao.getFeedByName(resourceData.resourceName.value)
            feed.isEnable = resourceData.isEnabled.value
            feedDao.updateFeed(feed = feed)
        }
    }

    fun setIsShowNameChangeDialog(isShow: Boolean) {
        isShowNameChangeDialog.value = isShow
    }

    fun insertFeed(feed: Feed) {
        viewModelScope.launch(Dispatchers.IO) {
            feedDao.insertFeed(feed = feed)
        }
    }

    fun setIsAddViewShow(isShow: Boolean) {
        isAddViewShow = isShow
    }
}

@Composable
fun ResourceManagerView(navController: NavController, vm: ResourceManagerViewModel = viewModel()) {
    FeediveTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(text = "ResourceManage") },
                    navigationIcon = {
                        IconButton(
                            onClick = { vm.setIsAddViewShow(true) }) {
                            Icon(Icons.Outlined.Add, contentDescription = "AddResource")
                        }
                    })
            })
        {
            if (vm.isAddViewShow) {
                ResourceAddDialog(vm)
            } else {
                ResourceList()
                ResourceManagerBottomBar()
            }
        }
    }
}

