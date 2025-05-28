package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeleteFileTest {

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
    fun `deleteFile should call removeObject on client`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        
        // Act
        minioServiceImpl.deleteFile(bucketName, filename)
        
        // Assert
        verify(minioClient).removeObject(any<RemoveObjectArgs>())
    }
    
    @Test
    fun `deleteFile should pass correct bucket and filename to client`() {
        // Arrange
        val bucketName = "specific-bucket"
        val filename = "specific-file.txt"
        
        // Use a custom argument matcher to capture and verify the arguments
        whenever(minioClient.removeObject(any<RemoveObjectArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<RemoveObjectArgs>(0)
            // Verify parameters
            assertEquals(bucketName, args.bucket())
            assertEquals(filename, args.`object`())
            null
        }
        
        // Act
        minioServiceImpl.deleteFile(bucketName, filename)
        
        // No explicit assert needed as the verification is in the answer block
    }
    
    @Test
    fun `deleteFile should throw MinioDnitException when client throws exception`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "error-file.txt"
        
        // Mock removeObject throws exception
        whenever(minioClient.removeObject(any<RemoveObjectArgs>())).thenThrow(RuntimeException("Delete failed"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.deleteFile(bucketName, filename)
        }
        
        assertEquals("[MinioDnitException] Delete failed", exception.message)
    }
}