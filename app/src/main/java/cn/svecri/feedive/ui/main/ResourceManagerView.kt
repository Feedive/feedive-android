package cn.svecri.feedive.ui.main

import android.app.Application
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
            ResourceAddDialog(vm)
        }
    }
}
