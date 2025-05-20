package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import dnit.storage.minio.api.StorageService
import io.minio.*
import io.minio.http.Method
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class MinioClientService(configurations: MinioConfiguration) : StorageService {
    private var client: MinioClient = MinioClient.builder()
        .endpoint(configurations.host, configurations.port, configurations.useSsl)
        .credentials(configurations.username, configurations.password)
        .build()

    override fun uploadFile(
        bucketName: String,
        filename: String,
        content: ByteArray
    ): String? {
        return throwableExecute<String?>(bucketName) {
            ->
            val size = content.size.toLong();
            val response = client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filename)
                    .stream(ByteArrayInputStream(content), size, -1)
                    .build()
            )
            response.`object`()
        }
    }

    override fun uploadFile(
        bucketName: String,
        filename: String,
        content: InputStream
    ): String? {
        return throwableExecute<String?>(bucketName) {
            ->
            val size = content.available().toLong();
            val response = client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filename)
                    .stream(content, size, -1)
                    .build()
            )
            response.`object`()
        }
    }

    override fun downloadFile(bucketName: String, filename: String): InputStream {
        return throwableExecute(bucketName) {
            ->
            val response: InputStream = client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(bucketName)
                    .build()
            )
            response
        }
    }

    override fun getUrl(
        bucketName: String,
        filename: String,
        expiration: Duration
    ): String {
        return throwableExecute(bucketName) {
            ->
            client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(filename)
                    .expiry(expiration.inWholeHours.toInt(), TimeUnit.HOURS)
                    .build()
            )
        }
    }

    override fun bucketExists(bucketName: String): Boolean {
        try {
            val result = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            return result
        }catch(e: Exception){
            return false
        }
    }

    override fun fileExists(bucketName: String, filename: String): Boolean {
        return throwableExecute(bucketName) {
            ->
            val stat = client.statObject(StatObjectArgs.builder().bucket(bucketName).`object`(filename).build())
            stat?.`object`() != null
        }
    }

    override fun downloadChunkedFile(
        bucketName: String,
        filename: String,
        offset: Long,
        length: Long
    ): InputStream {
        return throwableExecute(bucketName) {
            ->
            val response: InputStream = client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(bucketName)
                    .offset(offset)
                    .length(length)
                    .build()
            )
            response
        }
    }

    private fun ensureBucketExists(bucketName: String) {
        val result = bucketExists(bucketName)
        if (!result) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
        }
    }

    private fun <TResult> throwableExecute(bucketName: String, func: () -> TResult): TResult {
        try {
            ensureBucketExists(bucketName)
            return func();
        }catch (e: Exception){
            throw MinioDnitException("[MinioDnitException] ${e.message}", e)
        }
    }
}