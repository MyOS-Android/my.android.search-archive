package my.android.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tabs.*
import java.util.regex.Pattern

class ReverseImageSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_SEND || intent.action == REVERSE_SEARCH_LINK) {
            // If text/* is shared to us and the text is not a image link then call WebSearchActivity
            if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/")!!) {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    val imgPattern =
                        Pattern.compile("^https://[-a-zA-Z0-9+&@#/%?=~_|!:,.;\\[\\]]*[-a-zA-Z0-9+&\\]@#/%=~_|](\\.(?i)(jpe?g|png|gif|bmp))\$")
                    if (!imgPattern.matcher(it).matches()) {
                        // If text sent is not a image link, then call WebSearchActivity
                        startActivity(
                            Intent(this, WebSearchActivity::class.java)
                                .putExtra(Intent.EXTRA_PROCESS_TEXT, it)
                                .putExtra(WebSearchActivity.META,
                                    PreferenceManager.getDefaultSharedPreferences(this)
                                        .getBoolean(getString(R.string.meta_search_key), true)
                                )
                        )
                        finish()
                        return
                    }
                }
            }

            // Otherwise do image reverse search
            setContentView(R.layout.activity_tabs)

            viewPager.adapter = ViewStateAdapter(supportFragmentManager, lifecycle)
            TabLayoutMediator(tabs, viewPager) {tab, position ->
                when (position) {
                    ReverseImageSearchFragment.SERVICE_GOOGLE -> {tab.text = "Google"}
                    ReverseImageSearchFragment.SERVICE_SOGOU -> {tab.text = getString(R.string.sogou)}
                    ReverseImageSearchFragment.SERVICE_BING -> {tab.text = "Bing"}
                    ReverseImageSearchFragment.SERVICE_YANDEX -> {tab.text = "Yandex"}
                    //ReverseImageSearchFragment.SERVICE_TINEYE -> {tab.text = "TinEye"}
                    //ReverseImageSearchFragment.SERVICE_PAILITAO -> {tab.text = getString(R.string.pailitao)}
                }
            }.attach()

            viewPager.recyclerView.enforceSingleScrollDirection()

            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
                override fun onTabSelected(tab: TabLayout.Tab) {
                    tab.view.setOnLongClickListener { v->
                        PopupMenu(baseContext, v).run {
                            menu.add(Menu.NONE, 0, 0, R.string.menuitem_browser)
                            menu.add(Menu.NONE, 1, 1, R.string.menuitem_share_hyperlink)
                            menu.add(Menu.NONE, 2, 2, R.string.menuitem_copy_hyperlink)
                            show()
                            setOnMenuItemClickListener { menuItem->
                                (supportFragmentManager.findFragmentByTag("f${tabs.selectedTabPosition}") as ReverseImageSearchFragment).getCurrentUrl()?.let { url->
                                    when(menuItem.itemId) {
                                        0-> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        1-> startActivity(Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, url)
                                            type = "text/plain"
                                        })
                                        2-> (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", url))
                                    }
                                }
                                true
                            }
                        }
                        true
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            // Use reflection to reduce Viewpager2 slide sensitivity, so that PhotoView inside can zoom presently
            val recyclerView = (ViewPager2::class.java.getDeclaredField("mRecyclerView").apply{ isAccessible = true }).get(viewPager) as RecyclerView
            (RecyclerView::class.java.getDeclaredField("mTouchSlop")).apply {
                isAccessible = true
                set(recyclerView, (get(recyclerView) as Int) * 7)
            }

        }
        else finish()
    }

    private class ViewStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int = ReverseImageSearchFragment.SERVICES_TOTAL

        override fun createFragment(position: Int): Fragment {
            return ReverseImageSearchFragment.newInstance(position)
        }
    }

    companion object {
        const val REVERSE_SEARCH_LINK = "my.android.search.REVERSE_SEARCH_LINK"
    }
}
