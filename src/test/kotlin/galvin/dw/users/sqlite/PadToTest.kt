package galvin.dw.users.sqlite

import galvin.dw.PadTo.padTo
import galvin.dw.PadTo.paddedLayout
import org.junit.Assert
import org.junit.Test


class PadToTest{
    @Test
    @Throws(Exception::class)
    fun testPadTo() {
        verifyPadTo("Hello", 10, "Hello     ")
        verifyPadTo("Hello", "Hello".length, "Hello")
        verifyPadTo("hgjtlinfdthelidofnht", 5, "hgjtlinfdthelidofnht")
        verifyPadTo(null, 7, "       ")

        var expected = StringBuilder("")
        for (i in 0..99) {
            verifyPadTo("", i, expected.toString())
            expected.append(" ")
        }

        val foobar = "FooBar!"
        for (i in 0..foobar.length) {
            verifyPadTo(foobar, i, foobar)
        }

        expected = StringBuilder(foobar)
        for (i in foobar.length + 1..99) {
            expected.append(" ")
            verifyPadTo(foobar, i, expected.toString())
        }
    }

    @Throws(Exception::class)
    private fun verifyPadTo(text: String?, padTo: Int, expected: String) {
        val result = padTo(text, padTo)
        Assert.assertEquals("Unexpected string", expected, result)
    }

    ///

    @Test
    @Throws(Exception::class)
    fun testPaddedTable() {

        val PADDED_TABLE =
                "asdas         adasdkasdjkasjdkasdas asdasd          adsadsda      adasdadsasd dasdasdasd \n" +
                "asdas         adsasdadas            adasdasdasdadas adsdasd       asdasdads   adsadasd   \n" +
                "adsdasasdad   asdasdadas            asdadasdasd     adsdasdasdasd asdadasdads adsasdadads\n" +
                "asddas        ads                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         asd        \n" +
                "asdadasdadasd asdadasdasdasd        adsdaasdadas    asdasdadasd   adsadsdasad asdasdadada\n" +
                "gdgdgdgf      dgfdfgdgfdgf          dgfdgdfgdg      dfgdfgdgdgf   dfggfdg     dfgdggd    \n" +
                "dgfdfgdgfg    dgdggdgfdgdgf         dgdfgdggd       dfgdgdgdf     dgfdfggd    dgfgdgf    \n" +
                "dfgdgfdgd     dfgdfgdgdg            dggdgdfdgf      dgdgdf        dgdfgdf     dgdgdf     \n" +
                "dgf           dgf                   dgf             dgf           dgf         dgf        \n" +
                "1             2                     3               4             5           6          \n" +
                "a             b                     c               d             e           f          "

        val lists = mutableListOf<MutableList<String>>()
        for (i in 0..7) {
            lists.add( mutableListOf<String>() )
        }

        lists[0].add("asdas")
        lists[0].add("asdas")
        lists[0].add("adsdasasdad")
        lists[0].add("asddas")
        lists[0].add("asd")
        lists[0].add("asd")
        lists[0].add("asdadasdadasd")
        lists[0].add("gdgdgdgf")
        lists[0].add("dgfdfgdgfg")
        lists[0].add("dfgdgfdgd")
        lists[0].add("dgf")
        lists[0].add("1")
        lists[0].add("a")

        lists[1].add("adasdkasdjkasjdkasdas")
        lists[1].add("adsasdadas")
        lists[1].add("asdasdadas")
        lists[1].add("ads")
        lists[1].add("asd")
        lists[1].add("asd")
        lists[1].add("asdadasdasdasd")
        lists[1].add("dgfdfgdgfdgf")
        lists[1].add("dgdggdgfdgdgf")
        lists[1].add("dfgdfgdgdg")
        lists[1].add("dgf")
        lists[1].add("2")
        lists[1].add("b")

        lists[2].add("asdasd")
        lists[2].add("adasdasdasdadas")
        lists[2].add("asdadasdasd")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("adsdaasdadas")
        lists[2].add("dgfdgdfgdg")
        lists[2].add("dgdfgdggd")
        lists[2].add("dggdgdfdgf")
        lists[2].add("dgf")
        lists[2].add("3")
        lists[2].add("c")

        lists[3].add("adsadsda")
        lists[3].add("adsdasd")
        lists[3].add("adsdasdasdasd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asdasdadasd")
        lists[3].add("dfgdfgdgdgf")
        lists[3].add("dfgdgdgdf")
        lists[3].add("dgdgdf")
        lists[3].add("dgf")
        lists[3].add("4")
        lists[3].add("d")

        lists[4].add("adasdadsasd")
        lists[4].add("asdasdads")
        lists[4].add("asdadasdads")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("adsadsdasad")
        lists[4].add("dfggfdg")
        lists[4].add("dgfdfggd")
        lists[4].add("dgdfgdf")
        lists[4].add("dgf")
        lists[4].add("5")
        lists[4].add("e")

        lists[5].add("dasdasdasd")
        lists[5].add("adsadasd")
        lists[5].add("adsasdadads")
        lists[5].add("ads")
        lists[5].add("ads")
        lists[5].add("asd")
        lists[5].add("asdasdadada")
        lists[5].add("dfgdggd")
        lists[5].add("dgfgdgf")
        lists[5].add("dgdgdf")
        lists[5].add("dgf")
        lists[5].add("6")
        lists[5].add("f")

        val padded = paddedLayout( lists[0], lists[1], lists[2], lists[3], lists[4], lists[5] )
        Assert.assertEquals("Padded table layout failed", PADDED_TABLE, padded)
    }

