package galvin.ff.demo

import galvin.ff.*
import galvin.ff.resources.LoginResource
import galvin.ff.resources.LogoutResource
import io.dropwizard.Configuration
import java.io.File
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    HelloWorld.main(args)
}

class HelloWorld {
    companion object Main {
        fun main(args: Array<String>) {
            try {
                println("Setting up server...")
                val userDB = Setup.userDB()
                val accountRequestDB = Setup.accountRequestDB(userDB)
                val auditDB = Setup.auditDB()
                val loginManager = LoginManager( userDB , auditDB, DefaultTimeProvider() )

                Setup.createDefaultUsers( userDB, accountRequestDB )

                val api = arrayListOf(
                        LoginResource(loginManager),
                        LogoutResource(loginManager),

                        // this creates a simple "hello world" JSON API, which can be
                        // accessed at (eg)
                        //     http://localhost:8080/api/
                        //         (just returns "Hello, world!" as plain text)
                        // or
                        //     http://localhost:8080/api/json/Thomas
                        //         (returns a JSON formatted greeting to Thomas)
                        HelloResource().hello(),

                        // this creates a way to shut down the server via an API call; eg visiting
                        // http://localhost:8080/api/shutdown in a browser
                        ShutdownResource()
                )


                val statics = arrayListOf(
                        //this creates a web page available at http://localhost:8080/html
                        StaticResource(location = "/galvin/ff/demo/html/", context = "/html")
                )
                //val statics = arrayListOf( StaticResource( location="/tmp/", context="/html", onClasspath=false ) )

                val server = FeatherfallServer<HelloConfig>(
                        serverRootPath = "/api", //sets the root path to the JSON API
                        apiResources = api,
                        staticResources = statics
                )
                server.start()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }


    }
}

class HelloConfig: Configuration()

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
class HelloResource {
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

@Path("/shutdown")
@Produces(MediaType.TEXT_PLAIN)
class ShutdownResource{
    @GET
    fun shutdown(): String{
        println("Received shutdown signal")
        ShutdownThread().start()
        return "Shutting down"
    }
}

class ShutdownThread: Thread(){
    override fun run(){
        try{
            Thread.sleep(100)
            System.exit(0)
        }
        catch(t: Throwable){}
    }
}