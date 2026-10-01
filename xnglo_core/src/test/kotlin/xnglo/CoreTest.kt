import xnglo.XngloCore

// Minimal hand-rolled harness (no JUnit dependency, so this stays
// buildable with a bare `kotlinc`, same reasoning as
// xnglonpp-ext/tests/core_test.cpp). Mirrors the same assertions as that
// C++ port and htrlib's own __tests__/hsciistr.test.ts, since all three
// need to agree for devanagari.
var failures = 0

fun expectEq(label: String, got: String, want: String) {
    if (got != want) {
        System.err.println("FAIL $label: got \"$got\" want \"$want\"")
        failures++
    } else {
        println("ok   $label -> \"$got\"")
    }
}

fun main() {
    expectEq("xi38(अनार)", XngloCore.toXi38("अनार"), "Anar")
    expectEq("xi38(नमस्ते)", XngloCore.toXi38("नमस्ते"), "nmsTe")
    expectEq(
        "xi38(हल्दी के पानी में नहाना चाहिए)",
        XngloCore.toXi38("हल्दी के पानी में नहाना चाहिए"),
        "HlDi ke pani me nHana caHiye"
    )
    expectEq(
        "xi38(जाऊँ दुआ कई पढ़ाई कउआ)",
        XngloCore.toXi38("जाऊँ दुआ कई पढ़ाई कउआ"),
        "zau Dua kyi pRai kAua"
    )
    expectEq(
        "xi38(गएैसा गए आए हुए लिए)",
        XngloCore.toXi38("गएैसा गए आए हुए लिए"),
        "gyesa gye aye Huye liye"
    )
    // decomposed-nukta ढ़ (ढ + combining U+093C), not the precomposed form
    expectEq("xi38(पढ़ाई, decomposed nukta)", XngloCore.toXi38("पढ\u093Cाई"), "pRai")

    expectEq("u38(नमस्ते)", XngloCore.toU38("नमस्ते"), "नमसतe")
    expectEq("u38(अनार)", XngloCore.toU38("अनार"), "अनaर")

    // xi38, other 5 scripts -- reusing the same smoke-test strings
    // verified against htrlib directly when each uN_map.ts was populated
    // there.
    expectEq(
        "xi38 gurmukhi(ਸਤ ਸ੍ਰੀ ਅਕਾਲ ਪੰਜਾਬ)",
        XngloCore.toXi38("ਸਤ ਸ੍ਰੀ ਅਕਾਲ ਪੰਜਾਬ"), "sT sri Akal pnzab"
    )
    expectEq("xi38 gujarati(કેમ છો ગુજરાત)", XngloCore.toXi38("કેમ છો ગુજરાત"), "kem Co guzraT")
    // precomposed nukta ଡ଼ (U+0B5C) -- decomposed nukta isn't composed for
    // any script but devanagari yet, see CLAUDE.md.
    expectEq("xi38 oriya(ଓ\u0B5Cିଶା, precomposed nukta)", XngloCore.toXi38("ଓ\u0B5Cିଶା"), "oRiSa")
    expectEq("xi38 kannada(ನಮಸ್ಕಾರ ಕನ್ನಡ)", XngloCore.toXi38("ನಮಸ್ಕಾರ ಕನ್ನಡ"), "nmskar knnd")
    expectEq("xi38 sinhala(ආයුබෝවන්)", XngloCore.toXi38("ආයුබෝවන්"), "Aayubown")

    // u38, gurmukhi -- ISCII-aligned, so the generic letter/mark split in
    // toU38() should apply the same way it does for devanagari.
    expectEq("u38 gurmukhi(ਸਤ)", XngloCore.toU38("ਸਤ"), "ਸਤ") // no marks, letters untouched
    expectEq("u38 sinhala passthrough(ආයුබෝවන්)", XngloCore.toU38("ආයුබෝවන්"), "ආයුබෝවන්")

    // anusvara/candrabindu context handling -- repo owner's reported bugs:
    // "N" (later "m") was showing up where xi38 correctly drops/resolves
    // it; u38 should assimilate to the actual native nasal consonant.
    expectEq("u38(में स्थित)", XngloCore.toU38("में स्थित"), "मe सथiत")
    expectEq(
        "u38(संस्कृति)",
        XngloCore.toU38("संस्कृति"),
        "\u0938\u0928\u0938\u0915" + "ri" + "\u0924" + "i"
    )

    // htrlib's test-parts/unicode-india-to-u38.ts testcase1: exercises
    // the whitelist/normalization rules (ङ/ञ/ण->न, ष->स), independent
    // vowels other than अ, the और hardcode, danda dropping, and matra
    // ai's leading-consonant promotion all in one sentence.
    expectEq(
        "u38(ऋषि के आश्रम में...)",
        XngloCore.toU38(
            "ऋषि के आश्रम में (गंगा) किनारे बैठकर शिष्यों ने वाङ्गमय और चञ्चल मन को एकाग्र करने का पाठ सीखा।"
        ),
        "रiसi कe अaशरम मe (गनगa) कiनaरe बयeठकर शiसयo नe वaनगमय और चनचल मन कo eकaगर करनe कa पaठ सiखa"
    )

    if (failures > 0) {
        System.err.println("$failures failure(s)")
        kotlin.system.exitProcess(1)
    }
    println("all passed")
}
