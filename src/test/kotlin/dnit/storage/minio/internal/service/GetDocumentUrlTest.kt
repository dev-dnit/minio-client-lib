package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetDocumentUrlTest {

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
    fun `getDocumentUrl should return URL when successful`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        val expirationInDays = 7
        val expectedUrl = "https://minio-server.example.com/test-bucket/test-file.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&..."
        
        // Mock successful URL generation
        whenever(minioClient.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>())).thenReturn(expectedUrl)
        
        // Act
        val result = minioServiceImpl.getDocumentUrl(bucketName, filename, expirationInDays)
        
        // Assert
        assertEquals(expectedUrl, result)
    }
    
    @Test
    fun `getDocumentUrl should use GET method`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        val expectedUrl = "https://minio-server.example.com/test-bucket/test-file.txt?X-Amz-Algorithm=AWS4-HMAC-SHA256&..."
        
        // Use a custom argument matcher to verify the HTTP method
        whenever(minioClient.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<GetPresignedObjectUrlArgs>(0)
            assertEquals(Method.GET, args.method())
            expectedUrl
        }
        
        // Act
        val result = minioServiceImpl.getDocumentUrl(bucketName, filename)
        
        // Assert
        assertEquals(expectedUrl, result)
    }
    
    @Test
    fun `getDocumentUrl should throw MinioDnitException when client throws exception`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "error-file.txt"
        
        // Mock URL generation throws exception
        whenever(minioClient.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>())).thenThrow(RuntimeException("URL generation failed"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.getDocumentUrl(bucketName, filename)
        }
        
        assertEquals("[MinioDnitException] URL generation failed", exception.message)
    }
}