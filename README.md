# DNIT - Minio Helper Library

Biblioteca auxiliar para operações com armazenamento MinIO.
Facilita o upload, download e gerenciamento de arquivos em buckets MinIO, simplificando o uso do SDK oficial.

✨ Funcionalidades Principais
- 📄 Upload, download e exclusão de arquivos.

- 🔗 Geração de URLs temporárias (presigned) para acesso seguro a arquivos.

- 🗂️ Criação e verificação de buckets automaticamente.

- 🔐 Controle de expiração de links de acesso.


## 🚀 Instalação

Adicione a dependência no seu projeto Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.dev-dnit</groupId>
    <artifactId>minio-helper</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🐳 Instanciando MinIO local
Execute o comando abaixo para rodar um container MinIO local:
```bash
docker run -p 9000:9000 -p 9001:9001 --name minio_dnit -e "MINIO_ROOT_USER=minio123" -e "MINIO_ROOT_PASSWORD=minio123" quay.io/minio/minio server /data --console-address ":9001"
```
