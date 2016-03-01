package edu.gmu.cs

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class AwsResilienceServerSideApplication {

	@Value('${upload.location}')
	protected String UPLOAD_DIR

	@Value('${multipart.location}')
	protected String TEMP_UPLOAD_DIR

	@Value('${server.port}')
	private String SERVER_PORT

	static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run AwsResilienceServerSideApplication, args

//		List<Thread> replicationThreads = ctx.getBean(Replicator.class).replicate(['http://localhost:8081'])
//		replicationThreads.each {
//			it.start()
//		}
	}

}
