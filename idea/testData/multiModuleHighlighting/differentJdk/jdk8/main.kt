package foo

fun mainJdk8(x: List<String>, j6List: Jdk6List<String>) {
    x.stream().filter { it.length > 0 }

    j6List.size
    // TODO: stream should be available
    j6List.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: stream">stream</error>()

    buildList().<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: stream">stream</error>()
}
