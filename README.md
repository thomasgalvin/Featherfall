# Featherfall

What happens when you [drop a wizard](http://www.dropwizard.io/)? She casts [Featherfall](https://roll20.net/compendium/dnd5e/Feather%20Fall#content), of course!

*Featherfall* is a set of convenience utilities for Dropwizard, which eliminates as much boilerplate as possible when setting up a new REST API. 

With just a few lines of code, you can stand up a REST API, serve static content from your classpath or the file system, set up health checks, and create Dropwizard commands.

Featherfall also provides a user management service and an auditing database, allowing you to track any changes made to the data in your system.

Standing up a Featherfall service is as easy as:

```kotlin
fun main(args: Array<String>) {
	val userDB = createUserDB()
	val accountRequestDB = createAccountRequestDB(userDB)
	val auditDB = createAuditDB()
	val loginManager = LoginManager( userDB , auditDB, DefaultTimeProvider() )
	
	val api = arrayListOf(
		LoginResource(loginManager),
		LogoutResource(loginManager),
		HelloWorldResource() 
	)
	
	val healthChecks = arrayListOf(
		HealthCheckContext( "/api/json/username", HelloHealthCheck() )
	)
	
	val statics = arrayListOf(
		//this creates a web page available at http://localhost:8080/html
		StaticResource(location = "/path/to/my/webapp/", context = "/html")
	)
	val tasks = arrayListOf( ShutdownTask() )
	
	val server = FeatherfallServer<HelloConfig>(
		serverRootPath = "/api", //sets the root path to the JSON API
		apiResources = api,
		healthChecks = healthChecks,
		staticResources = statics,
		tasks = tasks
	)
	server.start()
}
```
