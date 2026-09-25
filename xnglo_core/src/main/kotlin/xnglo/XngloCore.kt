package xnglo

// Platform-independent core: pure Kotlin, no Android framework
// dependency, so it can be built and unit-tested with a bare `kotlinc`
// (see src/test/kotlin/) as well as from the Android app module.
//
// Ported from htrlib (https://github.com/zawa8/htrlib), specifically
// src/hsciistr/u10_to_xi52.ts and src/hsciistr/xnglo_post.ts. 6 scripts
// ported: devanagari (U1_MAP), gurmukhi (U3_MAP), gujarati (U4_MAP),
// oriya (U5_MAP), kannada (U8_MAP), sinhala (U10_MAP) -- the ones htrlib
// itself has verified per-script data for. This is a straight logical
// port of the same port already done in C++ for xnglonpp-ext (the
// Notepad++ plugin) -- see that repo's src/xnglo_core/core.cpp if the
// two ever need reconciling; they should behave identically.
object XngloCore {

    // `isciiAligned` marks whether the script shares devanagari's ISCII
    // code-point layout closely enough that toU38()'s generic letter/mark
    // range check applies -- true for the 5 non-sinhala scripts; sinhala
    // genuinely diverges (extra independent vowels shift everything
    // after them -- see u10_map.kt's own htrlib source note) so toU38()
    // doesn't support it.
    private data class ScriptTable(
        val base: Int,
        val viramaOffset: Int,
        val isciiAligned: Boolean,
        val map: Array<String>
    )

    private val SCRIPTS = listOf(
        ScriptTable(BLOCK_BASE, VIRAMA_OFFSET, true, U1_MAP),
        ScriptTable(GURMUKHI_BASE, GURMUKHI_VIRAMA_OFFSET, true, U3_MAP),
        ScriptTable(GUJARATI_BASE, GUJARATI_VIRAMA_OFFSET, true, U4_MAP),
        ScriptTable(ORIYA_BASE, ORIYA_VIRAMA_OFFSET, true, U5_MAP),
        ScriptTable(KANNADA_BASE, KANNADA_VIRAMA_OFFSET, true, U8_MAP),
        ScriptTable(SINHALA_BASE, SINHALA_VIRAMA_OFFSET, false, U10_MAP)
    )

    private fun tableFor(cp: Int): ScriptTable? =
        SCRIPTS.firstOrNull { cp >= it.base && cp < it.base + 128 }

