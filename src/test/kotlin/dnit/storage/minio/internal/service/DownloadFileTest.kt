package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.MinioClient
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DownloadFileTest {

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
    fun `downloadFile should return input stream when download is successful`() {
        // Arrange
        val bucketName = "test-bucket"
        val filename = "test-file.txt"
        val mockResponse = mock<GetObjectResponse>()
        whenever(mockResponse.read()).thenReturn(ByteArrayInputStream("Chunk of content".toByteArray()).read())
        whenever(mockResponse.available()).thenReturn("Chunk of content".toByteArray().size)
        val expectedContent = mockResponse

        // Mock successful download
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenReturn(expectedContent)

        // Act
        val result = minioServiceImpl.downloadFile(bucketName, filename)

        // Assert
        assertSame(expectedContent, result)
    }

    @Test
    fun `downloadFile should throw MinioDnitException when download fails`() {
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

    @Test
    fun `downloadFile should pass correct bucket and filename to client`() {
        // Arrange
        val bucketName = "specific-bucket"
        val filename = "specific-file.txt"
        val mockResponse = mock<GetObjectResponse>()
        whenever(mockResponse.read()).thenReturn(ByteArrayInputStream("Hello, World!".toByteArray()).read())
        whenever(mockResponse.available()).thenReturn("Hello, World!".toByteArray().size)
        val expectedContent = mockResponse

        // Use a custom argument matcher to capture and verify the arguments
        whenever(minioClient.getObject(any<GetObjectArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<GetObjectArgs>(0)
            // This is a simple way to verify the arguments without using ArgumentCaptor
            assertEquals(bucketName, args.bucket())
            assertEquals(filename, args.`object`())
            expectedContent
        }

        // Act
        val result = minioServiceImpl.downloadFile(bucketName, filename)

        // Assert
        assertSame(expectedContent, result)
    }
}
