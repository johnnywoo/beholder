// TODO move all these JS files to frontend module and copy into backend on build
(function(){

    //
    // PATCHES TO STDLIB
    //

    var MonkeyPatch = {};
    MonkeyPatch.systemOut = {
        print: function(text) {
            console.log(text)
        },
        println: function(text) {
            console.log(text + "\n")
        }
    };

    // println silently accumulates output by default in kotlin-lib.js (kotlin-js-library 0.8.11)
    // we want to redirect prints to console instead
    Kotlin.System.out = function () {
        return MonkeyPatch.systemOut;
    };


    //
    // BROWSER NATIVE
    //

    // Here we want to replace stub definitions of BrowserNative methods with
    // actual JS code that has access to global scope and other unfase things,
    // so we can work with them in kotlin code.
    var origCreateClass = Kotlin.createClass;
    Kotlin.createClass = function() {
        var argsArray = Array.prototype.slice.call(arguments, 0);
        if (arguments[2] && arguments[2].hasOwnProperty('thisIsBrowserNativeClassPleaseHackItMisterMonkey')) {
            var resolveNativeValue = function (object) {
                return object.nativeValue === null ? window : object.nativeValue;
            };

            arguments[2].get = function (name) {
                var nativeValue = resolveNativeValue(this);
                if (typeof nativeValue === 'undefined') {
                    throw new 'UndefinedNativeValueException';
                }
                return new Kotlin.modules.script.beholder.frontend.sugar.BrowserNative(
                    (typeof nativeValue[name] === 'undefined') ? null : nativeValue[name]
                );
            };

            arguments[2].call = function (args, context) { // vararg args: Any?, context: BrowserNative? = null
                if (context === void 0) {
                    context = this;
                }
                return new Kotlin.modules.script.beholder.frontend.sugar.BrowserNative(
                    resolveNativeValue(this).apply(context ? context.nativeValue : this, args)
                );
            };

            arguments[2].toString = function () {
                return Kotlin.toString(resolveNativeValue(this));
            };
        }

        return origCreateClass.apply(this, argsArray);
    }

})();
