package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ru.skillbranch.skillarticles.data.*
import ru.skillbranch.skillarticles.repositories.MarkdownElement
import ru.skillbranch.skillarticles.repositories.MarkdownParser

object ArticleRepository : IArticleRepository {
    private val local = LocalDataHolder
    private val network = NetworkDataHolder
    private val isSearchLiveData = MutableLiveData<Boolean>()
    private val prefs:PrefManager = PrefManager()

    override fun loadArticleContent(articleId: String): LiveData<List<MarkdownElement>?> {
        return network.loadArticleContent(articleId)
            .map { str ->
                str?.let { MarkdownParser.parse(it) }
            }
    }
    override fun getArticle(articleId: String): LiveData<ArticleData?> {
        return local.findArticle(articleId)
    }

    override fun loadArticlePersonalInfo(articleId: String): LiveData<ArticlePersonalInfo?> {
        return local.findArticlePersonalInfo(articleId)
    }

    override fun getAppSettings(): LiveData<AppSettings> = prefs.settings //from preferences
    override fun updateSettings(appSettings: AppSettings) {
        prefs.isBigText = appSettings.isBigText
        prefs.isDarkMode = appSettings.isDarkMode
    }

    override fun updateArticlePersonalInfo(info: ArticlePersonalInfo) {
        local.updateArticlePersonalInfo(info)
    }

    fun getSearchStatus() = isSearchLiveData

    fun updateSearchStatus(status:Boolean){
        isSearchLiveData.value = status
    }
}

interface  IArticleRepository{
    fun loadArticleContent(articleId: String): LiveData<List<MarkdownElement>?>
    fun getArticle(articleId: String): LiveData<ArticleData?>
    fun loadArticlePersonalInfo(articleId: String): LiveData<ArticlePersonalInfo?>
    fun getAppSettings(): LiveData<AppSettings>
    fun updateSettings(appSettings: AppSettings)
    fun updateArticlePersonalInfo(info: ArticlePersonalInfo)
}