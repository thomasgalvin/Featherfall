package galvin.dw

import io.dropwizard.Application
import java.io.File
import java.util.ArrayList
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import org.glassfish.jersey.server.ServerProperties
import javax.net.ssl.*


class ServiceBuilder<T: Configuration> {
    private var configFile: File? = null
    private var serverRootPath: String? = null;
    private var keystore: Keystore? = null
    private var disableSslValidation: Boolean = false
    private var displayWADL: Boolean = false
    private var swagger: SwaggerBundleConfiguration? = null

    private val resources = ArrayList<Any>()
    private val healthChecks = ArrayList<HealthCheck>()
    private val staticDirectories = ArrayList<StaticResource>()

    /**
     * Sets the location of the main Dropwizard config file.
     */
    fun withConfigFile(configFile: File) {
        this.configFile = configFile
    }

    /**
     * Sets the context root for the REST API. By default, the API will be
     * hosted at `/`; this method allows you to set it to `/api/` for example.
     */
    fun withServerRootPath(path: String){
        this.serverRootPath = path
    }

    fun withKeystor(keystore: Keystore){
        this.keystore = keystore
    }

    /**
     * Allows Dropwizard to use self-signed certificates when negotiating SSL/HTTPS.
     */
    fun disableSllValidation(){
        this.disableSslValidation = true
    }

    /**
     * Instructs Dropwizard to create a WADL for the REST API.
     */
    fun displayWADL(){
        this.displayWADL = true
    }

    /**
     * Configures Dropwizard to use Swagger to automatically document your REST API.
     */
    fun withSwagger(config: SwaggerBundleConfiguration){
        this.swagger = config
    }

    /**
     * Adds a resource to the Dropwizard REST API.
     */
    fun withResource(resource: Any) {
        resources.add(resource)
    }

    /**
     * Adds a health check to Dropwizard. This should be a lightweight test that makes sure
     * the API is running as expected.
     */
    fun withHealthCheck(healthCheck: HealthCheck) {
        healthChecks.add(healthCheck)
    }

    /**
     * Adds a directory with static content to Dropwizard. This can be used to serve
     * images, web apps, and other static media.
     */
    fun withStaticDirectory( resource: StaticResource ){
        this.staticDirectories.add(resource)
    }

    private fun build(): Server<T>{
        val srp = serverRootPath
        if( srp != null ) System.setProperty( "dw.server.rootPath", srp )

        val ks = keystore;
        if( ks != null ){
            System.setProperty( "javax.net.ssl.trustStore", ks.location.absolutePath );
            System.setProperty( "javax.net.ssl.trustStorePassword", ks.password );
        }

        if(disableSslValidation){
            val trustManagers = arrayOf<BlindTrustManager>( BlindTrustManager() )
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustManagers, java.security.SecureRandom() )
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier( AllHostsValid() );
        }

        val server = Server<T>(displayWADL)


        return server
    }

    private fun buildRuntimeArgs(): Array<String>{
        var cf = configFile
        if(cf == null) return arrayOf("server") else return arrayOf("server", cf.absolutePath )
    }
}

data class HealthCheck( val context: String, val healthCheck: Any)

data class StaticResource( val directory: File, val index: String = "index.html")

data class Keystore( val location: File, val password: String)

class Server<T: Configuration>( val displayWADL: Boolean): Application<T>() {
    override fun run( config: T, env: Environment ){
        if( displayWADL ){
            val properties = mutableMapOf<String, Any>( ServerProperties.WADL_FEATURE_DISABLE to false )
            env.jersey().resourceConfig.addProperties(properties)
        }
    }
}

class BlindTrustManager: X509TrustManager {
    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? {
        return null
    }

    override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?){
    }

    override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?){
    }
}

class AllHostsValid: HostnameVerifier{
    override fun verify(hostname: String, session: SSLSession): Boolean {
        return true
    }
}