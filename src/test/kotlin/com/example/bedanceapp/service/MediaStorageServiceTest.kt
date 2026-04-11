package com.example.bedanceapp.service

import io.minio.MinioClient
import io.minio.GetObjectResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.InputStream

@DisplayName("MediaStorageService Tests")
class MediaStorageServiceTest {

    @Mock private lateinit var minioClient: MinioClient

    private lateinit var service: MediaStorageService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = MediaStorageService(minioClient, "bucket")
    }

    @Test
    fun `upload delegates to minio client`() {
        service.upload("file.jpg", ByteArrayInputStream("abc".toByteArray()), 3, "image/jpeg")

        verify(minioClient).putObject(any())
    }

    @Test
    fun `get delegates to minio client`() {
        val stream = org.mockito.kotlin.mock<GetObjectResponse>()
        whenever(minioClient.getObject(any())).thenReturn(stream)

        val result: InputStream = service.get("file.jpg")

        verify(minioClient).getObject(any())
        kotlin.test.assertEquals(stream, result)
    }

    @Test
    fun `delete delegates to minio client`() {
        service.delete("file.jpg")

        verify(minioClient).removeObject(any())
    }
}

