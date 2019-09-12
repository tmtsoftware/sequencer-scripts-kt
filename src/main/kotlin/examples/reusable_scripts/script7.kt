package examples.reusable_scripts

import csw.params.commands.CommandResponse.Completed
import esw.ocs.dsl.core.reusableScript
import examples.eventKey

val script7 = reusableScript {
    log("============= Loading script 7 ============")

    handleSetup("command-2") { command ->
        log("============ command-2 ================")
        val events = getEvent(eventKey + 1)
        log(events.toString())
        events.forEach(::println)

        log("============ command-2 End ================")
        addOrUpdateCommand(Completed(command.runId()))
    }
}
