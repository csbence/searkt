package edu.unh.cs.searkt.environment.pancake

import edu.unh.cs.searkt.environment.State

class PancakeState(val ordering: ByteArray, var indexFlipped: Int) : State<PancakeState> {

    override fun copy(): PancakeState {
        val copyOrdering = ByteArray(ordering.size)
        ordering.forEachIndexed { index, byte -> copyOrdering[index] = byte }
        return PancakeState(copyOrdering, indexFlipped)
    }

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