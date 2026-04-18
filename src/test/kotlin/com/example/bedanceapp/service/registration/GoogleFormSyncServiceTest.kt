package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.GoogleFormsService
import com.google.api.services.forms.v1.model.Form
import com.google.api.services.forms.v1.model.FormResponse
import com.google.api.services.forms.v1.model.ListFormResponsesResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("GoogleFormSyncService Tests")
class GoogleFormSyncServiceTest {

    @Mock private lateinit var googleFormsService: GoogleFormsService
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var registrationStatusService: RegistrationStatusService
    @Mock private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    @Mock private lateinit var googleFormMapper: GoogleFormMapper
    @Mock private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var googleFormSyncService: GoogleFormSyncService

    private val eventId = UUID.randomUUID()
    private val organizerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        googleFormSyncService = GoogleFormSyncService(
            googleFormsService,
            userRepository,
            eventRegistrationRepository,
            registrationStatusService,
            eventRegistrationSettingsRepository,
            googleFormMapper,
            transactionTemplate
        )

        // Execute transaction callback inline for deterministic unit tests.
        doAnswer { invocation ->
            val callback = invocation.getArgument<Consumer<TransactionStatus>>(0)
            callback.accept(mock())
            null
        }.whenever(transactionTemplate).executeWithoutResult(any())
    }

    @Test
    fun `syncRegistrationData creates new registration from Google response`() {
        val settings = createSettings(formId = "form-1", formStructure = "old-structure")
        val organizer = createUser(organizerId, "organizer@example.com")
        val response = FormResponse()
            .setResponseId("resp-1")
            .setRespondentEmail("dancer@example.com")
            .setLastSubmittedTime("2026-04-10T12:00:00Z")
        val row = createRow(id = "resp-1", email = "dancer@example.com", userId = null, lastSubmittedTime = "2026-04-10T12:00:00Z")

        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(settings)
        whenever(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer))
        whenever(googleFormsService.getForm(organizer, "form-1")).thenReturn(Form().setRevisionId("rev-2"))
        whenever(googleFormsService.getFormResponses(organizer, "form-1")).thenReturn(
            listOf(response)
        )

        whenever(googleFormMapper.extractRevisionId("old-structure")).thenReturn("rev-1")
        whenever(googleFormMapper.convertGoogleFormToStructuredForm(any())).thenReturn(StructuredForm("rev-2", emptyList()))
        whenever(googleFormMapper.writeStructuredForm(any())).thenReturn("new-structure")

        whenever(userRepository.findByEmail("dancer@example.com")).thenReturn(null)
        whenever(googleFormMapper.mapFormResponseToRegistrationRow(response, null)).thenReturn(row)
        whenever(eventRegistrationRepository.findByEventIdAndResponseId(eventId, "resp-1")).thenReturn(emptyList())
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(emptyList())
        whenever(registrationStatusService.assignRegistrationStatus(any(), eq(RegistrationStatus.REGISTERED), eq(eventId), anyOrNull(), anyOrNull()))
            .thenReturn(RegistrationStatus.REGISTERED)
        whenever(googleFormMapper.writeRowStructure(row)).thenReturn("row-json")

        googleFormSyncService.syncRegistrationData(eventId, organizerId, 20)

        val registrationCaptor = argumentCaptor<EventRegistration>()
        verify(eventRegistrationRepository).save(registrationCaptor.capture())
        assertEquals("resp-1", registrationCaptor.firstValue.responseId)
        assertEquals("dancer@example.com", registrationCaptor.firstValue.email)
        assertEquals("row-json", registrationCaptor.firstValue.formResponses)
        assertEquals(RegistrationStatus.REGISTERED, registrationCaptor.firstValue.status)
        assertNotNull(registrationCaptor.firstValue.updatedAt)

        verify(eventRegistrationSettingsRepository).save(settings.copy(formStructure = "new-structure"))
        verify(eventRegistrationRepository).flush()
    }

    @Test
    fun `syncRegistrationData updates existing responses when incoming timestamp is newer`() {
        val settings = createSettings(formId = "form-1", formStructure = "same-revision")
        val organizer = createUser(organizerId, "organizer@example.com")
        val response = FormResponse()
            .setResponseId("resp-2")
            .setRespondentEmail("dancer@example.com")
            .setLastSubmittedTime("2026-04-11T12:00:00Z")
        val row = createRow(id = "resp-2", email = "dancer@example.com", userId = UUID.randomUUID(), lastSubmittedTime = "2026-04-11T12:00:00Z")
        val existing = EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = row.user.userId,
            status = RegistrationStatus.REGISTERED,
            roleId = null,
            email = "dancer@example.com",
            isAnonymous = true,
            responseId = "resp-2",
            formResponses = "old-row-json"
        )

        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(settings)
        whenever(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer))
        whenever(googleFormsService.getForm(organizer, "form-1")).thenReturn(Form().setRevisionId("rev-2"))
        whenever(googleFormsService.getFormResponses(organizer, "form-1")).thenReturn(
            listOf(response)
        )
        whenever(googleFormMapper.extractRevisionId("same-revision")).thenReturn("rev-2")

        whenever(userRepository.findByEmail("dancer@example.com")).thenReturn(createUser(row.user.userId!!, "dancer@example.com"))
        whenever(googleFormMapper.mapFormResponseToRegistrationRow(response, row.user.userId)).thenReturn(row)

        whenever(eventRegistrationRepository.findByEventIdAndResponseId(eventId, "resp-2")).thenReturn(listOf(existing))
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(listOf(existing))
        whenever(googleFormMapper.parseRowStructure("old-row-json")).thenReturn(
            RowStructure(data = emptyList(), lastSubmittedTime = "2026-04-10T12:00:00Z")
        )
        whenever(googleFormMapper.writeRowStructure(row)).thenReturn("new-row-json")

        googleFormSyncService.syncRegistrationData(eventId, organizerId, 20)

        val registrationCaptor = argumentCaptor<EventRegistration>()
        verify(eventRegistrationRepository).save(registrationCaptor.capture())
        assertEquals(existing.id, registrationCaptor.firstValue.id)
        assertEquals("new-row-json", registrationCaptor.firstValue.formResponses)

        verify(eventRegistrationSettingsRepository, never()).save(any())
        verify(eventRegistrationRepository).flush()
    }

    @Test
    fun `syncRegistrationData backfills user id for existing registration`() {
        val settings = createSettings(formId = "form-1", formStructure = "same-revision")
        val organizer = createUser(organizerId, "organizer@example.com")
        val mappedUserId = UUID.randomUUID()
        val response = FormResponse()
            .setResponseId("resp-3")
            .setRespondentEmail("dancer@example.com")
        val row = createRow(id = "resp-3", email = "dancer@example.com", userId = mappedUserId, lastSubmittedTime = null)
        val existing = EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = null,
            status = RegistrationStatus.REGISTERED,
            roleId = null,
            email = "dancer@example.com",
            isAnonymous = true,
            responseId = "resp-3",
            formResponses = "existing-row-json"
        )

        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(settings)
        whenever(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer))
        whenever(googleFormsService.getForm(organizer, "form-1")).thenReturn(Form().setRevisionId("rev-2"))
        whenever(googleFormsService.getFormResponses(organizer, "form-1")).thenReturn(
            listOf(response)
        )
        whenever(googleFormMapper.extractRevisionId("same-revision")).thenReturn("rev-2")

        whenever(userRepository.findByEmail("dancer@example.com")).thenReturn(createUser(mappedUserId, "dancer@example.com"))
        whenever(googleFormMapper.mapFormResponseToRegistrationRow(response, mappedUserId)).thenReturn(row)

        whenever(eventRegistrationRepository.findByEventIdAndResponseId(eventId, "resp-3")).thenReturn(listOf(existing))
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(listOf(existing))
        whenever(googleFormMapper.parseRowStructure("existing-row-json")).thenReturn(
            RowStructure(data = emptyList(), lastSubmittedTime = "2026-04-10T12:00:00Z")
        )

        googleFormSyncService.syncRegistrationData(eventId, organizerId, 20)

        val registrationCaptor = argumentCaptor<EventRegistration>()
        verify(eventRegistrationRepository).save(registrationCaptor.capture())
        assertEquals(existing.id, registrationCaptor.firstValue.id)
        assertEquals(mappedUserId, registrationCaptor.firstValue.userId)

        verify(eventRegistrationSettingsRepository, never()).save(any())
        verify(eventRegistrationRepository).flush()
    }

    private fun createSettings(formId: String, formStructure: String?): EventRegistrationSettings {
        return EventRegistrationSettings(
            eventId = eventId,
            event = null,
            registrationMode = RegistrationMode.GOOGLE_FORM,
            formId = formId,
            formStructure = formStructure,
            requireApproval = false
        )
    }

    private fun createUser(id: UUID, email: String): User {
        return User(
            id = id,
            email = email,
            provider = "google",
            providerId = "provider-$id"
        )
    }

    private fun createRow(id: String, email: String, userId: UUID?, lastSubmittedTime: String?): RegistrationRow {
        return RegistrationRow(
            id = id,
            user = RegistrationUserDto(email = email, userId = userId),
            data = emptyList(),
            lastSubmittedTime = lastSubmittedTime,
            status = RegistrationStatus.PENDING
        )
    }
}

