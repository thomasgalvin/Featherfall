package galvin.dw.demo

import galvin.dw.FeatherfallServer
import galvin.dw.StaticResource
import io.dropwizard.Configuration
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    try {
        println("Setting up server...")
        val api = arrayListOf<Any>( HelloResource() )
        val statics = arrayListOf<StaticResource>( StaticResource( location="/galvin/dw/demo/html/", context="/html" ) )
        //val statics = arrayListOf<StaticResource>( StaticResource( location="/tmp/", context="/html", onClasspath=false ) )

        val server = FeatherfallServer<HelloConfig>(
                apiResources = api,
                staticResources = statics
        )
        server.start()
    }
    catch(t: Throwable){
        t.printStackTrace()
    }
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
    @Path("/json/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun helloJson( @PathParam("name") name: String ) : Greeting{
        return Greeting(name)
    }
}

data class Greeting( val name: String = "world", val message: String = "Hello, $name!" )