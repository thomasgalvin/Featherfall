package galvin.ff

class DatabaseError :  RuntimeException {
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}