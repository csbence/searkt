package edu.unh.cs.searkt.environment.pancake

class PancakeState(val ordering: ByteArray, var indexFlipped: Int) {

    private var hashCode: Int = 0

    init {
        generateKey()
    }

    override fun equals(other: Any?): Boolean {
        other as PancakeState
        return other.ordering.contentEquals(ordering)
    }

    override fun hashCode(): Int {
        return hashCode
    }

    private fun generateKey() {
        /* FNV-1a */
        var offsetBasis = 0xCBF29CE48422232
        val fnvPrime = 0x100000001B3
        ordering.forEach { value ->
            offsetBasis = offsetBasis xor value.toLong()
            offsetBasis *= fnvPrime
        }

        hashCode = offsetBasis.toInt()

    }
}