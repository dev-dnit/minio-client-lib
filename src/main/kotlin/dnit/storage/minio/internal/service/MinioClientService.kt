package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.StorageService
import io.minio.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.time.Duration

class MinioClientService : StorageService {
    private lateinit var client: MinioClient

    constructor(configurations: MinioConfiguration) {
        client = MinioClient.builder()
            .endpoint(configurations.host, configurations.port, configurations.useSsl)
            .credentials(configurations.username, configurations.password)
            .build();
    }

    override fun uploadFile(
        bucketName: String,
        filename: String,
        content: ByteArray
    ): String {
        ensureBucketExists(bucketName)
        val size = content.size.toLong();
        val response = client.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(filename)
                .stream(ByteArrayInputStream(content), size, -1)
                .build()
        )

        return response.`object`();
    }

    override fun uploadFile(
        bucketName: String,
        filename: String,
        content: InputStream
    ): String {
        TODO("Not yet implemented")
    }

    override fun downloadFile(bucketName: String, filename: String): InputStream {
        TODO("Not yet implemented")
    }

    override fun getUrl(
        bucketName: String,
        filename: String,
        expiration: Duration
    ): String {
        TODO("Not yet implemented")
    }

    override fun bucketExists(bucketName: String): Boolean {
        val result = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
        return result
    }

    override fun fileExists(bucketName: String, filename: String): Boolean {
        ensureBucketExists(bucketName)
        val stat = client.statObject(StatObjectArgs.builder().bucket(bucketName).`object`(filename).build())
        return stat?.`object`() != null
    }

    override fun downloadChunkedFile(
        bucketName: String,
        filename: String,
        offset: Long,
        length: Long
    ): InputStream {
        TODO("Not yet implemented")
    }

    private fun ensureBucketExists(bucketName: String) {
        val result = bucketExists(bucketName)
        if (!result) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
        }
    }
}