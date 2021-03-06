package business.plants.klox

class LoxFunction(private val declaration: Stmt.Function, private val closure: Environment) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in 0 until declaration.params.size) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return returnValue.value
        }
        return null
    }


    override fun arity(): Int {
        return declaration.params.size
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}