package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

enum class Foo {
    `yield`
}

fun box(): String {
    testNotRenamed("yield", { Foo.`yield` })

    return "OK"
}