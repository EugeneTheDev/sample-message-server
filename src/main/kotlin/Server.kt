import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.*

fun main() {
    embeddedServer(Netty, System.getenv("PORT").toInt()) {

        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }

        routing {
            get("/users") {
                call.respond(currentUsers)
            }

            get("/allMessages") {
                call.respond(messages)
            }

            get("/lastMessages") {
                val sinceId = call.request.queryParameters["sinceId"]?.toIntOrNull()

                sinceId?.also {
                    call.respond(messages.filter { message ->  message.id > it })
                } ?: kotlin.run {
                    call.respond(HttpStatusCode.BadRequest, ErrorMessage("Missing or invalid parameter sinceId"))
                }
            }

            post("/register") {
                val user = call.receive<User>()

                if (!currentUsers.any { it.username == user.username }) {
                    currentUsers.add(user)
                    call.respond(SuccessMessage("User was registered"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorMessage("User is already exist"))
                }
            }

            post("/postMessage") {
                val messageRequest = call.receive<MessageRequest>()

                if (messageRequest.text.isNotBlank() && currentUsers.any { it.username == messageRequest.author }) {
                    messages.add(Message(
                        id = messages.size,
                        author = messageRequest.author,
                        text = messageRequest.text,
                        date = Date()
                    ))
                    call.respond(SuccessMessage("Message sent"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorMessage("Text is empty or author is unknown"))
                }
            }
        }
    }.start(wait = true)
}

data class User(
    val username: String,
    val firstName: String,
    val lastName: String
)

data class Message(
    val id: Int,
    val author: String,
    val text: String,
    val date: Date
)

data class MessageRequest(
    val author: String,
    val text: String
)

data class SuccessMessage(
    val message: String
) {
    val success: Boolean = true
}

data class ErrorMessage(
    val message: String
) {
    val success: Boolean = false
}

val currentUsers = mutableListOf<User>()
val messages = mutableListOf<Message>()