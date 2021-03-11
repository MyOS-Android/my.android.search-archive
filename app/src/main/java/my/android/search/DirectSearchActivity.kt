package my.android.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.directsearch_activity.*

class DirectSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.directsearch_activity)

        query_type_rg.apply { check(if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(getString(R.string.meta_search_key), true)) R.id.meta_query else R.id.browser_query) }
        meta_query.setOnClickListener { if (edit_query.text.isNotEmpty()) searchIt(edit_query.text.toString(), true) }
        browser_query.setOnClickListener { if (edit_query.text.isNotEmpty()) searchIt(edit_query.text.toString(), false) }

        edit_query.run {
            requestFocus()
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH || id == EditorInfo.IME_NULL)
                    searchIt(edit_query.text.toString(), query_type_rg.checkedRadioButtonId==R.id.meta_query)
                else false
            }
        }
    }

    private fun searchIt(query: String, meta: Boolean) : Boolean {
        if (query.isNotBlank()) {
            // Hide soft keyboard
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(edit_query.windowToken, 0)

            if (meta) {
                if (android.util.Patterns.WEB_URL.matcher(query).matches()) {
                    val url = if (query.startsWith("http")) query else "https://$query"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                else startActivity(Intent(this, WebSearchActivity::class.java)
                        .putExtra(Intent.EXTRA_PROCESS_TEXT, query).putExtra(WebSearchActivity.META, true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            } else {
                // Default search enabled
                val sp = PreferenceManager.getDefaultSharedPreferences(this)
                val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(sp.getString(getString(R.string.search_engine_key), getString(R.string.url_duck))+query))
                startActivity(searchIntent)
            }
            finish()
            return true
        }
        else return false
    }
}
