package galvin.dw

import io.dropwizard.Application
import java.io.File
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import org.glassfish.jersey.server.ServerProperties
import com.codahale.metrics.health.HealthCheck
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle
import io.dropwizard.setup.Bootstrap
import io.federecio.dropwizard.swagger.SwaggerBundle
import java.util.*
import javax.net.ssl.*
import javax.servlet.DispatcherType
import java.util.EnumSet
import org.eclipse.jetty.servlets.CrossOriginFilter
import com.fasterxml.jackson.databind.SerializationFeature

/**
 * This class provides a convenient wa of configuring and starting
 * a Dropwizard server.
 *
 * Usage will look something like this:
 *
 * val server: FeatherfallServer( ... )
 * server.start()
 */
class FeatherfallServer<T: Configuration>( private val configFile: File? = null,
                                           serverRootPath: String? = null,
                                           keystore: Keystore? = null,
                                           disableSslValidation: Boolean = false,
                                           private val jsonPrettyPrint: Boolean = true,
                                           private val disableSameSiteOriginPolicy: Boolean = false,
                                           private val displayWADL: Boolean = true,
                                           private val resources: List<Any> = listOf<Any>(),
                                           private val healthChecks: List<HealthCheckContext> = listOf<HealthCheckContext>(),
                                           private val staticDirectories: List<StaticResource> = listOf<StaticResource>()
    ) :Application<T>() {
    init {
        if (serverRootPath != null) System.setProperty("dw.server.rootPath", serverRootPath)

        if (keystore != null) {
            System.setProperty("javax.net.ssl.trustStore", keystore.location.absolutePath);
            System.setProperty("javax.net.ssl.trustStorePassword", keystore.password);
        }

        if (disableSslValidation) {
            val trustManagers = arrayOf<BlindTrustManager>(BlindTrustManager())
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustManagers, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(AllHostsValid());
        }
    }

    fun start() {
        run( *buildRuntimeArgs() )
    }

    override fun initialize( bootstrap : Bootstrap<T>){
        super.initialize( bootstrap )

        //the current release of the sagger API is broken. Again.
        //if (!swaggerPackages.isEmpty()) {
        //    bootstrap.addBundle(ConfiguredSwaggerBundle(swaggerPackages))
        //}
    }

    override fun run(config: T, env: Environment) {
        if (jsonPrettyPrint) {
            val mapper = env.objectMapper
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
        }

        if (displayWADL) {
            val properties = mutableMapOf<String, Any>(ServerProperties.WADL_FEATURE_DISABLE to false)
            env.jersey().resourceConfig.addProperties(properties)
        }

        if (disableSameSiteOriginPolicy){
            val filter = env.servlets().addFilter("CORS", CrossOriginFilter::class.java)
            filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")
            filter.setInitParameter("allowedOrigins", "*")    // allowed origins comma separated
            filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin")
            filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS")
            filter.setInitParameter("preflightMaxAge", "5184000") // 2 months
            filter.setInitParameter("allowCredentials", "true")
        }

        for (resource in resources) {
            env.jersey().register(resource)
        }

        for ( (context, healthCheck) in healthChecks ) {
            env.healthChecks().register(context, healthCheck)
        }
    }

    private fun buildRuntimeArgs(): Array<String> {
        if (configFile == null) return arrayOf("server") else return arrayOf("server", configFile.absolutePath)
    }
}

data class HealthCheckContext(val context: String, val healthCheck: HealthCheck)

data class StaticResource(val location: String, val context: String = "", val index: String = "index.html", val uuid: String = UUID.randomUUID().toString())

data class Keystore(val location: File, val password: String)

class BlindTrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? { return null }

    override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}

    override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
}

class AllHostsValid : HostnameVerifier {
    override fun verify(hostname: String, session: SSLSession): Boolean { return true }
}

class ConfiguredSwaggerBundle<T: Configuration>(val swaggerPackages: List<String>): SwaggerBundle<T>() {
    private val swaggerConfig: SwaggerBundleConfiguration = SwaggerBundleConfiguration();

    init{
        val packageNames = swaggerPackages.joinToString()
        swaggerConfig.resourcePackage = packageNames
    }

    override fun getSwaggerBundleConfiguration(p0: T): SwaggerBundleConfiguration {
        return swaggerConfig
    }

}