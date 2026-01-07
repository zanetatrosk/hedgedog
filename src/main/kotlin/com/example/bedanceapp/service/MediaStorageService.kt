package com.example.bedanceapp.service

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class MediaStorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket}") private val bucket: String
) {

    fun upload(
        objectName: String,
        inputStream: InputStream,
        size: Long,
        contentType: String
    ) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        )
    }

    fun get(objectName: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .build()
        )
    }
}
