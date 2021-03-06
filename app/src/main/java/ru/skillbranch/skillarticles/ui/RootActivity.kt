package ru.skillbranch.skillarticles.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.getSpans
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.databinding.ActivityRootBinding
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.setMarginOptionally
import ru.skillbranch.skillarticles.markdown.MarkdownBuilder
import ru.skillbranch.skillarticles.ui.custom.SearchFocusSpan
import ru.skillbranch.skillarticles.ui.custom.SearchSpan
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
import ru.skillbranch.skillarticles.ui.delegates.viewBinding
import ru.skillbranch.skillarticles.viewmodels.*

class RootActivity : AppCompatActivity(), IArticleView {
    private val vb: ActivityRootBinding by viewBinding(ActivityRootBinding::inflate)
    private val viewModel: ArticleViewModel by viewModels { viewModelFactory }
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var viewModelFactory: ViewModelProvider.Factory = ViewModelFactory(this, "0")
    private val vbBottomBar
        get() = vb.bottombar.binding
    private val vbSubmenu
        get() = vb.submenu.binding
    private lateinit var searchView: SearchView

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val bgColor by AttrValue(R.attr.colorSecondary)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val fgColor by AttrValue(R.attr.colorOnSecondary)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        setupToolbar()
        setupBottomBar()
        setupSubmenu()
        viewModel.observeState(this, ::renderUi)
        viewModel.observeSubState(this, ArticleState::toBottombarData, ::renderBotombar)
        viewModel.observeSubState(this, ArticleState::toSubmenuData, ::renderSubmenu)
        viewModel.observeNotifications(this) {
            renderNotification(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        viewModel.saveState()
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(vb.coordinatorContainer, notify.message, Snackbar.LENGTH_LONG)
            .setAnchorView(vb.bottombar)
        when (notify) {
            is Notify.TextMessage -> {
            }
            is Notify.ActionMessage -> {
                snackbar.setActionTextColor(getColor(R.color.color_accent_dark))
                snackbar.setAction(notify.actionLabel) {
                    notify.actionHandler.invoke()
                }
            }
            is Notify.ErrorMessage -> {
                with(snackbar) {
                    setBackgroundTint(getColor(R.color.design_default_color_error))
                    setTextColor(getColor(android.R.color.white))
                    setActionTextColor(getColor(android.R.color.white))
                    setAction(notify.errLabel) {
                        notify.errHandler?.invoke()
                    }
                }
            }
        }
        snackbar.show()
    }

    override fun setupSubmenu() {
        with(vbSubmenu) {
            btnTextUp.setOnClickListener { viewModel.handleUpText() }
            btnTextDown.setOnClickListener { viewModel.handleDownText() }
            switchMode.setOnClickListener { viewModel.handleNightMode() }
        }
    }

    override fun setupBottomBar() {
        with(vbBottomBar) {
            btnLike.setOnClickListener { viewModel.handleLike() }
            btnBookmark.setOnClickListener { viewModel.handleBookmark() }
            btnSettings.setOnClickListener { viewModel.handleToggleMenu() }
            btnShare.setOnClickListener { viewModel.handleShare() }
            btnResultUp.setOnClickListener {
                searchView.clearFocus()
                vb.tvTextContent.requestFocus()
                viewModel.handleUpResult()
            }
            btnResultDown.setOnClickListener {
                searchView.clearFocus()
                vb.tvTextContent.requestFocus()
                viewModel.handleDownResult()
            }
            btnSearchClose.setOnClickListener {
                viewModel.handleSearchMode(false)
                invalidateOptionsMenu()
            }
        }
    }

    override fun renderBotombar(data: BottombarData) {
        with(vbBottomBar){
            btnLike.isChecked = data.isLike
            btnBookmark.isChecked = data.isBookmark
            btnSettings.isChecked = data.isShowMenu
        }
        if (data.isSearch) showSearchBar(data.resultsCount, data.searchPosition)
        else hideSearchBar()
    }

    override fun renderSubmenu(data: SubmenuData) {
        with(vbSubmenu){
            switchMode.isChecked = data.isDarkMode
            btnTextDown.isChecked = !data.isBigText
            btnTextUp.isChecked = data.isBigText
        }
        if (data.isShowMenu) vb.submenu.open() else vb.submenu.close()
    }

    override fun renderUi(data: ArticleState) {
        delegate.localNightMode =
            if (data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        with(vb.tvTextContent){
            textSize = if(data.isBigText) 18f else 14f
            movementMethod = LinkMovementMethod()// for scroll and handle link click
            MarkdownBuilder(context)
                .markdownToSpan(data.content)
                .run { setText(this, TextView.BufferType.SPANNABLE) }
        }

        with(vb.toolbar){
            title = data.title ?: "loading"
            subtitle = data.category ?: "loading"
            if (data.categoryIcon != null) logo = ContextCompat.getDrawable(context, data.categoryIcon as Int)
        }

        if (data.isLoadingContent) return
        if (data.isSearch){
            renderSearchResult(data.searchResults)
            renderSearchPosition(data.searchPosition)
        }else clearSearchResult()
    }

    override fun setupToolbar() {
        with(vb.toolbar) {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            val logo = if (childCount > 2) this.getChildAt(2) as ImageView else null
            logo?.scaleType = ImageView.ScaleType.CENTER_CROP
            println(logo?.id)
            val lp = logo?.layoutParams as? Toolbar.LayoutParams
            lp?.let {
                it.width = this@RootActivity.dpToIntPx(40)
                it.height = this@RootActivity.dpToIntPx(40)
                it.marginEnd = this@RootActivity.dpToIntPx(16)
                logo.layoutParams = it
            }
        }
    }

    override fun renderSearchResult(searchResult: List<Pair<Int, Int>>) {
        val content = vb.tvTextContent.text as Spannable
        clearSearchResult()
        searchResult.forEach { (start, end) ->
            content.setSpan(
                SearchSpan(bgColor, fgColor),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun renderSearchPosition(searchPosition: Int) {
        val content = vb.tvTextContent.text as Spannable
        val spans = content.getSpans<SearchSpan>()
        content.getSpans<SearchFocusSpan>()
            .forEach { content.removeSpan(it) }
        if (spans.isNotEmpty()){
            val result = spans[searchPosition]
            Selection.setSelection(content, content.getSpanStart(result))
            content.setSpan(
                SearchFocusSpan(bgColor, fgColor),
                content.getSpanStart(result),
                content.getSpanEnd(result),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun clearSearchResult() {
        val content = vb.tvTextContent.text as Spannable
        content.getSpans<SearchSpan>()
            .forEach { content.removeSpan(it) }

    }

    override fun showSearchBar(resultsCount: Int, searchPosition: Int) {
        with(vb.bottombar){
            setSearchState(true)
            setSearchInfo(resultsCount, searchPosition)
        }
        vb.scroll.setMarginOptionally(bottom = dpToIntPx(56))
    }

    override fun hideSearchBar() {
        with(vb.bottombar){
            setSearchState(false)
        }
        vb.scroll.setMarginOptionally(bottom = dpToIntPx(0))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val menuItem = menu?.findItem(R.id.action_search)

        searchView = (menuItem?.actionView as? SearchView)!!
        searchView.queryHint = getString(R.string.search)

        if (viewModel.currentState.isSearch) {
            menuItem.expandActionView()
            searchView.setQuery(viewModel.currentState.searchQuery, false)
            searchView.requestFocus()
        } else {
            searchView.clearFocus()
        }

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(true)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewModel.handleSearchMode(false)
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.handleSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.handleSearch(newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }
}