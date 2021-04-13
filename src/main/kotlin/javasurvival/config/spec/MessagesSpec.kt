package javasurvival.config.spec

import com.uchuhimo.konf.ConfigSpec

object MessagesSpec : ConfigSpec() {
    val reaction by required<Long>()
}
