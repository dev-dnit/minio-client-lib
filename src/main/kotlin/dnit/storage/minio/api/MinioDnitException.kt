package dnit.storage.minio.api

/**
 * Classe de exceção customizada para operações de armazenamento Minio.
 * Essa exceção é lançada quando alguma operação, como: upload, download, ou URL de acesso, falha.
 */
class MinioDnitException : Throwable {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}