package galvin.dw.tools

import galvin.dw.isBlank
import galvin.dw.neverNull
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option

data class Opt( val short: String,
                val long: String = "",
                val desc: String,
                val required: Boolean = false,
                val argName: String = "" ){
    fun build(): Option{
        val builder = Option.builder( short ).required(required)

        if( !isBlank(long) ){ builder.longOpt(long) }
        if( !isBlank(desc) ){ builder.desc(desc) }

        if( !isBlank(argName) ){
            builder.hasArg().argName(argName)
        }

        return builder.build()
    }

    fun on( cmd: CommandLine ): Boolean{
        return cmd.hasOption( short ) || cmd.hasOption( long )
    }

    fun get( cmd: CommandLine ): String{
        return neverNull( cmd.getOptionValue(short) )
    }
}