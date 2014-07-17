Beholder
========

### IDEA project setup

1. File -> Import project... -> .../beholder/build.gradle

2. Run 'assemble' gradle task (from IDEA or just `./gradlew`)
   to compile everything and place certain things where IDEA can see them

3. Mark following files as plain text so they won't confuse IDEA:

        frontend/src/main/kotlin/kotlin-js-library/core/javalang.kt
        frontend/src/main/kotlin/kotlin-js-library/kotlin/Integers.kt

4. Compiled JS and Kotlin stdlib in JS are placed in `frontend/build/web`,
   which is excluded from IDEA project by default; you can remove the exclude
   in Module Settings.
