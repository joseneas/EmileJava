package gsort.pos.engsisubiq.EmileMobile

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.AppBarLayout
import android.support.v7.app.AlertDialog
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v4.widget.DrawerLayout
import android.view.View
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.readystatesoftware.systembartint.SystemBarTintManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

import java.lang.IllegalStateException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    /**
     * When some fragment set toolbar to back state, this property will be true
     * and will be used in onBackPressed method to decide
     * if enable home button in Toolbar
     */
    private var isToolbarBackButtonEnabled: Boolean = false

    /**
     * Keep the name of current fragment on stack
     */
    private var currentFragmentTag: String = ""

    /**
     * Get a instance of LocalStorage class
     */
    private var localStorage: LocalStorage? = null

    /**
     * Keeps a instance of Android AlertDialog class
     */
    private var dialog: AlertDialog? = null

    /**
     * A Java HashMap with application fragments tags and instance
     */
    private var fragments: HashMap<Int, Fragment>? = null

    /**
     * Initial method called by Application when activity is created
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localStorage = LocalStorage.getInstance()
        localStorage!!.initialize(this)

        setContentView(R.layout.activity_main)

        val userProfile = UserProfile.getInstance()
        userProfile.setActivity(this)
        userProfile.loadFromLocalStorage()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // https://github.com/jgilfelt/SystemBarTint
        // create our manager instance after the content view is set
        val tintManager = SystemBarTintManager(this)
        // enable status bar tint
        tintManager.isStatusBarTintEnabled = true
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true)
        // set the transparent color of the status bar, 20% darker
        tintManager.setTintColor(Color.parseColor("#80215dbc"))

        // create all fragments
        fragments = HashMap()
        fragments!![R.id.exit_to_app] = LoginFragment()
        fragments!![R.id.view_msg]    = MessagesFragment()
        fragments!![R.id.send_msg]    = SendMessageFragment()
        fragments!![R.id.my_profile]  = UserProfileFragment()

        when {
            userProfile.isLoggedIn -> {
                loadInitialFragment()
                val nt = Notifications.getInstance()
                nt.initialize(this)
            }
            else -> {
                setToolBarEnabled(false)
                setDrawerEnabled(false)
                addFragment(fragments!![R.id.exit_to_app], "login")
            }
        }
    }

    /**
     * Handle back button clicks
     */
    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            supportFragmentManager.backStackEntryCount <= 1 -> moveTaskToBack(false)
            else -> super.onBackPressed()
        }

        if (isToolbarBackButtonEnabled)
            setToolBarHomeButton()
    }

    /**
     * Handle the drawer menu clicks by opening the corresponding fragment
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var tag = ""
        val id = item.itemId
        val fragment = fragments!![id]
        when (id) {
            R.id.view_msg -> {
                tag = "messages_view"
            }
            R.id.send_msg -> {
                tag = "send_message"
            }
            R.id.my_profile -> {
                tag = "user_profile"
            }
            R.id.exit_to_app -> {
                // set user logged in to false
                UserProfile.getInstance().logout()

                // disable toolbar and drawer
                setToolBarEnabled(false)
                setDrawerEnabled(false)

                // remove all tags from stack
                clearFragmentsStack()

                tag = "user_login"
            }
        }

        if (currentFragmentTag != tag)
            addFragment(fragment, tag)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun showAlertDialog(title: String, message: String?, positiveBtnText: String? = "OK", confirmCallback: (() -> Unit)? = null, negativeBtnText: String? = "", cancelCallback: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)

        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveBtnText, { _, _ -> confirmCallback?.invoke() })

        if (negativeBtnText != "")
            builder.setNegativeButton(negativeBtnText, DialogInterface.OnClickListener { _, _ -> cancelCallback?.invoke() })

        dialog = builder.create()
        dialog!!.window.attributes.windowAnimations = R.style.DialogAnimation
        dialog!!.window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog!!.show()
    }

    fun loadInitialFragment(clearStack: Boolean? = false) {
        // set drawer menu listener clicks
        initializeWidgets()

        if (clearStack!!)
            clearFragmentsStack()

        // load initial fragment
        addFragment(fragments!![R.id.view_msg], null)
        currentFragmentTag = "messages_view"
    }

    fun addFragment(fragment: Fragment? = null, tag: String?) {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, R.anim.slide_out_right)
                .replace(R.id.content_frame, fragment)
                .addToBackStack(tag)
                .commit()
        supportFragmentManager.executePendingTransactions()
        currentFragmentTag = tag ?: ""
    }

    fun setNavViewItemSelected(id: Int) {
        nav_view.setCheckedItem(id)
    }

    fun setToolBarHomeButton() {
        setKeyboardEnabled(false)
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        supportActionBar!!.setDisplayShowHomeEnabled(false)

        toolbar!!.setNavigationIcon(R.drawable.ic_menu)
        toolbar!!.setNavigationOnClickListener({
            drawer_layout.openDrawer(GravityCompat.START)
        })

        setDrawerEnabled(true)

        isToolbarBackButtonEnabled = false
    }

    fun setToolBarBackButton() {
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        setDrawerEnabled(false)

        isToolbarBackButtonEnabled = true

        toolbar!!.setNavigationOnClickListener(View.OnClickListener {
            onBackPressed()
        })
    }

    fun setToolBarTitle(title: String) {
        toolbar!!.title = title
    }

    fun setKeyboardEnabled(enabled: Boolean, editText: EditText? = null) {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        // Find the currently focused view, so we can grab the correct window token from it.

        var view = this.currentFocus
        // If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null)
            view = View(this)

        if (enabled)
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        else
            imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun setNavViewUserInformation(userProfile: UserProfile) {
        val navigationView = findViewById<View>(R.id.nav_view) as? NavigationView
        val headerView = navigationView!!.getHeaderView(0)

        val navUsername = headerView.findViewById(R.id.navHeaderUserName) as? TextView
        val navUserEmail = headerView.findViewById(R.id.navHeaderUserEmail) as? TextView

        runOnUiThread({
            navUsername!!.text = userProfile.shortUsername
            navUserEmail!!.text = userProfile.email.toLowerCase()
        })
    }

    fun capitalizeString(line: String): String {
        return Character.toUpperCase(line.toCharArray()[0]) + line.substring(1).toLowerCase()
    }

    private fun setDrawerEnabled(enabled: Boolean) {
        val lockMode = if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        drawer_layout.addDrawerListener(toggle)
        drawer_layout.setDrawerLockMode(lockMode)

        toggle.syncState()
        toggle.onDrawerStateChanged(lockMode)
        toggle.isDrawerIndicatorEnabled = enabled
    }

    private fun setToolBarEnabled(enabled: Boolean) {
        val visibilityMode = if (enabled) View.VISIBLE else View.GONE
        val appBarLayout: AppBarLayout = findViewById(R.id.appBarLayout)

        if (appBarLayout.visibility != visibilityMode)
            appBarLayout.visibility = visibilityMode

        if (toolbar.visibility != visibilityMode)
            toolbar.visibility = visibilityMode

        setSupportActionBar(toolbar)
    }

    private fun initializeWidgets() {
        setToolBarEnabled(true)
        setDrawerEnabled(true)

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun clearFragmentsStack() {
        var i = 0
        val fragmentsSize = supportFragmentManager.backStackEntryCount

        while (i < fragmentsSize) {
            try {
                supportFragmentManager!!.popBackStackImmediate()
                supportFragmentManager!!.beginTransaction().disallowAddToBackStack().commitAllowingStateLoss()
                i++
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}
