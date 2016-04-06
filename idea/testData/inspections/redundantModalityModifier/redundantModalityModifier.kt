// Abstract
abstract class Base {
    // Redundant final
    final fun foo() {}
    // Abstract
    abstract fun bar()
    // Open
    open val gav = 42
}

class FinalDerived : Base() {
    // Redundant final
    override final fun bar() {}
    // Non-final member in final class
    override open val gav = 13
}
// Open
open class OpenDerived : Base() {
    // Final
    override final fun bar() {}
    // Redundant open
    override open val gav = 13
}
// Redundant final
final class Final
