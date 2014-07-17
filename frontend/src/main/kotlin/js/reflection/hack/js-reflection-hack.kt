package js.reflection.hack

import js.native
import js.regexp.RegExp
import js.regexp.replaceRegexp
import js.regexp.replaceAllRegexp
import java.util.ArrayList
import js.regexp.matchAllRegexp
import java.util.HashMap

native fun jsClass<reified T>() = JavaScriptClass()

native class NativeJavaScriptClass {
    [native("\$metadata$")] val metadata: Metadata = js.noImpl
}

class Metadata {
    native val baseClasses = Array(0, {NativeJavaScriptClass()})
}

class JavaScriptClass(val nativeClass: NativeJavaScriptClass = js.noImpl, val name: String = "") {
    /**
        A Kotlin constructor looks like this:
        function $fun(argFromConstructor) {
            "use strict";
            $fun.baseInitializer.call(this, 'whatever');
            this.argFromConstructor = argFromConstructor;
            this.stringProp = 'stringProp';
            this.listProp = new Kotlin.ArrayList();
            this.hashProp = new Kotlin.PrimitiveHashMap();
            this.otherClassProp = new _.beholder.backend.api.Blah('dsd');
        }
     */

    val baseClasses: Array<NativeJavaScriptClass>
        get() = nativeClass.metadata.baseClasses

    fun getConstructorArgNames(): List<String> {
        val args = nativeClass.toString()
            .replaceRegexp(RegExp("((\\/\\/.*$)|(\\/\\*[\\s\\S]*?\\*\\/))", "mg"), "") // strip comments
            .split("[()]")[1] // get the part inside parens
            .split(",") // split into arg names
        val list = ArrayList<String>()
        for (arg in args) {
            list.add(arg.replaceAllRegexp("^\\s+|\\s+$", ""))
        }
        return list
    }

    fun getDefinedProperties(): MutableMap<String, Property> {
        val js = nativeClass.toString().replaceRegexp(RegExp("((\\/\\/.*$)|(\\/\\*[\\s\\S]*?\\*\\/))", "mg"), "") // strip comments
        val map = HashMap<String, Property>()
        for (match in js.matchAllRegexp("this.(\\w+)\\s*=\\s*(?:new ([\\w.]+))?")) {
            map.put(match[1], Property(if (match[2] is String) match[2] else null))
        }
        return map
    }

    fun getProperties(): Map<String, Property> {
        val map = HashMap<String, Property>()
        map.putAll(getDefinedProperties())
        for (klass in baseClasses) {
            for (entry in JavaScriptClass(klass).getProperties().entrySet()) {
                if (!map.containsKey(entry.getKey())) {
                    map.put(entry.getKey(), entry.getValue())
                }
            }
        }
        return map
    }
}

// TODO we can relatively easily support primitives (anything scalar), array lists of primitives and hash maps of primitives
// with some effort and more hacks around compiled code, we can also support userspace classes, which enables some decent complexity
class Property(val typedef: String?) {
    val isPrimitive = typedef == null
    val isList      = typedef == "Kotlin.ArrayList"
    val isMap       = typedef == "Kotlin.PrimitiveHashMap"
    val isUserspace = typedef?.substring(0, 2) == "_."
}
