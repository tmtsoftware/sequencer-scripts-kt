package aoesw

import esw.ocs.dsl.core.script
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.floatKey
import esw.ocs.dsl.params.taiTimeKey
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture


script {
    val prefix = "aoesw.aosq"
    val aoeswOffsetTime = taiTimeKey(name = "scheduledTime")
    val aoeswOffsetXKey = floatKey("x")
    val aoeswOffsetYKey = floatKey("y")

    val probeOffsetXKey = floatKey("x")
    val probeOffsetYKey = floatKey("y")

    handleSetup("offset") { command ->
        val scheduledTime = command[aoeswOffsetTime.key].get()
        val offsetX = command[aoeswOffsetXKey.key].get()
        val offsetY = command[aoeswOffsetYKey.key].get()

        val probeCommand =
                setup(prefix, "scheduledOffset", command.jMaybeObsId().map { it.obsId() }.nullable())
                        .add(probeOffsetXKey.set(offsetX.jGet(0).get()))
                        .add(probeOffsetYKey.set(offsetY.jGet(0).get()))

        addSubCommand(command, probeCommand)
        scheduleOnce(scheduledTime.jGet(0).get()) {
            // fixme: this block should be suspendable
            val response = async { submitCommandToAssembly("probeAssembly", probeCommand) }
            response.asCompletableFuture().thenAccept {
                updateSubCommand(it)
            }

        }
    }
}