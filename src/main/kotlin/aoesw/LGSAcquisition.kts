package aoesw

import csw.params.commands.CommandResponse
import csw.params.commands.Sequence
import csw.params.core.models.Choice
import csw.params.events.SystemEvent
import csw.params.javadsl.JUnits.NoUnits
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.*
import esw.ocs.impl.dsl.StopIf
import kotlin.math.sqrt
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

object aosq {
    val prefix = "aoesw.aosq"
    val name = "ao-sequencer"
}

object oiwfsPoaAssembly {
    val prefix = "iris.oiwfs.poa"
    val name = "oiwfs-poa-assembly"
}

object oiwfsDetectorAssembly {
    val prefix = "iris.oiwfs.detector"
    val name = "oiwfs-detector-assembly"
}

object rtcAssembly {
    val prefix = "nfiraos.rtc"
    val name = "nfiraos-rtc-assembly"
}

@UseExperimental(ExperimentalTime::class)
script {
    val oiwfsExposureModeChoices = choicesOf("SINGLE", "CONTINUOUS", "STOP", "NOOP")
    val oiwfsExposureModeKey = choiceKey("mode", oiwfsExposureModeChoices)

    val oiwfsStateEvent = eventKey(rtcAssembly.prefix, "oiwfsState")
    val oiwfsStateEnableChoices = choicesOf("NONE", "TT", "TTF")
    val oiwfsStateEnableKey = choiceKey("enable", oiwfsStateEnableChoices)
    val oiwfsStateFluxHighKey = booleanKey("fluxHigh")
    val oiwfsStateFluxLowKey = booleanKey("fluxlow")

    val ttfOffsetEvent = eventKey(rtcAssembly.prefix, "telOffloadTt") // ??
    val ttfOffsetXKey = floatKey("x")
    val ttfOffsetYKey = floatKey("y")

    val tcsOffsetCoordinateSystemChoices = choicesOf("RADEC", "XY", "ALTAZ")
    val tcsOffsetCoordSystemKey = choiceKey("coordinateSystem", tcsOffsetCoordinateSystemChoices)
    val tcsOffsetXKey = floatKey("x")
    val tcsOffsetYKey = floatKey("y")
    val tcsOffsetVirtualTelescopeChoices =
            choicesOf("MOUNT", "OIWFS1", "OIWFS2", "OIWFS3", "OIWFS4", "ODGW1", "ODGW2", "ODGW3", "ODGW4", "GUIDER1", "GUIDER2")
    val tcsOffsetVTKey = choiceKey("virtualTelescope", tcsOffsetVirtualTelescopeChoices)

    val loopeventKey = eventKey(rtcAssembly.prefix, "loop")
    val oiwfsLoopStatesChoices = choicesOf("IDLE", "LOST", "ACTIVE")
    val oiwfsLoopKey = choiceKey("oiwfsPoa", oiwfsLoopStatesChoices)

    fun handleOiwfsLoopOpen(oiwfsProbeNum: Int) {
        // Do something
    }

    onEvent(loopeventKey.key()) { event ->
        when (event) {
            is SystemEvent -> {
                val oiwfsLoopStates = event(oiwfsLoopKey)
                val ii = oiwfsLoopStates.jValues().indexOf(Choice("LOST"))
                if (ii != -1) handleOiwfsLoopOpen(ii)
            }
        }
    }

    val TCSOFFSETTHRESHOLD = 2.0 // arcsec ???
    fun isOffsetRequired(x: Float, y: Float): Boolean = sqrt(x * x + y * y) > TCSOFFSETTHRESHOLD

    fun increaseExposureTime() {
        // not sure how this is done
    }

    // fixme: should return SubmitResponse
    suspend fun offsetTcs(xoffset: Float, yoffset: Float, probeNum: Int, obsId: String?) =
            submitSequence("tcs", "darknight",
                    // fixme: provide better api in script-dsl
                    Sequence.apply(
                            setup(aosq.prefix, "offset", obsId)
                                    .add(tcsOffsetCoordSystemKey.set(arrayOf(Choice("RADEC")), NoUnits))
                                    .add(tcsOffsetXKey.set(xoffset))
                                    .add(tcsOffsetYKey.set(yoffset))
                                    .add(tcsOffsetVTKey.set(arrayOf(Choice("OIWFS$probeNum")), NoUnits)), null
                    )
            )

    handleSetup("enableOiwfsTtf") { command ->
        val ttfProbeNum = when (val event = getEvent(oiwfsStateEvent.key()).first()) {
            is SystemEvent -> event(oiwfsStateEnableKey).jValues().indexOf(Choice("TTF"))
            else -> -1
        }

        var ttfFluxHigh = false
        var ttfFluxLow = false
        var xoffset = 0.0f
        var yoffset = 0.0f

        val subscription = onEvent(oiwfsStateEvent.key(), ttfOffsetEvent.key()) { event ->
            when {
                event is SystemEvent && event.eventName() == oiwfsStateEvent.eventName() && ttfProbeNum != -1 -> {
                    ttfFluxHigh = event(oiwfsStateFluxHighKey).value(ttfProbeNum)
                    ttfFluxLow = event(oiwfsStateFluxLowKey).value(ttfProbeNum)
                }
                event is SystemEvent && event.eventName() == ttfOffsetEvent.eventName() -> {
                    xoffset = event(ttfOffsetXKey)(0)
                    yoffset = event(ttfOffsetYKey)(0)
                }
            }
        }

        // start continuous exposures on TTF probe
        val probeExpModes = (0..2).map { if (it == ttfProbeNum) Choice("CONTINUOUS") else Choice("NOOP") }
        val startExposureCommand = setup(aosq.prefix, "exposure", command.obsId)
                .add(oiwfsExposureModeKey.set(probeExpModes.toTypedArray(), NoUnits))

        val response = submitAndWaitCommandToAssembly(oiwfsDetectorAssembly.name, startExposureCommand)

        when (response) {
            is CommandResponse.Completed -> {
                val guideStarLockedThreshold = 5 // number of consecutive loops without an offset to consider stable
                var timesGuideStarLocked: Int = 0
                val maxAttempts = 20 // maximum number of loops on this guide star before rejecting
                var attempts = 0

                loop(500.milliseconds) {
                    when {
                        ttfFluxLow -> increaseExposureTime() // period tbd
                        isOffsetRequired(xoffset, yoffset) -> {
                            val offsetResponse = offsetTcs(xoffset, yoffset, ttfProbeNum, command.obsId)
                            timesGuideStarLocked = 0
                        }
                        else -> timesGuideStarLocked += 1
                    }

                    attempts += 1
                    StopIf((timesGuideStarLocked == guideStarLockedThreshold) || (attempts == maxAttempts))
                }

                if (timesGuideStarLocked == guideStarLockedThreshold) addOrUpdateCommand(CommandResponse.Completed(command.runId()))
                else addOrUpdateCommand(CommandResponse.Error(command.runId(), "Guide Star Unstable"))
            }
            else -> addOrUpdateCommand(CommandResponse.Error(command.runId(), "Error starting WFS exposures: $response"))
        }
        subscription.unsubscribe()
    }
}
