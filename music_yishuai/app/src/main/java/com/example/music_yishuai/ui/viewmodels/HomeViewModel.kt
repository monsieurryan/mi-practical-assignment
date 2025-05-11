package com.example.music_yishuai.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_yishuai.data.model.ModuleConfig
import com.example.music_yishuai.network.NetworkClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var homePageData by mutableStateOf<List<ModuleConfig>?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var error by mutableStateOf<String?>(null)
        private set

    // 添加一个全局变量来跟踪应用是否已经自动播放过
    private var _hasAutoPlayedInSession = false
    
    // 保存网络请求任务的引用，以便可以取消
    private var loadDataJob: Job? = null
    
    // 添加一个标志，表示应用是否正在退出
    private var isExiting = false

    init {
        loadHomePageData()
    }

    fun loadHomePageData() {
        // 如果应用正在退出，则不加载数据
        if (isExiting) {
            println("HomeViewModel: 应用正在退出，不加载数据")
            return
        }
        
        // 取消之前的任务（如果有）
        loadDataJob?.cancel()
        
        loadDataJob = viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = NetworkClient.apiService.getHomePage()
                if (response.code == 200) {
                    homePageData = response.data.records
                } else {
                    error = response.msg
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    println("HomeViewModel: 网络请求被取消")
                    return@launch
                }
                error = e.message
                println("HomeViewModel: 加载首页数据失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // 获取或重置自动播放状态
    fun getAndUpdateAutoPlayState(): Boolean {
        val currentState = _hasAutoPlayedInSession
        _hasAutoPlayedInSession = true
        return currentState
    }

    // 重置自动播放状态，在应用退出时调用
    fun resetAutoPlayState() {
        _hasAutoPlayedInSession = false
        
        // 标记应用正在退出，阻止新的数据加载
        isExiting = true
        
        // 取消当前的数据加载任务
        loadDataJob?.cancel()
        println("HomeViewModel: 重置自动播放状态，标记应用正在退出")
    }
    
    override fun onCleared() {
        super.onCleared()
        println("HomeViewModel: onCleared 被调用，取消所有任务")
        
        // 标记应用正在退出
        isExiting = true
        
        // 取消所有协程
        viewModelScope.coroutineContext.cancelChildren()
        
        // 显式取消加载数据的任务
        loadDataJob?.cancel()
    }
} 