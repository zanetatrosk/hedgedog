package com.example.bedanceapp.specification

import com.example.bedanceapp.model.*
import jakarta.persistence.criteria.*
import org.springframework.data.jpa.domain.Specification
import java.util.UUID

object EventSpecification {

    fun hasStatus(status: String): Specification<Event> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get<String>("status"), status)
        }
    }

    fun hasEventName(eventName: String?): Specification<Event>? {
        return eventName?.takeIf { it.isNotBlank() }?.let { searchTerm ->
            Specification { root, _, criteriaBuilder ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("eventName")),
                    "%${searchTerm.trim().lowercase()}%"
                )
            }
        }
    }

    fun hasLocationFilters(
        city: String?,
        country: String?
    ): Specification<Event>? {
        // Only create specification if at least one filter is provided
        if (city == null && country == null) {
            return null
        }

        return Specification { root, _, criteriaBuilder ->
            val location = root.join<Event, Location>("location", JoinType.LEFT)
            val predicates = mutableListOf<Predicate>()

            city?.let {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(location.get("city")),
                        "%${it.lowercase()}%"
                    )
                )
            }

            country?.let {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(location.get("country")),
                        "%${it.lowercase()}%"
                    )
                )
            }

            // Combine all predicates with AND logic
            when (predicates.size) {
                0 -> criteriaBuilder.conjunction()
                1 -> predicates[0]
                else -> criteriaBuilder.and(*predicates.toTypedArray())
            }
        }
    }

    fun hasDanceStyles(danceStyleIds: List<UUID>?): Specification<Event>? {
        return danceStyleIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            Specification { root, query, _ ->
                query?.distinct(true)
                val danceStyleJoin = root.join<Event, DanceStyle>("danceStyles", JoinType.INNER)
                danceStyleJoin.get<UUID>("id").`in`(ids)
            }
        }
    }

    fun hasEventTypes(eventTypeIds: List<UUID>?): Specification<Event>? {
        return eventTypeIds?.takeIf { it.isNotEmpty() }?.let { ids ->
            Specification { root, query, _ ->
                query?.distinct(true)
                val eventTypeJoin = root.join<Event, EventType>("typesOfEvents", JoinType.INNER)
                eventTypeJoin.get<UUID>("id").`in`(ids)
            }
        }
    }

    fun buildSpecification(
        status: String,
        eventName: String?,
        city: String?,
        country: String?,
        danceStyleIds: List<UUID>?,
        eventTypeIds: List<UUID>?
    ): Specification<Event> {
        var spec = hasStatus(status)

        hasEventName(eventName)?.let { spec = spec.and(it) }
        hasLocationFilters(city, country)?.let { spec = spec.and(it) }
        hasDanceStyles(danceStyleIds)?.let { spec = spec.and(it) }
        hasEventTypes(eventTypeIds)?.let { spec = spec.and(it) }

        return spec
    }
}

