package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import dnit.storage.minio.api.MinioService
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.ListObjectsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.RemoveObjectsArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import io.minio.http.Method
import io.minio.messages.DeleteObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit


/**
 * Implementação do StorageService que utiliza o cliente MinIO para operações de armazenamento.
 * Esta classe fornece funcionalidades para interagir com um servidor MinIO,
 * permitindo upload, download, verificação de existência e obtenção de URLs para arquivos.
 *
 * @param configurations Configurações para conexão com o servidor MinIO
 */
class MinioServiceImpl(configurations: MinioConfiguration) : MinioService {

    private var client: MinioClient = MinioClient
        .builder()
        .endpoint(configurations.host, configurations.port, configurations.useSsl)
        .credentials(configurations.username, configurations.password)
        .apply { configurations.region?.let { region(it) } }
        .build()


    override fun bucketExists(bucketName: String): Boolean {
        return try {
            client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
        } catch (e: Exception) {
            throw MinioDnitException("Failed to check if bucket exists", e)
        }
    }



    override fun fileExists(bucketName: String, filename: String): Boolean {
        return try {
            val statObjectArgs = StatObjectArgs.builder()
                .bucket(bucketName)
                .`object`(filename)
                .build()

            client.statObject(statObjectArgs)
            true
        } catch (e: Exception) {
            // MinIO throws an exception if object does not exist
            if (e is ErrorResponseException && e.errorResponse().code() == "NoSuchKey") {
                false

            } else {
                throw MinioDnitException("Failed to check if object exists", e)
            }
        }
    }



    override fun listObjectsByPage(
        bucketName: String,
        page: Int,
        pageSize: Int
    ): List<String> {

        require(page >= 0) { "Page must be >= 0" }
        require(pageSize >= 1) { "Page size must be >= 1" }

        return throwableExecute {
            val argsBuilder = ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)

            val objectsIterable = client.listObjects(argsBuilder.build())

            objectsIterable
                .map { it.get().objectName() }
                .drop(page* pageSize)
                .take(pageSize)
        }
    }



    override fun uploadFile(
        bucketName: String,
        filename: String,
        content: ByteArray
    ): String? {
        return throwableExecute<String?> {
            ensureBucketExists(bucketName)

            val size = content.size.toLong()
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
        return throwableExecute<String?> {
            ensureBucketExists(bucketName)

            val size = content.available().toLong()
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
        return throwableExecute {
            client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filename)
                    .build()
            )
        }
    }



    override fun getDocumentUrl(
        bucketName: String,
        filename: String,
        expirationInDays: Int
    ): String {
        return throwableExecute {
            client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(filename)
                    .expiry(expirationInDays, TimeUnit.DAYS)
                    .build()
            )
        }
    }



    override fun downloadChunkedFile(
        bucketName: String,
        filename: String,
        offset: Long,
        length: Long
    ): InputStream {
        return throwableExecute {
            client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filename)
                    .offset(offset)
                    .length(length)
                    .build()
            )
        }
    }



    override fun deleteFile(bucketName: String, filename: String) {
        return throwableExecute {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(filename)
                    .build()
            )
        }
    }



    override fun deleteFolder(bucketName: String, folderName: String) {
        return throwableExecute {
            // Ensure folderName ends with "/" to avoid partial matches
            val prefix = if (folderName.endsWith("/")) folderName else "$folderName/"

            val objects = client.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build()
            )

            val toDelete = objects.map { DeleteObject(it.get().objectName()) }.toList()

            if (toDelete.isNotEmpty()) {
                val results = client.removeObjects(
                    RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(toDelete)
                        .build()
                )

                // Check for errors
                for (result in results) {
                    val err = result.get()
                    if (err != null) {
                        throw MinioDnitException("Error deleting object ${err.objectName()} : ${err.message()}")
                    }
                }
            }
        }
    }

    private fun ensureBucketExists(bucketName: String) {
        try {
            if (!bucketExists(bucketName)) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            }

        } catch (e: Exception) {
            throw MinioDnitException("Failed to ensure bucket exists", e)
        }
    }



    private fun <TResult> throwableExecute(func: () -> TResult): TResult {
        return try {
            func()
        } catch (e: Exception) {
            throw MinioDnitException("[MinioDnitException] ${e.message}", e)
        }
    }

}
