package edu.unh.cs.ai.realtimesearch.util

/**
 * @author Bence Cserna (bence@cserna.net)
 */


/**
 * Returns a sequence containing elements while the [predicate] is satisfied.
 *
 * The operation is _intermediate_ and _stateless_.
 */
fun <T> Sequence<T>.generateWhile(predicate: () -> Boolean): Sequence<T> {
    return GenerateWhileSequence(this, predicate)
}

/**
 * A sequence that returns values from the underlying [sequence] while the [predicate] function returns
 * `true`, and stops returning values once the function returns `false` for the next element.
 */
class GenerateWhileSequence<T>(private val sequence: Sequence<T>,
                               private val predicate: () -> Boolean) : Sequence<T> {

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val iterator = sequence.iterator()
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var nextItem: T? = null

        private fun calcNext() {
            iterator.hasNext()
            if (predicate()) {
                nextState = 1
                nextItem = iterator.next()
                return
            }
            nextState = 0
        }

        override fun next(): T {
            if (nextState == -1)
                calcNext() // will change nextState
            if (nextState == 0)
                throw NoSuchElementException()
            @Suppress("UNCHECKED_CAST")
            val result = nextItem as T

            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext() // will change nextState
            return nextState == 1
        }
    }
}