    @Test
    @Throws(Exception::class)
    fun testPaddedTableTwo() {
        val PADDED_TABLE =
                "asdas         adasdkasdjkasjdkasdas asdasd          adsadsda      adasdadsasd dasdasdasd \n" +
                "asdas         adsasdadas            adasdasdasdadas adsdasd       asdasdads   adsadasd   \n" +
                "adsdasasdad   asdasdadas            asdadasdasd     adsdasdasdasd asdadasdads adsasdadads\n" +
                "asddas        ads                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         asd        \n" +
                "asdadasdadasd asdadasdasdasd        adsdaasdadas    asdasdadasd   adsadsdasad asdasdadada\n" +
                "gdgdgdgf      dgfdfgdgfdgf          dgfdgdfgdg      dfgdfgdgdgf   dfggfdg     dfgdggd    \n" +
                "dgfdfgdgfg    dgdggdgfdgdgf         dgdfgdggd       dfgdgdgdf     dgfdfggd    dgfgdgf    \n" +
                "dfgdgfdgd     dfgdfgdgdg            dggdgdfdgf      dgdgdf        dgdfgdf     dgdgdf     \n" +
                "dgf           dgf                   dgf             dgf           dgf         dgf        \n" +
                "1             2                     3               4             5           6          \n" +
                "a             b                     c               d             e           f          \n" +
                "              x                                     xxxxxxxxx                 xxxxxxxxxx \n" +
                "              zzzzz zzz                                                                  "

        val lists = mutableListOf<MutableList<String>>()
        for (i in 0..6) {
            lists.add( mutableListOf<String>() )
        }

        lists[0].add("asdas")
        lists[0].add("asdas")
        lists[0].add("adsdasasdad")
        lists[0].add("asddas")
        lists[0].add("asd")
        lists[0].add("asd")
        lists[0].add("asdadasdadasd")
        lists[0].add("gdgdgdgf")
        lists[0].add("dgfdfgdgfg")
        lists[0].add("dfgdgfdgd")
        lists[0].add("dgf")
        lists[0].add("1")
        lists[0].add("a")

        lists[1].add("adasdkasdjkasjdkasdas")
        lists[1].add("adsasdadas")
        lists[1].add("asdasdadas")
        lists[1].add("ads")
        lists[1].add("asd")
        lists[1].add("asd")
        lists[1].add("asdadasdasdasd")
        lists[1].add("dgfdfgdgfdgf")
        lists[1].add("dgdggdgfdgdgf")
        lists[1].add("dfgdfgdgdg")
        lists[1].add("dgf")
        lists[1].add("2")
        lists[1].add("b")
        lists[1].add("x")
        lists[1].add("zzzzz zzz")

        lists[2].add("asdasd")
        lists[2].add("adasdasdasdadas")
        lists[2].add("asdadasdasd")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("adsdaasdadas")
        lists[2].add("dgfdgdfgdg")
        lists[2].add("dgdfgdggd")
        lists[2].add("dggdgdfdgf")
        lists[2].add("dgf")
        lists[2].add("3")
        lists[2].add("c")

        lists[3].add("adsadsda")
        lists[3].add("adsdasd")
        lists[3].add("adsdasdasdasd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asdasdadasd")
        lists[3].add("dfgdfgdgdgf")
        lists[3].add("dfgdgdgdf")
        lists[3].add("dgdgdf")
        lists[3].add("dgf")
        lists[3].add("4")
        lists[3].add("d")
        lists[3].add("xxxxxxxxx")

        lists[4].add("adasdadsasd")
        lists[4].add("asdasdads")
        lists[4].add("asdadasdads")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("adsadsdasad")
        lists[4].add("dfggfdg")
        lists[4].add("dgfdfggd")
        lists[4].add("dgdfgdf")
        lists[4].add("dgf")
        lists[4].add("5")
        lists[4].add("e")

        lists[5].add("dasdasdasd")
        lists[5].add("adsadasd")
        lists[5].add("adsasdadads")
        lists[5].add("ads")
        lists[5].add("ads")
        lists[5].add("asd")
        lists[5].add("asdasdadada")
        lists[5].add("dfgdggd")
        lists[5].add("dgfgdgf")
        lists[5].add("dgdgdf")
        lists[5].add("dgf")
        lists[5].add("6")
        lists[5].add("f")
        lists[5].add("xxxxxxxxxx")

        val padded = paddedLayout( lists[0], lists[1], lists[2], lists[3], lists[4], lists[5] )
        Assert.assertEquals("Padded table layout failed", PADDED_TABLE, padded)
    }


