package edu.unh.cs.ai.realtimesearch.experiment.configuration

object EmptyConfiguration : ExperimentConfiguration {
    override fun set(key: String, value: Any) {
        throw UnsupportedOperationException()
    }

    override fun get(key: String) {
        throw UnsupportedOperationException()
    }

    override fun contains(key: String): Boolean {
        throw UnsupportedOperationException()
    }

    override val domainName: String
        get() {
            throw UnsupportedOperationException()
        }

    override val rawDomain: String
        get() {
            throw UnsupportedOperationException()
        }

    override val algorithmName: String
        get() {
            throw UnsupportedOperationException()
        }

    override val terminationCheckerType: String
        get() {
            throw UnsupportedOperationException()
        }

    override val terminationCheckerParameter: Int
        get() {
            throw UnsupportedOperationException()
        }
}