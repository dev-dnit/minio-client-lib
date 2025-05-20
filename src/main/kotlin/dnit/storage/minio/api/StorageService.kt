package dnit.storage.minio.api

import java.io.InputStream
import kotlin.time.Duration

interface StorageService {
    fun uploadFile(bucketName: String, filename: String, content: ByteArray): String
    fun uploadFile(bucketName: String, filename: String, content: InputStream): String
    fun downloadFile(bucketName: String, filename: String): InputStream;
    fun getUrl(bucketName: String, filename: String, expiration: Duration = Duration.ZERO): String;
    fun bucketExists(bucketName: String): Boolean;
    fun fileExists(bucketName: String, filename: String): Boolean;
    fun downloadChunkedFile(bucketName: String, filename: String, offset: Long, length: Long): InputStream;
}