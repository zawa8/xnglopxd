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

    if (failures > 0) {
        System.err.println("$failures failure(s)")
        kotlin.system.exitProcess(1)
    }
    println("all passed")
}
