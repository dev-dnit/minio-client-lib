package dnit.storage.minio.internal.service

import dnit.storage.minio.api.MinioConfiguration
import dnit.storage.minio.api.MinioDnitException
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.RemoveObjectsArgs
import io.minio.Result
import io.minio.messages.DeleteError
import io.minio.messages.Item
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeleteFolderTest {

    private lateinit var minioClient: MinioClient
    private lateinit var minioServiceImpl: MinioServiceImpl

    @BeforeEach
    fun setUp() {
        minioClient = mock()

        // Inject mocked client
        val service = MinioServiceImpl(MinioConfiguration())
        val clientField = MinioServiceImpl::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        clientField.set(service, minioClient)

        minioServiceImpl = service
    }

    @Test
    fun `deleteFolder should list objects with proper prefix and call removeObjects`() {
        val bucket = "bucket"
        val folder = "folder" // without trailing '/'
        val objectNames = listOf("folder/a.txt", "folder/b.txt", "folder/sub/c.txt")

        // Mock listObjects to return the expected results
        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<ListObjectsArgs>(0)
            // Ensure prefix is folder/
            assertEquals("$folder/", args.prefix())
            // Ensure recursive is true by assuming non-null; ListObjectsArgs does not expose directly,
            // but our service always sets it; here we only verify prefix and bucket
            assertEquals(bucket, args.bucket())
            createItemResults(objectNames)
        }

        // Mock removeObjects to return an empty iterable (no DeleteError)
        whenever(minioClient.removeObjects(any<RemoveObjectsArgs>())).thenAnswer { invocation ->
            val args = invocation.getArgument<RemoveObjectsArgs>(0)
            assertEquals(bucket, args.bucket())
            // We cannot easily introspect the internal list of DeleteObject, so we simply return empty results
            emptyList<Result<DeleteError>>()
        }

        // Act
        minioServiceImpl.deleteFolder(bucket, folder)

        // Assert interactions
        verify(minioClient).listObjects(any<ListObjectsArgs>())
        verify(minioClient).removeObjects(any<RemoveObjectsArgs>())
    }

    @Test
    fun `deleteFolder should not call removeObjects when no objects are found`() {
        val bucket = "bucket"
        val folder = "emptyFolder"

        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenReturn(emptyList())

        minioServiceImpl.deleteFolder(bucket, folder)

        verify(minioClient).listObjects(any<ListObjectsArgs>())
        verify(minioClient, never()).removeObjects(any<RemoveObjectsArgs>())
    }

    @Test
    fun `deleteFolder should throw MinioDnitException when listObjects throws`() {
        val bucket = "bucket"
        val folder = "boom"

        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenThrow(RuntimeException("List failed"))

        val ex = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.deleteFolder(bucket, folder)
        }
        assertEquals("[MinioDnitException] List failed", ex.message)
    }

    @Test
    fun `deleteFolder should throw MinioDnitException when removeObjects throws`() {
        val bucket = "bucket"
        val folder = "folder"
        val objectNames = listOf("folder/a.txt")

        whenever(minioClient.listObjects(any<ListObjectsArgs>())).thenAnswer { createItemResults(objectNames) }

        whenever(minioClient.removeObjects(any<RemoveObjectsArgs>())).thenThrow(RuntimeException("Remove failed"))

        val ex = assertThrows(MinioDnitException::class.java) {
            minioServiceImpl.deleteFolder(bucket, folder)
        }
        assertEquals("[MinioDnitException] Remove failed", ex.message)
    }

    private fun createItemResults(objectNames: List<String>): Iterable<Result<Item>> {
        val results = mutableListOf<Result<Item>>()
        for (name in objectNames) {
            val item = mock<Item>()
            whenever(item.objectName()).thenReturn(name)
            val res = mock<Result<Item>>()
            whenever(res.get()).thenReturn(item)
            results.add(res)
        }
        return results
    }
}