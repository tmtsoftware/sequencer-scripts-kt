package examples

import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent

const val eventKey = "csw.a.b."
fun event(id: Int) = SystemEvent(Prefix("csw.a.b"), EventName(id.toString()))