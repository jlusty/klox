package business.plants.klox

class Environment(val enclosing: Environment? = null) {
    private val values: MutableMap<String, Any?> = HashMap()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun ancestor(distance: Int): Environment {
        var environment: Environment = this
        for (i in 0 until distance) {
            // TODO: Modify types such that null case handled better here?
            environment = environment.enclosing ?: throw Error("Failed to find enclosing environment")
        }

        return environment
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }

        // TODO: No need to walk any more as only using this for globals?
        if (enclosing != null) return enclosing.get(name)

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        // TODO: No need to walk any more as only using this for globals?
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }
}