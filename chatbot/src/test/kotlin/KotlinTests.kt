import org.junit.Test
import java.util.Arrays



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
    fun repro(){


        val r = Arrays.stream(intArrayOf(1, 2, 3)).mapToObj { i ->
            println(i)
            Integer.toString(i)
        }.toArray()
    }
}