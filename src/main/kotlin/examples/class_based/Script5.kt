package examples.class_based

import csw.params.commands.CommandResponse.Completed
import esw.ocs.dsl.core.ScriptKt
import esw.ocs.impl.dsl.CswServices
import examples.eventKey
import examples.reusable_scripts.script6
import examples.reusable_scripts.script7
import kotlinx.coroutines.delay

@Deprecated("Use script based approach to write scripts")
class Script5(cswServices: CswServices) : ScriptKt(cswServices) {
    init {

        log("============= Loading script 5 ============")

        var totalEventsRec = 0

        loadScripts(
            script6,
            script7
        )

        handleSetup("command-3") { command ->
            log("============ command-3 ================")

            val keys = (0.until(50)).map { eventKey + it }.toTypedArray()

            onEvent(*keys) { event ->
                println("=======================")
                log("Received: ${event.eventName()}")
                totalEventsRec += 1
            }

            log("============ command-3 End ================")
            addOrUpdateCommand(Completed(command.runId()))
        }

        handleShutdown {
            while (totalEventsRec <= 49) {
                delay(100)
            }
            close()
        }

    }

}