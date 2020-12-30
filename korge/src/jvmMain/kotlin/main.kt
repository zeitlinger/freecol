import com.soywiz.korge.Korge
import net.sf.freecol.FreeCol
import org.freecol.globalClient
import org.freecol.start

suspend fun main() {
    Thread { FreeCol.main(arrayOf("--no-sound", "--fast")) }.run()
    while (globalClient == null) {
        Thread.sleep(100)
    }

    Korge(width = 900, height = 512) {
        start()
    }
}