    @Test
    @Throws(Exception::class)
    fun testPaddedTableHeader() {
        val PADDED_TABLE =
                "asdas         adasdkasdjkasjdkasdas asdasd          adsadsda      adasdadsasd dasdasdasd \n" +
                "-----------------------------------------------------------------------------------------\n" +
                "asdas         adsasdadas            adasdasdasdadas adsdasd       asdasdads   adsadasd   \n" +
                "adsdasasdad   asdasdadas            asdadasdasd     adsdasdasdasd asdadasdads adsasdadads\n" +
                "asddas        ads                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         asd        \n" +
                "asdadasdadasd asdadasdasdasd        adsdaasdadas    asdasdadasd   adsadsdasad asdasdadada\n" +
                "gdgdgdgf      dgfdfgdgfdgf          dgfdgdfgdg      dfgdfgdgdgf   dfggfdg     dfgdggd    \n" +
                "dgfdfgdgfg    dgdggdgfdgdgf         dgdfgdggd       dfgdgdgdf     dgfdfggd    dgfgdgf    \n" +
                "dfgdgfdgd     dfgdfgdgdg            dggdgdfdgf      dgdgdf        dgdfgdf     dgdgdf     \n" +
                "dgf           dgf                   dgf             dgf           dgf         dgf        \n" +
                "1             2                     3               4             5           6          \n" +
                "a             b                     c               d             e           f          "

        val lists = mutableListOf<MutableList<String>>()
        for (i in 0..6) {
            lists.add( mutableListOf<String>() )
        }

        lists[0].add("asdas")
        lists[0].add("asdas")
        lists[0].add("adsdasasdad")
        lists[0].add("asddas")
        lists[0].add("asd")
        lists[0].add("asd")
        lists[0].add("asdadasdadasd")
        lists[0].add("gdgdgdgf")
        lists[0].add("dgfdfgdgfg")
        lists[0].add("dfgdgfdgd")
        lists[0].add("dgf")
        lists[0].add("1")
        lists[0].add("a")

        lists[1].add("adasdkasdjkasjdkasdas")
        lists[1].add("adsasdadas")
        lists[1].add("asdasdadas")
        lists[1].add("ads")
        lists[1].add("asd")
        lists[1].add("asd")
        lists[1].add("asdadasdasdasd")
        lists[1].add("dgfdfgdgfdgf")
        lists[1].add("dgdggdgfdgdgf")
        lists[1].add("dfgdfgdgdg")
        lists[1].add("dgf")
        lists[1].add("2")
        lists[1].add("b")

        lists[2].add("asdasd")
        lists[2].add("adasdasdasdadas")
        lists[2].add("asdadasdasd")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("adsdaasdadas")
        lists[2].add("dgfdgdfgdg")
        lists[2].add("dgdfgdggd")
        lists[2].add("dggdgdfdgf")
        lists[2].add("dgf")
        lists[2].add("3")
        lists[2].add("c")

        lists[3].add("adsadsda")
        lists[3].add("adsdasd")
        lists[3].add("adsdasdasdasd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asdasdadasd")
        lists[3].add("dfgdfgdgdgf")
        lists[3].add("dfgdgdgdf")
        lists[3].add("dgdgdf")
        lists[3].add("dgf")
        lists[3].add("4")
        lists[3].add("d")

        lists[4].add("adasdadsasd")
        lists[4].add("asdasdads")
        lists[4].add("asdadasdads")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("adsadsdasad")
        lists[4].add("dfggfdg")
        lists[4].add("dgfdfggd")
        lists[4].add("dgdfgdf")
        lists[4].add("dgf")
        lists[4].add("5")
        lists[4].add("e")

        lists[5].add("dasdasdasd")
        lists[5].add("adsadasd")
        lists[5].add("adsasdadads")
        lists[5].add("ads")
        lists[5].add("ads")
        lists[5].add("asd")
        lists[5].add("asdasdadada")
        lists[5].add("dfgdggd")
        lists[5].add("dgfgdgf")
        lists[5].add("dgdgdf")
        lists[5].add("dgf")
        lists[5].add("6")
        lists[5].add("f")

        val padded = paddedLayout( '-', lists[0], lists[1], lists[2], lists[3], lists[4], lists[5] )
        Assert.assertEquals("Padded table layout failed", PADDED_TABLE, padded)
    }

