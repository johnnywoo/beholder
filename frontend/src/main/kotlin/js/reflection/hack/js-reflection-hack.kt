package js.reflection.hack

import js.native

native fun jsClass<reified T>() = JavaScriptClass()

native class NativeJavaScriptClass

class JavaScriptClass(val nativeClass: NativeJavaScriptClass = js.noImpl, val name: String = "") {
    val rawConstructorJs = nativeClass.toString()
}
