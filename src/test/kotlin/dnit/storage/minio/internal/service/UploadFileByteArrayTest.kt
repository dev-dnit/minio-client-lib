package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UploadFileByteArrayTest {

    private lateinit var minioClient: MinioClient
    private lateinit var minioClientService: MinioClientService
    
    @BeforeEach
    fun setUp() {
        minioClient = mock()
        
        // Use reflection to set the mocked client
        val service = MinioClientService(MinioConfiguration())
        val clientField = MinioClientService::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(service, minioClient)
        
        minioClientService = service
    }
    
    @Test
    fun `uploadFile should throw MinioDnitException when bucket check fails`() {
        // Arrange
        val bucketName = "error-bucket"
        val filename = "test-file.txt"
        val content = "Hello, World!".toByteArray()
        
        // Mock bucket check throws exception
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenThrow(RuntimeException("Bucket check failed"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioClientService.uploadFile(bucketName, filename, content)
        }
        
        assertEquals("Failed to check if bucket exists", exception.message)
    }
    
    @Test
    fun `uploadFile should throw MinioDnitException when upload fails`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "error-file.txt"
        val content = "Hello, World!".toByteArray()
        
        // Mock bucket exists check
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenReturn(true)
        
        // Mock upload throws exception
        whenever(minioClient.putObject(any<PutObjectArgs>())).thenThrow(RuntimeException("Upload failed"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioClientService.uploadFile(bucketName, filename, content)
        }
        
        assertEquals("[MinioDnitException] Upload failed", exception.message)
    }
}