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
    // uh38's actual native-letter alphabet is a SMALL WHITELIST, not "any
    // devanagari letter" -- confirmed by the repo owner's test data: even
    // genuine LETTERS (not just anusvara/candrabindu marks) outside this
    // set get normalized to the nearest letter that IS in it: ङ/ञ/ण -> न
    // (uh38 only keeps one dental nasal, not one per varga), ष -> स
    // (only one dental sibilant kept, not both). Independent vowels
    // other than अ aren't in the alphabet either and get special-cased
    // below. Devanagari-only so far, same scope as the rest of this
    // file's u38 logic -- see CLAUDE.md.
    private val DEVA_LETTER_WHITELIST_OFFSETS = setOf(
        0x05, // अ
        0x15, 0x16, 0x17, 0x18, // क ख ग घ
        0x1a, 0x1b, 0x1c, 0x1d, // च छ ज झ
        0x1f, 0x20, 0x21, 0x22, // ट ठ ड ढ
        0x24, 0x25, 0x26, 0x27, 0x28, // त थ द ध न
        0x2a, 0x2b, 0x2c, 0x2d, 0x2e, // प फ ब भ म
        0x2f, 0x30, // य र
        0x32, // ल
        0x35, // व
        0x36, // श
        0x38, // स
        0x39, // ह
        0x5c // ड़
    )
    private val DEVA_NORMALIZE_OFFSET = mapOf(
        0x19 to 0x28, // ङ -> न
        0x1e to 0x28, // ञ -> न
        0x23 to 0x28, // ण -> न
        0x37 to 0x38  // ष -> स
    )

    // Independent vowels other than अ (offset 0x06-0x14): not in the
    // alphabet, so represented as their xi38 raw value, itself further
    // split so the CONSONANT-shaped part (if any) still renders native
    // -- ऋ/ऌ's raw "ri"/"li" become र/ल (native) + i (Latin). Raw values
    // that were underscore-marked for glide handling (इ ई उ ऊ ए ऐ etc)
    // are used bare, no अ prefix -- e.g. ए -> "e", not "अe" (glide
    // marking is about mid-word vs word-initial romanization, moot here
    // since the letter itself, not a glide consonant, is what's being
    // represented). Everything else without underscore whose raw
    // doesn't start with a consonant either (just आ, raw "a") falls
    // back to "अ" + its raw value. The SAME "raw value starts with a
    // consonant-key letter -> promote that letter to native, keep the
    // rest Latin" rule also applies to MARKS below, not just these
    // independent-vowel letters -- e.g. matra ai (ै, raw "ye") ->
    // "य" + "e", confirmed by बैठकर -> बयeठकर.
    private val CONSONANT_FIRST_CHAR_OFFSET = mapOf('r' to 0x30, 'l' to 0x32, 'y' to 0x2f)

    // Promotes a raw xi38 value's leading consonant-key letter (if any)
    // to its native devanagari character, leaving the rest as-is. Used
    // both for independent vowels (devaLetterText) and for marks (the
    // main loop's else branch) -- see CONSONANT_FIRST_CHAR_OFFSET.
    private fun promoteLeadingConsonant(raw: String, base: Int): String {
        val first = raw[0]
        val off = CONSONANT_FIRST_CHAR_OFFSET[first]
        return if (off != null) (base + off).toChar() + raw.substring(1) else raw
    }

    private fun devaLetterText(offset: Int, map: Array<String>, base: Int): String {
        if (offset in DEVA_LETTER_WHITELIST_OFFSETS) {
            return (base + offset).toChar().toString()
        }
        DEVA_NORMALIZE_OFFSET[offset]?.let { return (base + it).toChar().toString() }
        if (offset in 0x06..0x14) {
            val raw = map[offset]
            if (raw.startsWith('_')) return raw.substring(1) // glide-marked -> bare, no अ prefix
            val promoted = promoteLeadingConsonant(raw, base)
            if (promoted != raw) return promoted // ऋ/ऌ
            return (base + 0x05).toChar() + raw // just आ -> अ + raw
        }
        // Not in the alphabet and not one of the cases above (e.g. a
        // rare extra letter) -- fall back to the plain xi38 value
        // rather than guessing a normalization target with no evidence
        // for it.
        return map[offset]
    }

    // "और" ("and") is common enough, and different enough from what the
    // general rule above would produce (अ + ौ's raw "ou", neither part
    // promotable -> "अour"), that it's handled as a literal whole-word
    // exception instead -- same kind of hardcode as चाहिए elsewhere in
    // htrlib's history. Matched on word boundaries so it doesn't fire
    // inside a longer word that happens to contain और as a substring.
    private val WHOLE_WORD_HARDCODES = mapOf("और" to "और")

    fun toU38(input: String): String {
        if (input.isEmpty()) return ""

        // Whole-word hardcodes are protected with a PUA placeholder
        // before any other processing, then restored verbatim at the
        // end, so nothing below can touch them.
        val hardcodeStash = mutableListOf<String>()
        var protectedInput = input
        for ((word, replacement) in WHOLE_WORD_HARDCODES) {
            val re = Regex("(?<![\u0900-\u097F])" + Regex.escape(word) + "(?![\u0900-\u097F])")
            protectedInput = re.replace(protectedInput) {
                hardcodeStash.add(replacement)
                "${hardcodeStash.size - 1}"
            }
        }

        var s = applyConjunctSpecials(protectedInput)
        s = dropMalformedVowelMatra(s)
        s = composeNukta(s)

        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val cp = c.toInt()
            val t = tableFor(cp)
            if (t == null || !t.isciiAligned) {
                out.append(c)
                i++
                continue
            }
            val offset = cp - t.base
            if (offset == t.viramaOffset) {
                i++
                continue // drop virama entirely
            }
            val isDevanagari = t.base == BLOCK_BASE
            if (!isDevanagari) {
                // Only devanagari has the confirmed whitelist/
                // normalization rules below -- other scripts fall back
                // to the old simple letter/mark split until they have
                // their own confirmed test data.
                val isLetter = offset in 0x04..0x39 || offset in 0x58..0x61 || offset == 0x7F
                out.append(if (isLetter) c else t.map[offset])
                i++
                continue
            }
            if (offset == 0x01 || offset == 0x02) {
                // Anusvara/candrabindu: dropped at a true word boundary
                // (not followed by a devanagari letter at all), न
                // otherwise -- except before a labial (प वर्ग), where it
                // stays म. (Simplified from full 5-way sandhi: ङ/ञ/ण
                // aren't in uh38's alphabet anyway, confirmed by गंगा's
                // अनुस्वार, before ग/velar, resolving to न not ङ.)
                val nt = if (i + 1 < s.length) tableFor(s[i + 1].toInt()) else null
                if (nt == null || nt != t) {
                    i++
                    continue // word boundary -> drop
                }
                val noffset = s[i + 1].toInt() - nt.base
                val nextIsLetter = noffset in 0x04..0x39 || noffset in 0x58..0x61 || noffset == 0x7F
                if (!nextIsLetter) {
                    i++
                    continue
                }
                val target = if (noffset in 0x2a..0x2e) 0x2e else 0x28 // labial->म, else->न
                out.append((t.base + target).toChar())
                i++
                continue
            }
            if (c.isLetter()) {
                out.append(devaLetterText(offset, t.map, t.base))
            } else if (offset == 0x64 || offset == 0x65) {
                // danda / double danda (।॥) -- dropped, not shown as
                // Latin "."
            } else {
                val raw = t.map[offset]
                // Vocalic-R/L MATRAS (offsets 0x43, 0x62, 0x63 -- raw
                // "ri"/"li", same text as the independent ऋ/ऌ LETTERS'
                // raw value at 0x0b/0x0c) are deliberately NOT promoted,
                // staying fully Latin -- confirmed by संस्कृति's matra
                // ऋ staying "ri", not "रi" (contrast with the
                // independent-letter ऋ in ऋषि, which DOES promote via
                // devaLetterText).
                val isVocalicMatra = offset == 0x43 || offset == 0x62 || offset == 0x63
                out.append(if (isVocalicMatra) raw else promoteLeadingConsonant(raw, t.base))
            }
            i++
        }

        var result = out.toString()
        if (hardcodeStash.isNotEmpty()) {
            result = Regex("\uE010([0-9]+)\uE011").replace(result) { m ->
                hardcodeStash[m.groupValues[1].toInt()]
            }
        }
        return result
    }
}
