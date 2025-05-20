package dnit.storage.minio.api

class MinioConfiguration {
    val host: String = "localhost";
    val port: Int = 9000;
    val username: String = "minio123";
    val password: String = "minio123";
    val useSsl: Boolean = false;
}