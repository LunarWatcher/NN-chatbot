import org.joda.time.DateTimeZone
import org.junit.Test

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

    @Test
    fun typeCast(){
        val x: String = "124321";
        val y: Long = 84189

        val z = x.toLong()

    }
}