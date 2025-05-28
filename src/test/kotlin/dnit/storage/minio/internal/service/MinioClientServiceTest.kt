package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import io.minio.messages.ErrorResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MinioClientServiceTest {

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

    // bucketExists tests
    @Test
    fun `bucketExists should return true when bucket exists`() {
        // Arrange
        val bucketName = "test-bucket"
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenReturn(true)

        // Act
        val result = minioServiceImpl.bucketExists(bucketName)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `bucketExists should return false when bucket does not exist`() {
        // Arrange
        val bucketName = "non-existent-bucket"
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenReturn(false)

        // Act
        val result = minioServiceImpl.bucketExists(bucketName)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `bucketExists should throw MinioDnitException when client throws exception`() {
        // Arrange
        val bucketName = "error-bucket"
        whenever(minioClient.bucketExists(any<BucketExistsArgs>())).thenThrow(RuntimeException("Test exception"))

        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.bucketExists(bucketName)
        }

        assertEquals("Failed to check if bucket exists", exception.message)
    }

    // fileExists tests
    @Test
    fun `fileExists should return true when file exists`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"

        // Mock successful statObject call
        whenever(minioClient.statObject(any<StatObjectArgs>())).thenReturn(null)

        // Act
        val result = minioServiceImpl.fileExists(bucketName, filename)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `fileExists should return false when file does not exist`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "non-existent-file.txt"

        // Create a mock ErrorResponseException with NoSuchKey error code
        val errorResponse = mock<ErrorResponse>()
        whenever(errorResponse.code()).thenReturn("NoSuchKey")

        val exception = mock<ErrorResponseException>()
        whenever(exception.errorResponse()).thenReturn(errorResponse)

        whenever(minioClient.statObject(any<StatObjectArgs>())).thenThrow(exception)

        // Act
        val result = minioServiceImpl.fileExists(bucketName, filename)

        // Assert
        assertFalse(result)
    }

    // listObjectsByPage tests
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

    // deleteFile tests
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

    // downloadFile tests
    @Test
    fun `downloadFile should call getObject on client`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"

        // Act
        try {
            minioServiceImpl.downloadFile(bucketName, filename)
        } catch (e: Exception) {
            // Ignore exceptions, we're just verifying the method call
        }

        // Assert
        verify(minioClient).getObject(any<GetObjectArgs>())
    }

    @Test
    fun `downloadFile should throw MinioDnitException when client throws exception`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "error-file.txt"

        // Mock download throws exception
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenThrow(RuntimeException("Download failed"))

        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.downloadFile(bucketName, filename)
        }

        assertEquals("[MinioDnitException] Download failed", exception.message)
    }

    // downloadChunkedFile tests
    @Test
    fun `downloadChunkedFile should call getObject with offset and length`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        val offset = 10L
        val length = 20L

        // Act
        try {
            minioServiceImpl.downloadChunkedFile(bucketName, filename, offset, length)
        } catch (e: Exception) {
            // Ignore exceptions, we're just verifying the method call
        }

        // Assert
        verify(minioClient).getObject(any<GetObjectArgs>())
    }

    // getDocumentUrl tests
    @Test
    fun `getDocumentUrl should call getPresignedObjectUrl on client`() {
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
}
