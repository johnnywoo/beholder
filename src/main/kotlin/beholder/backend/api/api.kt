package beholder.backend.api

import java.util.ArrayList
import java.util.HashMap

open class Message(val action: String)

class EchoMessage(val data: String) : Message("echo")

class LoginMessage(val apiKey: String) : Message("login")

class ComplexMessage(val argFromConstructor: String): Message("whatever") {
    val stringProp: String = "stringProp"

    val listProp = ArrayList<String>()
    val hashProp = HashMap<Int, String>()

    val otherClassProp = Blah("dsd")
}

class Blah(val kekeke: String) {

}