    // An independent devanagari vowel letter (U+0904-U+0914) directly
    // followed by a dependent matra (U+093E-U+094C) is malformed -- a
    // matra only attaches to a consonant -- and in practice is someone
    // typing an extra vowel letter by mistake right before the matra
    // they meant. Drop the spurious independent vowel, keep the matra.
    // Devanagari-only so far -- harmless no-op on the other 5 scripts'
    // text, but doesn't do their equivalent cleanup yet. See CLAUDE.md.
    private fun dropMalformedVowelMatra(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c.toInt() in 0x0904..0x0914 && i + 1 < input.length &&
                input[i + 1].toInt() in 0x093E..0x094C
            ) {
                i++
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    // Composes the 8 devanagari nukta consonants (क़ ख़ ग़ ज़ ड़ ढ़ फ़ य़)
    // when the input gives them as decomposed base-letter + U+093C (not a
    // canonical Unicode decomposition, so String.normalize NFC can't do
    // this -- has to be by hand). Devanagari-only, same caveat as above.
    private val nuktaPairs = mapOf(
        '\u0915' to '\u0958', '\u0916' to '\u0959', '\u0917' to '\u095A', '\u091C' to '\u095B',
        '\u0921' to '\u095C', '\u0922' to '\u095D', '\u092B' to '\u095E', '\u092F' to '\u095F'
    )

    private fun composeNukta(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (i + 1 < input.length && input[i + 1] == '\u093C' && nuktaPairs.containsKey(c)) {
                sb.append(nuktaPairs.getValue(c))
                i += 2
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    // क्ष -> s, ज्ञ -> gy (whole-conjunct special cases, ported as-is,
    // devanagari-specific -- no-op on other scripts' text).
    private fun applyConjunctSpecials(input: String): String {
        var s = input
        s = Regex("^\u0915\u094D\u0937").replace(s, "s")
        s = Regex("(\\W)\u0915\u094D\u0937").replace(s, "$1s")
        s = Regex("\u091C\u094D\u091E").replace(s, "gy")
        return s
    }

    // Postprocessing, ported from xnglo_post.ts's xnglo_india_post().
    // Only used by toXi38(); toU38() intentionally skips this.
    private fun xngloIndiaPost(input: String): String {
        var s = input
        s = Regex("^#S").replace(s, "S")
        s = Regex("(\\W)#S").replace(s, "$1S")
        s = Regex("#S").replace(s, "kS")
        s = Regex("^_").replace(s, "")
        s = Regex("(\\W)_").replace(s, "$1")
        s = Regex("([^\\Waiueo_])_u").replace(s, "$1Au")
        s = Regex("([^\\Waiueo_])_o").replace(s, "$1Ao")
        s = Regex("a_i").replace(s, "ai")
        s = Regex("a_u").replace(s, "au")
        s = Regex("a_o").replace(s, "ao")
        s = Regex("_i").replace(s, "yi")
        s = Regex("_e").replace(s, "ye")
        s = Regex("_u").replace(s, "xu")
        s = Regex("_o").replace(s, "xo")
        s = Regex("N$").replace(s, "")
        s = Regex("N(\\W)").replace(s, "$1")
        s = Regex("Nb").replace(s, "mb")
        s = Regex("NB").replace(s, "mB")
        s = Regex("Np").replace(s, "mp")
        s = Regex("Nf").replace(s, "mf")
        s = Regex("N(?![kKgG])").replace(s, "n")
        s = Regex("([^kgcztdjqpbs])v").replace(s, "$1h")
        return s
    }

    /**
     * Full romanization (htrlib's xi38 / uL2xi52()). Runs across all 6
     * ported scripts; anything outside those blocks passes through
     * unchanged.
     */
    fun toXi38(input: String): String {
        if (input.isEmpty()) return ""
        var s = applyConjunctSpecials(input)
        s = dropMalformedVowelMatra(s)
        s = composeNukta(s)

        val out = StringBuilder()
        for (c in s) {
            val cp = c.toInt()
            val t = tableFor(cp)
            if (t != null) {
                out.append(t.map[cp - t.base])
            } else {
                out.append(c)
            }
        }
        return xngloIndiaPost(out.toString())
    }

    /**
     * Semi-transliteration (htrlib's u*38 family / uL2u38()). LETTERS
     * (consonants, independent vowels) are kept as the original
     * character; MARKS (matras, anusvara, candrabindu, visarga, nukta
     * signs) convert to their xi38 value; virama is dropped entirely.
     * Only supported for the 5 ISCII-aligned scripts -- sinhala passes
     * through untouched (see ScriptTable.isciiAligned).
     */
    fun toU38(input: String): String {
        if (input.isEmpty()) return ""
        var s = applyConjunctSpecials(input)
        s = dropMalformedVowelMatra(s)
        s = composeNukta(s)

        val out = StringBuilder()
        for (c in s) {
            val cp = c.toInt()
            val t = tableFor(cp)
            if (t == null || !t.isciiAligned) {
                out.append(c)
                continue
            }
            val offset = cp - t.base
            if (offset == t.viramaOffset) continue // drop virama entirely
            val isLetter = offset in 0x04..0x39 || offset in 0x58..0x61 || offset == 0x7F
            if (isLetter) {
                out.append(c)
            } else {
                out.append(t.map[offset])
            }
        }
        return out.toString()
    }
}
