package dnit.storage.minio.api

/**
 * Classe de Configuração para a inicialização do cliente Minio.
 * Utilizada em conjunto com o Storage Service para a criação de um cliente

 * val service: StorageService = MinioClientService(MinioConfiguration())
 * @see StorageService
 * @see MinioConfiguration
 * @see MinioClientService
 */
data class MinioConfiguration(
    val host: String = "localhost",
    val port: Int = 9000,
    val useSsl: Boolean = false,
    val username: String = "minio123",
    val password: String = "minio123",
) {

    init {
        require(host.isNotBlank()) { "Host cannot be blank" }
        require(port in 1..65535) { "Port must be between 1 and 65535" }
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }
    }

}
