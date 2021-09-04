# klox

klox is an interpreter for the Lox language (from the book "Crafting Interpreters"), written in Kotlin.

## Running klox

Running `./gradlew run --console=plain` will allow you to use the Lox REPL.
Running `./gradlew run --args='C:\some\absolute\path\to\file.txt'` will allow you to run Lox on a file.

## Running code generation tool

Running `kotlinc -script src/main/kotlin/business/plants/tool/GenerateAst.kts src/main/kotlin/business/plants/klox/` will run the code generation script.
