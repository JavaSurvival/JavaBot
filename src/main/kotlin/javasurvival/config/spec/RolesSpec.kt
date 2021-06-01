package javasurvival.config.spec

import com.uchuhimo.konf.ConfigSpec

object RolesSpec : ConfigSpec() {
    val mod by required<Long>()
    val heHim by required<Long>()
    val heThey by required<Long>()
    val sheHer by required<Long>()
    val sheThey by required<Long>()
    val theyThem by required<Long>()
    val itThey by required<Long>()
    val announcements by required<Long>()
    val events by required<Long>()
    val minecraft by required<Long>()
}
