package xnglo

// Ported from htrlib (https://github.com/zawa8/htrlib)
// src/hsciistr/dicts/u5_map.ts -- oriya, block U+0B00-U+0B7F.
// Keep this in sync by hand if htrlib's u5_map.ts changes; there is no
// automated sync between the two repos.

// index = codepoint - 0x0B00 (offset within the oriya block).
internal val U5_MAP: Array<String> = arrayOf(
    "", "N", "N", ":", "", "A", "a", "_i", "_i", "_u", "_u", "ri", "li",
    "", "", "_e", "_e", "", "", "o", "ou", "k", "K", "g", "G", "N", "c",
    "C", "z", "Z", "n", "t", "th", "d", "dh", "n", "T", "Th", "D", "Dh",
    "n", "", "p", "f", "b", "B", "m", "y", "r", "", "l", "l", "", "w",
    "S", "s", "s", "H", "", "", "", "!", "a", "i", "i", "u", "u", "ri",
    "r", "", "", "e", "ye", "", "", "o", "ou", "", "", "", "", "", "", "",
    "", "", "", "", "", "", "", "", "R", "R", "", "y", "ri", "li", "li",
    "li", "", "", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "",
    "w", "", "", "", "", "", "", "", "", "", "", "", "", "", ""
)

internal const val ORIYA_BASE = 0x0B00
internal const val ORIYA_VIRAMA_OFFSET = 0x4d
