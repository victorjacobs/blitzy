package dev.vjcbs.blitzy.blitzortung

fun lzwDecompress(str: String): String {
    val compressed = str.map { it.code }.toMutableList()

    // Build the dictionary.
    var dictSize = 256
    val dictionary = mutableMapOf<Int, String>()
    (0 until dictSize).forEach { dictionary[it] = it.toChar().toString() }

    var w = compressed.removeAt(0).toChar().toString()
    val result = StringBuilder(w)
    for (k in compressed) {
        val entry: String = if (dictionary.containsKey(k))
            dictionary[k]!!
        else if (k == dictSize)
            w + w[0]
        else
            throw IllegalArgumentException("Bad compressed k: $k")
        result.append(entry)

        // Add w + entry[0] to the dictionary.
        dictionary[dictSize++] = w + entry[0]
        w = entry
    }

    return result.toString()
}
