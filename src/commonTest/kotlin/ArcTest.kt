import org.wysko.kmidi.midi.StandardMidiFileReader
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import kotlin.test.Test

class ArcTest {
    @Test
    fun `Test arcs from notes`() {
        val smf = StandardMidiFileReader.readByteArray(SmfExamples.example1)
        val sequence = smf.toTimeBasedSequence()
        val arcs = sequence.convertArcsToTimedArcs(smf.tracks.single().arcs)

        assert(arcs.size == 4)
    }
}
