package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.Result
import io.minio.messages.Item
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ListObjectsByPageTest {

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
    fun `listObjectsByPage should return correct objects for valid page and pageSize`() {
        // Arrange
        val bucketName = "test-bucket"
        val page = 1
        val pageSize = 2
        val objectNames = listOf("file1.txt", "file2.txt", "file3.txt", "file4.txt", "file5.txt")
        
        // Create mock Items and Results
        val mockResults = createMockResults(objectNames)
        
        // Mock the listObjects method to return our mock Iterable
        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenReturn(mockResults)
        
        // Act
        val result = minioServiceImpl.listObjectsByPage(bucketName, page, pageSize)
        
        // Assert
        // We expect items from index 2 and 3 (page 1, size 2)
        assertEquals(listOf("file3.txt", "file4.txt"), result)
    }
    
    @Test
    fun `listObjectsByPage should return empty list when page is beyond available objects`() {
        // Arrange
        val bucketName = "test-bucket"
        val page = 10
        val pageSize = 2
        val objectNames = listOf("file1.txt", "file2.txt", "file3.txt")
        
        // Create mock Items and Results
        val mockResults = createMockResults(objectNames)
        
        // Mock the listObjects method to return our mock Iterable
        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenReturn(mockResults)
        
        // Act
        val result = minioServiceImpl.listObjectsByPage(bucketName, page, pageSize)
        
        // Assert
        assertEquals(emptyList<String>(), result)
    }
    
    @Test
    fun `listObjectsByPage should throw IllegalArgumentException when page is negative`() {
        // Arrange
        val bucketName = "test-bucket"
        val page = -1
        val pageSize = 2
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            minioServiceImpl.listObjectsByPage(bucketName, page, pageSize)
        }
        
        assertEquals("Page must be >= 0", exception.message)
    }
    
    @Test
    fun `listObjectsByPage should throw IllegalArgumentException when pageSize is less than 1`() {
        // Arrange
        val bucketName = "test-bucket"
        val page = 0
        val pageSize = 0
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            minioServiceImpl.listObjectsByPage(bucketName, page, pageSize)
        }
        
        assertEquals("Page size must be >= 1", exception.message)
    }
    
    @Test
    fun `listObjectsByPage should throw MinioDnitException when client throws exception`() {
        // Arrange
        val bucketName = "test-bucket"
        val page = 0
        val pageSize = 10
        
        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenThrow(RuntimeException("Test exception"))
        
        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.listObjectsByPage(bucketName, page, pageSize)
        }
        
        assertEquals("[MinioDnitException] Test exception", exception.message)
    }
    
    private fun createMockResults(objectNames: List<String>): Iterable<Result<Item>> {
        val results = mutableListOf<Result<Item>>()
        
        for (name in objectNames) {
            val item = mock<Item>()
            whenever(item.objectName()).thenReturn(name)
            
            val result = mock<Result<Item>>()
            whenever(result.get()).thenReturn(item)
            
            results.add(result)
        }
        
        return results
    }
}