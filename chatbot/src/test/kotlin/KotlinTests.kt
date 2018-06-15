@file:Suppress("NAME_SHADOWING")

import io.github.lunarwatcher.chatbot.bot.Bot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.joda.time.DateTimeZone
import org.junit.Test
import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte1.other
import kotlin.math.exp
import kotlin.math.pow

class KotlinTests {

    @Test
    fun getRevision(){
        val command = "git rev-parse HEAD"
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
        val output = process.inputStream.bufferedReader().readLine()!!
        println(output)
    }

    @Test
    fun timezones(){
        val dateTime = DateTimeZone.getAvailableIDs()
        println(dateTime)
        DateTimeZone.forID("Europe/Rome")//This is the main test; if it throws an exception, the test has failed
    }

}

