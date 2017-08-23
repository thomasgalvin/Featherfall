package galvin.dw.demo

import galvin.dw.FeatherfallServer
import io.dropwizard.Configuration
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    val resources = arrayListOf<Any>( HelloResource() )

    val server = FeatherfallServer<HelloConfig>(
            resources = resources
    )
    server.start()
}

class HelloConfig: Configuration(){}

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
class HelloResource(){
    @GET
    fun hello() :String{
        return "Hello, world!"
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun helloJson( @PathParam("name") name: String ) : Greeting{
        return Greeting(name)
    }
}

data class Greeting( val name: String = "world", val message: String = "Hello, $name!" )