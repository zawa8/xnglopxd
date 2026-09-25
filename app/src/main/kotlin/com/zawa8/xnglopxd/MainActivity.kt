package com.zawa8.xnglopxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import xnglo.XngloCore

/**
 * A small notepad screen: type or paste text, pick a display font, and
 * transliterate the current selection (or the whole document if nothing
 * is selected) via [XngloCore] -- htrlib's devanagari xi38/u38 scheme,
 * ported to Kotlin (see xnglo_core module, and CLAUDE.md for what else
 * is/isn't ported yet).
 *
 * NOTE: this whole module is UNBUILT/UNTESTED against the real Android
 * SDK/emulator -- there's no Android toolchain in the sandbox this was
 * written in. Only xnglo_core (plain Kotlin, no Android dependency) was
 * actually compiled and run. See CLAUDE.md.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotepadScreen()
                }
            }
        }
    }
}

// A curated set of built-in font families. No custom .ttf files are
// bundled yet (e.g. a proper Devanagari display face) -- see CLAUDE.md
// for how to add one under app/src/main/res/font/ and list it here.
private data class FontChoice(val label: String, val family: FontFamily)
private val FONT_CHOICES = listOf(
    FontChoice("Default", FontFamily.Default),
    FontChoice("Serif", FontFamily.Serif),
    FontChoice("Sans Serif", FontFamily.SansSerif),
    FontChoice("Monospace", FontFamily.Monospace),
    FontChoice("Cursive", FontFamily.Cursive),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen() {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var fontChoice by remember { mutableStateOf(FONT_CHOICES[0]) }
    var fontMenuOpen by remember { mutableStateOf(false) }
    var transliterateMenuOpen by remember { mutableStateOf(false) }

    // Replaces the current selection with `transform(selection)`, or the
    // whole document if nothing is selected (selection start == end).
    fun applyTransform(transform: (String) -> String) {
        val sel = text.selection
        val full = text.text
        if (sel.collapsed) {
            val result = transform(full)
            text = TextFieldValue(result, selection = TextRange(result.length))
        } else {
            val before = full.substring(0, sel.min)
            val selected = full.substring(sel.min, sel.max)
            val after = full.substring(sel.max)
            val result = transform(selected)
            text = TextFieldValue(
                before + result + after,
                selection = TextRange(before.length + result.length)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("htr-xnglo notepad") },
                actions = {
                    IconButton(onClick = { fontMenuOpen = true }) {
                        Icon(Icons.Filled.FontDownload, contentDescription = "Pick font")
                    }
                    DropdownMenu(expanded = fontMenuOpen, onDismissRequest = { fontMenuOpen = false }) {
                        FONT_CHOICES.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.label, fontFamily = choice.family) },
                                onClick = {
                                    fontChoice = choice
                                    fontMenuOpen = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = { transliterateMenuOpen = true }) {
                        Icon(Icons.Filled.Translate, contentDescription = "Transliterate")
                    }
                    DropdownMenu(
                        expanded = transliterateMenuOpen,
                        onDismissRequest = { transliterateMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("-> xi38 (full romanization)") },
                            onClick = {
                                applyTransform(XngloCore::toXi38)
                                transliterateMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("-> u38 (keep native letters)") },
                            onClick = {
                                applyTransform(XngloCore::toU38)
                                transliterateMenuOpen = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontChoice.family),
                keyboardOptions = KeyboardOptions.Default,
            )
        }
    }
}
