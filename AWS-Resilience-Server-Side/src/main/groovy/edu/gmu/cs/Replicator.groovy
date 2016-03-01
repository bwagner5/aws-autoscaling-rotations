package edu.gmu.cs

import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
@Configuration
class Replicator {

    @Value('${upload.location}')
    protected String UPLOAD_DIR

    @Value('${multipart.location}')
    protected String TEMP_UPLOAD_DIR

    @Value('${server.port}')
    private String SERVER_PORT

    public List<Thread> replicate(List<String> hosts){

        println "${SERVER_PORT} <---------------------------------------- HERE!!"
        println "${UPLOAD_DIR} <---------------------------------------- HERE!!"
        println "${TEMP_UPLOAD_DIR} <---------------------------------------- HERE!!"

        if(SERVER_PORT == '8081') return []

        List<Thread> replicationThreads = []
        hosts.each { String host ->
            replicationThreads.add(
                    new Thread({
                        while(true){
                            File tempDir = new File(TEMP_UPLOAD_DIR)
                            File[] files = tempDir.listFiles().minus(new File('.DS_STORE'))

                            for(File file : files){
                                CloseableHttpClient httpClient = HttpClients.createDefault();
                                HttpPost uploadFile = new HttpPost(host + File.separator + 'upload');

                                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                                builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, "${file}");
                                HttpEntity multipart = builder.build();

                                uploadFile.setEntity(multipart);

                                CloseableHttpResponse response = httpClient.execute(uploadFile);
                                HttpEntity responseEntity = response.getEntity();
                            }
                            sleep(1000)
                        }

                    })
            )
        }

        return replicationThreads
    }

}