    @Test
    @Throws(Exception::class)
    fun testPaddedTableHeaderTwo() {
        val PADDED_TABLE =
                "asdas         adasdkasdjkasjdkasdas asdasd          adsadsda      adasdadsasd dasdasdasd \n" +
                "=========================================================================================\n" +
                "asdas         adsasdadas            adasdasdasdadas adsdasd       asdasdads   adsadasd   \n" +
                "adsdasasdad   asdasdadas            asdadasdasd     adsdasdasdasd asdadasdads adsasdadads\n" +
                "asddas        ads                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         ads        \n" +
                "asd           asd                   ads             asd           asd         asd        \n" +
                "asdadasdadasd asdadasdasdasd        adsdaasdadas    asdasdadasd   adsadsdasad asdasdadada\n" +
                "gdgdgdgf      dgfdfgdgfdgf          dgfdgdfgdg      dfgdfgdgdgf   dfggfdg     dfgdggd    \n" +
                "dgfdfgdgfg    dgdggdgfdgdgf         dgdfgdggd       dfgdgdgdf     dgfdfggd    dgfgdgf    \n" +
                "dfgdgfdgd     dfgdfgdgdg            dggdgdfdgf      dgdgdf        dgdfgdf     dgdgdf     \n" +
                "dgf           dgf                   dgf             dgf           dgf         dgf        \n" +
                "1             2                     3               4             5           6          \n" +
                "a             b                     c               d             e           f          \n" +
                "              x                                     xxxxxxxxx                 xxxxxxxxxx \n" +
                "              zzzzz zzz                                                                  "

        val lists = mutableListOf<MutableList<String>>()
        for (i in 0..6) {
            lists.add( mutableListOf<String>() )
        }

        lists[0].add("asdas")
        lists[0].add("asdas")
        lists[0].add("adsdasasdad")
        lists[0].add("asddas")
        lists[0].add("asd")
        lists[0].add("asd")
        lists[0].add("asdadasdadasd")
        lists[0].add("gdgdgdgf")
        lists[0].add("dgfdfgdgfg")
        lists[0].add("dfgdgfdgd")
        lists[0].add("dgf")
        lists[0].add("1")
        lists[0].add("a")

        lists[1].add("adasdkasdjkasjdkasdas")
        lists[1].add("adsasdadas")
        lists[1].add("asdasdadas")
        lists[1].add("ads")
        lists[1].add("asd")
        lists[1].add("asd")
        lists[1].add("asdadasdasdasd")
        lists[1].add("dgfdfgdgfdgf")
        lists[1].add("dgdggdgfdgdgf")
        lists[1].add("dfgdfgdgdg")
        lists[1].add("dgf")
        lists[1].add("2")
        lists[1].add("b")
        lists[1].add("x")
        lists[1].add("zzzzz zzz")

        lists[2].add("asdasd")
        lists[2].add("adasdasdasdadas")
        lists[2].add("asdadasdasd")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("ads")
        lists[2].add("adsdaasdadas")
        lists[2].add("dgfdgdfgdg")
        lists[2].add("dgdfgdggd")
        lists[2].add("dggdgdfdgf")
        lists[2].add("dgf")
        lists[2].add("3")
        lists[2].add("c")

        lists[3].add("adsadsda")
        lists[3].add("adsdasd")
        lists[3].add("adsdasdasdasd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asd")
        lists[3].add("asdasdadasd")
        lists[3].add("dfgdfgdgdgf")
        lists[3].add("dfgdgdgdf")
        lists[3].add("dgdgdf")
        lists[3].add("dgf")
        lists[3].add("4")
        lists[3].add("d")
        lists[3].add("xxxxxxxxx")

        lists[4].add("adasdadsasd")
        lists[4].add("asdasdads")
        lists[4].add("asdadasdads")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("asd")
        lists[4].add("adsadsdasad")
        lists[4].add("dfggfdg")
        lists[4].add("dgfdfggd")
        lists[4].add("dgdfgdf")
        lists[4].add("dgf")
        lists[4].add("5")
        lists[4].add("e")

        lists[5].add("dasdasdasd")
        lists[5].add("adsadasd")
        lists[5].add("adsasdadads")
        lists[5].add("ads")
        lists[5].add("ads")
        lists[5].add("asd")
        lists[5].add("asdasdadada")
        lists[5].add("dfgdggd")
        lists[5].add("dgfgdgf")
        lists[5].add("dgdgdf")
        lists[5].add("dgf")
        lists[5].add("6")
        lists[5].add("f")
        lists[5].add("xxxxxxxxxx")

        val padded = paddedLayout( '=', lists[0], lists[1], lists[2], lists[3], lists[4], lists[5] )
        Assert.assertEquals("Padded table layout failed", PADDED_TABLE, padded)
    }
}