package galvin.dw.demo

import galvin.dw.FeatherfallServer
import io.dropwizard.Configuration
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

fun main( args: Array<String> ){
    val resources = listOf<Any>( HelloWorldResource() )
    val swaggerPackages = listOf<String>( "galvin.dw.demo" )

    val server = FeatherfallServer<HelloWorldConfig>(
            resources=resources,
            swaggerPackages=swaggerPackages
    )
    server.start()
}

class HelloWorldConfig: Configuration(){}

@Path( "/" )
@Produces( MediaType.TEXT_PLAIN )
@Consumes( MediaType.APPLICATION_JSON )
class HelloWorldResource{

    @GET
    fun hello() :String {
        return "Hello, world!"
    }
}