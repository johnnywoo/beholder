package beholder.backend.api

open class Message(val action: String)

class EchoMessage(val data: String) : Message("echo")

class LoginMessage(val apiKey: String) : Message("login")

class FileFoundMessage(val file: String) : Message("file_found")
class FileLostMessage(val file: String) : Message("file_lost")
class LineScannedMessage(val file: String, val text: String) : Message("line_scanned")
