package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UploadFileInputStreamTest {

    private lateinit var minioClient: MinioClient
    private lateinit var minioServiceImpl: MinioServiceImpl
    
    @BeforeEach
    fun setUp() {
        minioClient = mock()
        
        // Use reflection to set the mocked client
        val service = MinioServiceImpl(MinioConfiguration())
        val clientField = MinioServiceImpl::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(service, minioClient)
        
        minioServiceImpl = service
    }
    
    @Test
    fun `uploadFile with InputStream should throw MinioDnitException when bucket check fails`() {
        // Arrange
        val bucketName = "error-bucket"
        val filename = "test-file.txt"
        val content = ByteArrayInputStream("Hello, World!".toByteArray())
        
        // Mock bucket check throws exception
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenThrow(RuntimeException("Bucket check failed"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.uploadFile(bucketName, filename, content)
        }
        
        assertEquals("[MinioDnitException] Failed to ensure bucket exists", exception.message)
    }
    
    @Test
    fun `uploadFile with InputStream should throw MinioDnitException when upload fails`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "error-file.txt"
        val content = ByteArrayInputStream("Hello, World!".toByteArray())
        
        // Mock bucket exists check
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenReturn(true)
        
        // Mock upload throws exception
        whenever(minioClient.putObject(any<PutObjectArgs>())).thenThrow(RuntimeException("Upload failed"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.uploadFile(bucketName, filename, content)
        }
        
        assertEquals("[MinioDnitException] Upload failed", exception.message)
    }
}