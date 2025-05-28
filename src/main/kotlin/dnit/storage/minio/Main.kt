package dnit.storage.minio

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.internal.service.MinioServiceImpl


fun main() {
    val service = MinioServiceImpl(MinioConfiguration())

    val file = service.listObjectsByPage("dnit", 0, 20)
    println("Total files: ${file}")
}