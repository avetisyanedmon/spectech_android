package com.spectech.domain.util

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RegionCitiesTest {

    @Test
    fun `every region in the picker has city suggestions`() {
        // The pickers commit full official names from RussianRegions.all and
        // RegionCities looks entries up by exact match — a key drifting out of
        // sync silently empties the city picker for that region.
        val missing = RussianRegions.all.filter { RegionCities.topCities(it).isEmpty() }
        missing.shouldBeEmpty()
    }

    @Test
    fun `every city map key is a known region`() {
        val unknown = RegionCities.knownRegions.filterNot { it in RussianRegions.all }
        unknown.shouldBeEmpty()
    }

    @Test
    fun `suggestions are tagged with their region as subtitle`() {
        val suggestions = RegionCities.topCities("Краснодарский край")
        suggestions.shouldNotBeEmpty()
        suggestions.forEach { it.subtitle shouldBe "Краснодарский край" }
    }

    @Test
    fun `returns empty list for unknown region`() {
        RegionCities.topCities("Атлантида").shouldBeEmpty()
    }

    @Test
    fun `returns empty list when no regions selected`() {
        RegionCities.topCities(emptySet()).shouldBeEmpty()
    }

    @Test
    fun `southern regions include their major stanitsas`() {
        RegionCities.topCities("Краснодарский край").map { it.name } shouldContain "Каневская"
        RegionCities.topCities("Ростовская область").map { it.name } shouldContain "Вёшенская"
        RegionCities.topCities("Ставропольский край").map { it.name } shouldContain "Ессентукская"
    }

    @Test
    fun `multi-region lookup deduplicates repeating city names`() {
        // "Киров" exists in both Калужская and Кировская областях.
        val names = RegionCities.topCities(setOf("Калужская область", "Кировская область"))
            .map { it.name.lowercase() }
        names.count { it == "киров" } shouldBe 1
    }

    @Test
    fun `federal cities map to themselves`() {
        RegionCities.topCities("Москва").map { it.name } shouldContain "Москва"
        RegionCities.topCities("Санкт-Петербург").map { it.name } shouldContain "Санкт-Петербург"
        RegionCities.topCities("Севастополь").map { it.name } shouldContain "Севастополь"
    }
}
