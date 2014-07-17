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

})();
