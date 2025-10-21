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
    <artifactId>minio-client-lib</artifactId>
    <version>1.2</version>
</dependency>
```

## 🐳 Instanciando MinIO local
Execute o comando abaixo para rodar um container MinIO local:
```bash
docker run -p 9000:9000 -p 9001:9001 --name minio_dnit -e "MINIO_ROOT_USER=minio123" -e "MINIO_ROOT_PASSWORD=minio123" quay.io/minio/minio server /data --console-address ":9001"
```


## 🔧 Utilização
Para utilizar o MinioClient via injeção de dependências (Spring), basta criar uma classe que implemente a interface MinioClientService.

Inicialmente, crie as variáveis de ambiente com os valores de acesso ao Minio.
```properties
dnit.env.minio.host=${MINIO_HOST:localhost}
dnit.env.minio.port=${MINIO_PORT:9000}
dnit.env.minio.username=${MINIO_USERNAME:minio123}
dnit.env.minio.password=${MINIO_PASSWORD:minio123}
```

Em seguida, faça a criação de Beans para que o MinioClientService seja injetado na classe que deseja utilizar.
```java

import dnit.storage.minio.api.MinioConfiguration;
import dnit.storage.minio.api.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MySpringProjectConfiguration {

    @Value("${dnit.env.minio.host}")
    String host;

    @Value("${dnit.env.minio.port}")
    int port;

    @Value("${dnit.env.minio.username}")
    String username;

    @Value("${dnit.env.minio.password}")
    String password;


    @Bean
    MinioConfiguration configuration () {
        final boolean useSSL = false;
        return new MinioConfiguration(host, port, useSSL, username, password);
    }

    @Bean
    MinioService service (MinioConfiguration configuration) {
        return new MinioServiceImpl(configuration);
    }

}
```


Em seguida, utilize o MinioService onde quiser
```kotlin
@RestController
class ExampleController(
    private val minioService: MinioService
) {

    @PostMapping("/minio-link")
    fun obtemLinkDocumento(
        @RequestBody fileName: String
    ) : String {

        return minioService.getDocumentUrl("dnit-bucket", fileName)
    }


    @PostMapping("/minio-download")
    fun obtemDocumentoParaDownload(
        @RequestBody fileName: String
    ): ResponseEntity<InputStreamResource> {
        val inputStream = minioService.downloadFile("dnit-bucket", fileName)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(InputStreamResource(inputStream))
    }

}
```