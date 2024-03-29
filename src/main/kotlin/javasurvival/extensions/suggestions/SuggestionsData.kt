package javasurvival.extensions.suggestions

import dev.kord.common.entity.Snowflake

interface SuggestionsData {
    fun get(id: String): Suggestion?
    fun get(id: Snowflake): Suggestion? = get(id.toString())

    fun add(id: String, suggestion: Suggestion): Boolean
    fun add(id: Snowflake, suggestion: Suggestion): Boolean = add(id.toString(), suggestion)

    fun load(): Int

    fun save(): Boolean
    fun save(id: String): Boolean
    fun save(suggestion: Suggestion): Boolean
}
