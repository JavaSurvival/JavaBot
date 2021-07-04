package javasurvival.config.spec

import com.uchuhimo.konf.ConfigSpec

object ChannelsSpec : ConfigSpec() {
    val logs by required<Long>()
    val modLogs by required<Long>()
    val suggestions by required<Long>()
}
