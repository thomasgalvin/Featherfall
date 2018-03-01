package galvin.ff.demo

import com.codahale.metrics.health.HealthCheck
import com.google.common.collect.ImmutableMultimap
import galvin.ff.*
import galvin.ff.resources.LoginResource
import galvin.ff.resources.LogoutResource
import io.dropwizard.Configuration
import io.dropwizard.servlets.tasks.Task
import java.io.File
import java.io.PrintWriter
import java.net.URL
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
                        HelloResource(),

                        // this creates a way to shut down the server via an API call; eg visiting
                        // http://localhost:8080/api/shutdown in a browser
                        ShutdownResource()
                )

                // You can see the status of these health checks by calling
                // http://localhost:8081/healthcheck
                val healthChecks = arrayListOf(
                        HealthCheckContext( "/api/json/Arwen", HelloHealthCheck() )
                )


                val statics = arrayListOf(
                        //this creates a web page available at http://localhost:8080/html
                        StaticResource(location = "/galvin/ff/demo/html/", context = "/html")
                )
                //val statics = arrayListOf( StaticResource( location="/tmp/", context="/html", onClasspath=false ) )

                // Tasks are invoked via a POST to /tasks/{task-name} on the admin port
                // eg curl -X POST http://localhost:8081/tasks/shutdown
                val tasks = arrayListOf(
                        ShutdownTask()
                )

                val server = FeatherfallServer<HelloConfig>(
                        serverRootPath = "/api", //sets the root path to the JSON API
                        apiResources = api,
                        healthChecks = healthChecks,
                        staticResources = statics,
                        tasks = tasks
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

class HelloHealthCheck: HealthCheck(){
    override fun check(): HealthCheck.Result {
        try {
            val url = URL("http://localhost:8080/api/json/Arwen")
            val result = url.readText()

            val expected =
                    "{\n" +
                            "  \"name\" : \"Arwen\",\n" +
                            "  \"message\" : \"Hello, Arwen!\"\n" +
                            "}"

            if (expected == result) {
                return HealthCheck.Result.healthy()
            }
        }
        catch( t: Throwable){
            t.printStackTrace()
        }

        return HealthCheck.Result.unhealthy("Error in hello health check")
    }
}

class ShutdownTask: Task("shutdown") {
    override fun execute(p0: ImmutableMultimap<String, String>?, p1: PrintWriter?) {
        println("Shutdown task activated")
        System.exit(0)
    }
}