package javasurvival.config.spec

import com.uchuhimo.konf.ConfigSpec

object EmojiSpec : ConfigSpec() {
    val event by required<String>()
    val announcement by required<String>()
}
