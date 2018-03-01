package galvin.ff

interface TimeProvider{
    fun now(): Long
}

class DefaultTimeProvider: TimeProvider{
    override fun now(): Long = System.currentTimeMillis()
}