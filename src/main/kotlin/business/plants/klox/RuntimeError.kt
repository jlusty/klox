package business.plants.klox

class RuntimeError(val token: Token, message: String) : RuntimeException(message)