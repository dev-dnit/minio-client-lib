package dnit.storage.minio.api

import java.io.InputStream

/**
 * Service responsável pelo armazenamento e recuperação de arquivos em um sistema de armazenamento de objetos.
 * Esta interface define operações para upload, download, verificação de existência e obtenção de URLs para arquivos
 * armazenados em buckets.
 *
 * A instanciação desse service é feita da seguinte maneira:
 * val service: StorageService = MinioClientService(MinioConfiguration())
 * @see MinioService
 * @see MinioConfiguration
 * @see dnit.storage.minio.internal.service.MinioServiceImpl
 */
interface MinioService {

    fun bucketExists(bucketName: String): Boolean

    fun fileExists(bucketName: String, filename: String): Boolean

    fun listObjectsByPage(bucketName: String, page: Int = 0, pageSize: Int = 20): List<String>

    fun getDocumentUrl(bucketName: String, filename: String, expirationInDays: Int = 7): String

    fun uploadFile(bucketName: String, filename: String, content: ByteArray): String?

    fun uploadFile(bucketName: String, filename: String, content: InputStream): String?

    fun downloadFile(bucketName: String, filename: String): InputStream

    fun downloadChunkedFile(bucketName: String, filename: String, offset: Long, length: Long): InputStream

    fun deleteFile(bucketName: String, filename: String)

}
