package com.arshadshah.nimaz.core.text

/**
 * A [StringProvider] that returns a stable, readable stand-in instead of a real resource.
 *
 * The format is deliberately not the real copy: a test asserting on `"string:$id"` is asserting
 * that the ViewModel resolved *that* resource, which is the thing under test, and stays green
 * when the wording changes. Pass [format] when a test needs to control the text — sorting and
 * search tests do, since those compare the resolved strings against each other.
 *
 * A test fixture rather than a test class because both sides of the seam need it: `:app`'s
 * ViewModel tests, and anything here. It replaces two ad-hoc copies that had drifted into
 * `FakePlatformSeams.kt`.
 */
class FakeStringProvider(
    private val format: (Int, List<Any>) -> String = { id, args ->
        if (args.isEmpty()) "string:$id" else "string:$id(${args.joinToString()})"
    }
) : StringProvider {
    override fun get(id: Int, vararg args: Any): String = format(id, args.toList())
    override fun quantity(id: Int, count: Int, vararg args: Any): String =
        format(id, listOf(count) + args.toList())
}
