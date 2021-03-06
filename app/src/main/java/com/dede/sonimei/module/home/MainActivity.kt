package com.dede.sonimei.module.home

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.dede.sonimei.APE_LINK
import com.dede.sonimei.R
import com.dede.sonimei.WEB_LINK
import com.dede.sonimei.base.BaseActivity
import com.dede.sonimei.component.CaretDrawable
import com.dede.sonimei.component.PlayBottomSheetBehavior
import com.dede.sonimei.data.search.SearchSong
import com.dede.sonimei.module.play.PlayFragment
import com.dede.sonimei.module.search.SearchFragment
import com.dede.sonimei.module.setting.SettingActivity
import com.dede.sonimei.util.extends.hide
import com.dede.sonimei.util.extends.isNull
import com.dede.sonimei.util.extends.show
import com.dede.sonimei.util.extends.to
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_play_control.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class MainActivity : BaseActivity() {

    override fun getLayoutId() = R.layout.activity_main

    private lateinit var playBehavior: PlayBottomSheetBehavior<FrameLayout>
    private lateinit var playFragment: PlayFragment

    private val arrowAnim by lazy {
        val anim = ValueAnimator
                .ofFloat()
                .setDuration(250L)
        anim.interpolator = LinearInterpolator()
        return@lazy anim
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 全透明状态栏
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        val searchFragment = SearchFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_content, searchFragment)
//                .addToBackStack(null)
                .commit()

        playFragment = supportFragmentManager.findFragmentById(R.id.play_fragment) as PlayFragment

        val caretDrawable = CaretDrawable(this)
        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_UP
        iv_arrow_indicators.setImageDrawable(caretDrawable)

        playBehavior = BottomSheetBehavior.from(bottom_sheet).to()
        playBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var lastOffset = 0f
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (playBehavior.state == BottomSheetBehavior.STATE_SETTLING) {
                    if (lastOffset > slideOffset) {
                        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_DOWN
                    } else {
                        caretDrawable.caretProgress = CaretDrawable.PROGRESS_CARET_POINTING_UP
                    }
                }
                lastOffset = slideOffset

                bottom_sheet.open = if (slideOffset > 0.85f) {
                    fl_bottom_play.hide()
                    true
                } else {
                    fl_bottom_play.show()
                    false
                }

                // 隐藏fragment 优化过度绘制
                if (slideOffset >= 1f) {
                    fl_content.hide()
                } else {
                    fl_content.show()
                }

                var b = 1 - slideOffset * 2f
                if (b < 0f) b = 0f
                fl_bottom_play.alpha = b
            }

            @SuppressLint("RestrictedApi")
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        supportActionBar?.collapseActionView()// 关闭ActionBar搜索框

                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (arrowAnim.isRunning) {
                            arrowAnim.cancel()
                        }
                        arrowAnim.setFloatValues(caretDrawable.caretProgress, CaretDrawable.PROGRESS_CARET_POINTING_DOWN)
                        arrowAnim.addUpdateListener { animation ->
                            caretDrawable.caretProgress = animation.animatedValue as Float
                        }
                        arrowAnim.start()
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (arrowAnim.isRunning) {
                            arrowAnim.cancel()
                        }
                        arrowAnim.setFloatValues(caretDrawable.caretProgress, CaretDrawable.PROGRESS_CARET_POINTING_UP)
                        arrowAnim.addUpdateListener { animation ->
                            caretDrawable.caretProgress = animation.animatedValue as Float
                        }
                        arrowAnim.start()
                    }
                }
            }
        })
        playBehavior.onYVelocityChangeListener = object : PlayBottomSheetBehavior.OnYVelocityChangeListener {
            override fun onChange(vy: Float) {
                val state = playBehavior.state
                if (state == BottomSheetBehavior.STATE_EXPANDED ||
                        state == BottomSheetBehavior.STATE_COLLAPSED) {
                    return
                }
                caretDrawable.caretProgress = vy * .0023f
            }
        }

        fl_bottom_play.onClick {
            toggleBottomSheet()
        }
    }

    /**
     * 播放音乐
     */
    fun playSong(song: SearchSong?) {
        if (song == null || song.path.isNull()) {
            toast(R.string.play_path_empty)
            return
        }
        // 显示 mini play control
        playBehavior.isHideable = false
        playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//        val bottomDimens = resources.getDimensionPixelSize(R.dimen.search_list_bottom_margin)
//        fl_search_result.setPadding(0, 0, 0, bottomDimens)

        playFragment.playSong(song)
    }

    fun toggleBottomSheet() {
        if (playBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            playBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            playBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (super.onOptionsItemSelected(item)) {// 先走fragment的菜单键事件
            return true
        }
        return when (item?.itemId) {
            R.id.menu_setting -> {
                startActivity<SettingActivity>()
                true
            }
            R.id.menu_ape -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APE_LINK)))
                true
            }
            R.id.menu_about -> {
                AboutDialog(this).show()
                true
            }
            R.id.menu_webview -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WEB_LINK)))
                true
            }
            else -> {
                false
            }
        }
    }

    private var lastTime = 0L

    override fun onBackPressed() {
        if (playBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            toggleBottomSheet()
            return
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
            return
        }
        val millis = System.currentTimeMillis()
        if (lastTime + 2000L < millis) {
            toast(R.string.reclick_exit)
            lastTime = millis
        } else {
            super.onBackPressed()
        }
    }
}
