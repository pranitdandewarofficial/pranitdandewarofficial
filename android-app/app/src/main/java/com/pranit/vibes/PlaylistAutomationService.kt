package com.pranit.vibes

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class PlaylistAutomationService : AccessibilityService() {
    private var phase = "IDLE"
    private var index = 0
    private var lastActionAt = 0L
    private var attempts = 0
    private var playlistCreated = false

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 150
        }
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (!(pkg.contains("saavn", true) || pkg.contains("jio.media", true))) return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean("running", false)) return
        if (System.currentTimeMillis() - lastActionAt < 850) return
        val songs = prefs.getString("songs", "").orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (songs.isEmpty()) { stopRun("No songs in list"); return }
        if (index >= songs.size) { stopRun("Processed $index songs. Verify the playlist for skipped tracks."); return }
        val root = rootInActiveWindow ?: return
        attempts++
        if (attempts > 55) { stopRun("Paused safely: expected JioSaavn control not found. UI may have changed."); return }

        when (phase) {
            "IDLE", "OPEN_SEARCH" -> {
                if (clickAny(root, listOf("Search", "Search songs", "Search for songs"), true) || findEditable(root) != null) {
                    phase = "TYPE_QUERY"; acted()
                }
            }
            "TYPE_QUERY" -> {
                val field = findEditable(root)
                if (field != null) {
                    val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, songs[index]) }
                    if (field.performAction(AccessibilityNodeInfo.ACTION_FOCUS) && field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                        phase = "SUBMIT_SEARCH"; acted()
                    }
                }
            }
            "SUBMIT_SEARCH" -> {
                val field = findEditable(root)
                if (clickAny(root, listOf("Search", "search"), true)) {
                    phase = "PICK_RESULT"; acted()
                }
            }
            "PICK_RESULT" -> {
                val title = songs[index].substringBefore(" — ").substringBefore(" - ").trim()
                val node = findText(root, title)
                if (node != null) {
                    val row = clickableAncestor(node, 4)
                    val menu = row?.let { findDescendantByDescription(it, listOf("More options", "More", "Options", "Song options", "Menu")) }
                    if (menu != null && menu.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        phase = "OPEN_ADD_MENU"; acted()
                    } else if (clickAny(root, listOf("Add to playlist", "Add to Playlist"), false)) {
                        phase = "PICK_PLAYLIST"; acted()
                    }
                }
            }
            "OPEN_ADD_MENU" -> if (clickAny(root, listOf("Add to playlist", "Add to Playlist"), false)) {
                phase = "PICK_PLAYLIST"; acted()
            }
            "PICK_PLAYLIST" -> {
                val playlist = prefs.getString("playlist", "PRANIT VIBES").orEmpty()
                if (!playlistCreated && clickAny(root, listOf("Create new playlist", "New playlist", "Create playlist"), false)) {
                    phase = "CREATE_PLAYLIST"; acted()
                } else if (clickAny(root, listOf(playlist), false)) {
                    phase = "CONFIRM_ADD"; acted()
                } else if (clickAny(root, listOf("Done", "Add", "Save"), false)) {
                    phase = "ADVANCE"; acted()
                }
            }
            "CREATE_PLAYLIST" -> {
                val field = findEditable(root)
                if (field != null) {
                    val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prefs.getString("playlist", "PRANIT VIBES").orEmpty()) }
                    if (field.performAction(AccessibilityNodeInfo.ACTION_FOCUS) && field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) &&
                        clickAny(root, listOf("Create", "Done", "Save"), false)) {
                        playlistCreated = true; phase = "ADVANCE"; acted()
                    }
                }
            }
            "CONFIRM_ADD" -> {
                clickAny(root, listOf("Done", "Add", "Save", "Close"), false)
                phase = "ADVANCE"; acted()
            }
            "ADVANCE" -> {
                index++
                prefs.edit().putInt("index", index).apply()
                if (index >= songs.size) stopRun("Processed $index songs. Verify the playlist for skipped tracks.")
                else {
                    phase = "OPEN_SEARCH"; attempts = 0
                    clickAny(root, listOf("Search", "Search songs"), true)
                    acted()
                }
            }
        }
    }

    private fun acted() { lastActionAt = System.currentTimeMillis() }
    private fun stopRun(message: String) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("running", false).putString("status", message).apply()
        phase = "IDLE"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun clickAny(root: AccessibilityNodeInfo, labels: List<String>, descriptions: Boolean): Boolean {
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
            for (node in nodes) {
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                if (text.equals(label, true) || (descriptions && desc.equals(label, true))) {
                    val target = if (node.isClickable) node else clickableAncestor(node, 4)
                    if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
            }
        }
        return false
    }
    private fun findText(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByText(query) ?: return null
        return nodes.firstOrNull { it.text?.toString()?.contains(query, true) == true }
    }
    private fun findEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable) return root
        for (i in 0 until root.childCount) {
            val found = root.getChild(i)?.let { findEditable(it) }
            if (found != null) return found
        }
        return null
    }
    private fun clickableAncestor(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(depth) { if (current?.isClickable == true) return current; current = current?.parent }
        return null
    }
    private fun findDescendantByDescription(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        val desc = root.contentDescription?.toString().orEmpty()
        val text = root.text?.toString().orEmpty()
        if (labels.any { desc.equals(it, true) || text.equals(it, true) }) return root
        for (i in 0 until root.childCount) {
            val found = root.getChild(i)?.let { findDescendantByDescription(it, labels) }
            if (found != null) return found
        }
        return null
    }
    override fun onInterrupt() { stopRun("Automation interrupted") }
    companion object { const val PREFS = "pranit_vibes_automation" }
}