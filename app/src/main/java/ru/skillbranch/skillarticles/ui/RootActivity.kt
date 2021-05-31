package ru.skillbranch.skillarticles.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.databinding.ActivityRootBinding
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.ArticleState
import ru.skillbranch.skillarticles.viewmodels.ArticleViewModel
import ru.skillbranch.skillarticles.viewmodels.Notify
import ru.skillbranch.skillarticles.viewmodels.ViewModelFactory

class RootActivity : AppCompatActivity() {

    private val viewModel: ArticleViewModel by viewModels{ ViewModelFactory(params = "0")}
    private lateinit var vb: ActivityRootBinding
    private val vbBottomBar
    get() = vb.bottombar.binding
    private val vbSubMenu
    get() = vb.submenu.binding

    private var searchView: SearchView? = null
    private var isSearch = false
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityRootBinding.inflate(layoutInflater)
        setContentView(vb.root)

        setupToolbar()
        setupButtonbar()
        setupSubmenu()

        viewModel.observeState(this) {
            renderUi(it)
        }
        viewModel.observeNotifications(this) {
            renderNotification(it)
        }
    }

    private fun renderNotification(notify: Notify) {
        val snackbar = Snackbar.make(vb.coordinatorContainer, notify.message, Snackbar.LENGTH_SHORT)
                .setAnchorView(vb.bottombar)
                .setActionTextColor(getColor(R.color.color_accent_dark))

        when (notify) {
            is Notify.TextMessage -> {/*nothing*/
            }
            is Notify.ActionMessage -> {
                snackbar.setActionTextColor(getColor(R.color.color_accent_dark))
                snackbar.setAction(notify.actionLabel) {
                    notify.actionHandler()
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

    private fun setupSubmenu() {
        vbSubMenu.btnTextUp.setOnClickListener { viewModel.handleUpText() }
        vbSubMenu.btnTextDown.setOnClickListener { viewModel.handleDownText() }
        vbSubMenu.switchMode.setOnClickListener { viewModel.handleNightMode() }
    }

    private fun setupButtonbar() {
        vbBottomBar.btnLike.setOnClickListener { viewModel.handleLike() }
        vbBottomBar.btnBookmark.setOnClickListener { viewModel.handleBookmark() }
        vbBottomBar.btnShare.setOnClickListener { viewModel.handleShare() }
        vbBottomBar.btnSettings.setOnClickListener { viewModel.handleToggleMenu() }
    }

    private fun renderUi(data: ArticleState) {
        vbBottomBar.btnSettings.isChecked = data.isShowMenu
        if (data.isShowMenu) vb.submenu.open() else vb.submenu.close()

        vbBottomBar.btnLike.isChecked = data.isLike
        vbBottomBar.btnBookmark.isChecked = data.isBookmark

        vbSubMenu.switchMode.isChecked = data.isDarkMode
        delegate.localNightMode =
                if (data.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

        if (data.isBigText) {
            vb.tvTextContent.textSize = 18f
            vbSubMenu.btnTextUp.isChecked = true
            vbSubMenu.btnTextDown.isChecked = false
        } else {
            vb.tvTextContent.textSize = 14f
            vbSubMenu.btnTextUp.isChecked = false
            vbSubMenu.btnTextDown.isChecked = true
        }

        vb.tvTextContent.text =
                if (data.isLoadingContent) "loading" else data.content.first() as String

        vb.toolbar.title = data.title ?: "loading"
        vb.toolbar.subtitle = data.category ?: "loading"
        if (data.categoryIcon != null) vb.toolbar.logo = getDrawable(data.categoryIcon as Int)

        isSearch = data.isSearch
        searchQuery = data.searchQuery ?: ""
        refreshSearchView()
    }

    private fun refreshSearchView() {
        if (searchView?.query.toString() != searchQuery) {
            searchView?.setQuery(searchQuery, false)
        }
        if (searchView?.isIconified != !isSearch) {
            searchView?.isIconified = !isSearch
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(vb.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (vb.toolbar.childCount > 2) vb.toolbar.getChildAt(2) as ImageView else null
        logo?.scaleType = ImageView.ScaleType.CENTER_CROP
        val lp = logo?.layoutParams as? Toolbar.LayoutParams
        lp?.let {
            it.width = this.dpToIntPx(40)
            it.height = this.dpToIntPx(40)
            it.marginEnd = this.dpToIntPx(16)
            logo.layoutParams = it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        with(menu.findItem(R.id.action_search).actionView as SearchView) {
            searchView = this
            setOnSearchClickListener {
                viewModel.handleSearchMode(true)
            }
            setOnCloseListener {
                viewModel.handleSearchMode(false)
                false
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.handleSearch(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.handleSearch(newText)
                    return true
                }

            })
        }
        refreshSearchView()
        return super.onPrepareOptionsMenu(menu)
    }
}