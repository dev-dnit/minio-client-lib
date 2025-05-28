package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.MinioClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream

class DownloadChunkedFileTest {

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
    fun `downloadChunkedFile should return input stream when download is successful`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        val offset = 10L
        val length = 20L
        val mockResponse = mock<GetObjectResponse>()
        whenever(mockResponse.read()).thenReturn(ByteArrayInputStream("Chunk of content".toByteArray()).read())
        whenever(mockResponse.available()).thenReturn("Chunk of content".toByteArray().size)
        val expectedContent = mockResponse

        // Mock successful download
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenReturn(expectedContent)

        // Act
        val result = minioServiceImpl.downloadChunkedFile(bucketName, filename, offset, length)

        // Assert
        assertSame(expectedContent, result)
    }

    @Test
    fun `downloadChunkedFile should throw MinioDnitException when download fails`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "error-file.txt"
        val offset = 10L
        val length = 20L

        // Mock download throws exception
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenThrow(RuntimeException("Chunked download failed"))

        // Act & Assert
        val exception = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.downloadChunkedFile(bucketName, filename, offset, length)
        }

        assertEquals("[MinioDnitException] Chunked download failed", exception.message)
    }

    @Test
    fun `downloadChunkedFile should pass correct parameters to client`() {
        // Arrange
        val bucketName = "specific-bucket"
        val filename = "specific-file.txt"
        val offset = 100L
        val length = 200L
        val mockResponse = mock<GetObjectResponse>()
        whenever(mockResponse.read()).thenReturn(ByteArrayInputStream("Chunk of content".toByteArray()).read())
        whenever(mockResponse.available()).thenReturn("Chunk of content".toByteArray().size)
        val expectedContent = mockResponse

        // Use a custom argument matcher to capture and verify the arguments
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<GetObjectArgs>(0)
            // Verify all parameters
            assertEquals(bucketName, args.bucket())
            assertEquals(filename, args.`object`())
            assertEquals(offset, args.offset())
            assertEquals(length, args.length())
            expectedContent
        }

        // Act
        val result = minioServiceImpl.downloadChunkedFile(bucketName, filename, offset, length)

        // Assert
        assertSame(expectedContent, result)
    }

    @Test
    fun `downloadChunkedFile should work with zero offset`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        val offset = 0L
        val length = 100L
        val mockResponse = mock<GetObjectResponse>()
        whenever(mockResponse.read()).thenReturn(ByteArrayInputStream("Content from the beginning".toByteArray()).read())
        whenever(mockResponse.available()).thenReturn("Content from the beginning".toByteArray().size)
        val expectedContent = mockResponse

        // Use a custom argument matcher to verify the offset
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<GetObjectArgs>(0)
            assertEquals(0L, args.offset())
            expectedContent
        }

        // Act
        val result = minioServiceImpl.downloadChunkedFile(bucketName, filename, offset, length)

        // Assert
        assertSame(expectedContent, result)
    }
}
