package com.javasurvival.config.spec

import com.uchuhimo.konf.ConfigSpec

object BotSpec : ConfigSpec() {
    val token by required<String>()
    val guild by required<Long>()
}
