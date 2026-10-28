package com.pranit.vibes

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(PlaylistAutomationService.PREFS, Context.MODE_PRIVATE) }
    private lateinit var songsInput: EditText
    private lateinit var playlistInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(21, 19, 34)
        window.navigationBarColor = Color.rgb(21, 19, 34)
        buildUi()
        songsInput.setText(prefs.getString("songs", ""))
        playlistInput.setText(prefs.getString("playlist", "PRANIT VIBES"))
        status.text = prefs.getString("status", "Paste songs, enable the helper, then start.")
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(24)); setBackgroundColor(Color.rgb(21, 19, 34))
        }
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "PRANIT VIBES  ♫"; textSize = 28f; setTextColor(Color.rgb(200, 250, 116)); typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(label("JIOSAAVN PLAYLIST AUTOMATOR · BETA"))
        root.addView(label("Playlist name"))
        playlistInput = EditText(this).apply {
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); hint = "PRANIT VIBES"
            setPadding(dp(12), dp(10), dp(12), dp(10)); setBackgroundColor(Color.rgb(42, 38, 60))
        }
        root.addView(playlistInput, matchWrap())
        root.addView(label("Song list · one song per line"))
        songsInput = EditText(this).apply {
            minLines = 8; gravity = Gravity.TOP; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            hint = "Kesariya — Arijit Singh\nHusn — Anuv Jain\nBeliever — Imagine Dragons"
            setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(Color.rgb(42, 38, 60))
        }
        root.addView(songsInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)))
        root.addView(label("This beta uses Android Accessibility to interact with visible JioSaavn controls. Supervise the first run. It never asks for your password or OTP."))
        root.addView(button("1 · Enable automation service", false) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, matchWrap())
        root.addView(button("2 · Save list", false) { saveList(); toast("Saved " + songLines().size + " songs") }, matchWrap())
        root.addView(button("3 · Start playlist automation", true) {
            saveList()
            if (songLines().isEmpty()) { toast("Paste at least one song first"); return@button }
            prefs.edit().putBoolean("running", true).putInt("index", 0).putString("status", "Starting. Keep JioSaavn in the foreground.").apply()
            val launch = packageManager.getLaunchIntentForPackage("com.jio.media.android.music")
                ?: packageManager.getLaunchIntentForPackage("com.jiosaavn")
            if (launch != null) startActivity(launch)
            else {
                prefs.edit().putBoolean("running", false).putString("status", "JioSaavn app not detected. Install/open the official app first.").apply()
                status.text = prefs.getString("status", ""); toast(status.text.toString())
            }
        }, matchWrap())
        root.addView(button("Stop automation", false) {
            prefs.edit().putBoolean("running", false).putString("status", "Stopped by you.").apply()
            status.text = "Stopped by you."; toast("Automation stopped")
        }, matchWrap())
        status = TextView(this).apply { textSize = 13f; setTextColor(Color.rgb(200, 250, 116)); setPadding(0, dp(18), 0, dp(8)) }
        root.addView(status)
        root.addView(label("Reality check: JioSaavn has no verified public playlist-write API. This is best-effort visible UI automation, not an official integration. If its layout differs, it may pause rather than add every track."))
        setContentView(scroll)
    }
    private fun saveList() {
        prefs.edit().putString("songs", songsInput.text.toString()).putString("playlist", playlistInput.text.toString().trim().ifEmpty { "PRANIT VIBES" })
            .putString("status", "List saved · " + songLines().size + " songs").apply()
        status.text = prefs.getString("status", "")
    }
    private fun songLines() = songsInput.text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() }
    private fun label(s: String) = TextView(this).apply { text = s; textSize = 13f; setTextColor(Color.rgb(190, 186, 208)); setPadding(0, dp(14), 0, dp(8)) }
    private fun button(text: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; setTextColor(if (primary) Color.rgb(21,19,34) else Color.WHITE)
        setBackgroundColor(if (primary) Color.rgb(200,250,116) else Color.rgb(48,43,69)); setOnClickListener { click() }
    }
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(9) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}