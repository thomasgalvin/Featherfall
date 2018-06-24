package galvin.ff.db

import java.io.Closeable
import java.sql.Connection
import java.sql.Statement

object QuietCloser{
    fun close( vararg closeables: Closeable? ){
        for( closeable in closeables ){
            if( closeable != null && !isClosed(closeable) ){
                try{
                    closeable.close()
                } catch( ignored: Throwable ){}
            }
        }
    }

    fun close( vararg closeables: AutoCloseable? ){
        for( closeable in closeables ){
            if( closeable != null && !isClosed(closeable) ){
                try{
                    closeable.close()
                } catch( ignored: Throwable ){}
            }
        }
    }

    fun isClosed( target: Any? ): Boolean{
        if( target == null ) return true

        if( target is Connection ){
            return target.isClosed
        }

        if( target is Statement ){
            return target.isClosed
        }

        return false
    }
}