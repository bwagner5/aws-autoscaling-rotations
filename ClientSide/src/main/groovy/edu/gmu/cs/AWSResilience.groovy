package edu.gmu.cs

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceState
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients


@Slf4j
public class AWSResilience {

    private static final Random random = new Random()

    private static final int CHAOS_WAIT_PERIOD = 120 * 1000

    private static final int DOWNLOAD_HEAD_START_PERIOD = 2 * 1000

    private static final String URL = 'http://localhost:8080'//'http://resilience.brandonwagner.info'
    private static final List<String> FILES = ['smallfile.txt']//,'mediumfile.txt'//,'ubuntu-14.04.3-desktop-amd64.iso','bigfile.txt']

    private static final String FILE_UPLOAD_DIR = 'upload'
    private static final String FILE_DESTINATION_PATH = 'temp'

    private static final int DOWNLOAD_BUFFER_SIZE = 4096

    private static final String AWS_CREDENTIALS_PROFILE = 'gmu'

    private static final int UPLOAD_THREADS = 1
    private static final int DOWNLOAD_THREADS = 1


    public static void main(String[] args){

        File tempDir = new File(FILE_DESTINATION_PATH)
        tempDir.deleteDir()
        tempDir.mkdirs()


        List<Thread> uploadThreads = []
        for(thread in 1..UPLOAD_THREADS){
            uploadThreads.add(new Thread({
                uploadFile()
            }))
        }
        for(uploadThread in uploadThreads){
            uploadThread.start()
        }

        List<Thread> downloadThreads = []
        for(thread in 1..DOWNLOAD_THREADS){
            downloadThreads.add(new Thread({
                downloadFile()
            }))
        }

        Thread chaosThread = new Thread({
            chaos()
        })

        for(downloadThread in downloadThreads){
            downloadThread.start()
        }

        sleep(DOWNLOAD_HEAD_START_PERIOD)

        chaosThread.start()


    }

    public static void chaos(){
        AmazonEC2 ec2 = new AmazonEC2Client(new ProfileCredentialsProvider(AWS_CREDENTIALS_PROFILE))

        while(true){
            try {
                List<Instance> instanceList = ec2.describeInstances()?.getReservations()*.getInstances()*.first()
                instanceList = instanceList.findAll {
                    it?.getState() == InstanceState.newInstance().withCode(16).withName('running')
                }
                if(instanceList.size() == 1){
                    throw new SquashClusterException('Killing an instance would deplete the cluster')
                }
                Instance instanceToKill = instanceList.find { it?.launchTime == instanceList*.launchTime?.min() }

                log.info("Killing instance ${instanceToKill?.instanceId} which came online on ${instanceToKill?.launchTime}")
                ec2.terminateInstances(new TerminateInstancesRequest([instanceToKill?.instanceId]))
            }catch(AmazonServiceException ase){
                log.error('No instances to kill')
            }
            catch(SquashClusterException sce){
                log.error(sce.message)
            }finally{
                log.info("Waiting ${CHAOS_WAIT_PERIOD/1000} seconds")
                sleep(CHAOS_WAIT_PERIOD)
            }
        }
    }

    public static void uploadFile(){
        for(String file : FILES){

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(URL + File.separator + 'upload');

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", new File(FILE_UPLOAD_DIR + File.separator + file), ContentType.APPLICATION_OCTET_STREAM, "${random.nextInt(10 ** 2)}_${file}");
            HttpEntity multipart = builder.build();

            uploadFile.setEntity(multipart);

            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();

        }
    }

    public static void downloadFile(){

        for(String file : FILES){

            URL url = new URL(URL + File.separator + 'download' + File.separator + file)
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection()
            BufferedInputStream reader = setupConnection(httpConnection)
            FileOutputStream fileDestination = new FileOutputStream(FILE_DESTINATION_PATH + File.separator + file + File.separator + Thread.currentThread().getName())

            Long dataLength = httpConnection.contentLengthLong
            Long dataLengthProgress = 0

            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE]
            int length = 0
            while(dataLengthProgress < dataLength){
                while((length = reader.read(buffer)) > 0){
                    fileDestination.write(buffer, 0, length)
                    dataLengthProgress += length
                }
                if(dataLengthProgress < dataLength){
                    log.warn('Renegotiating Connection with Offset')
                    httpConnection.disconnect()
                    httpConnection = (HttpURLConnection) url.openConnection()
                    httpConnection.addRequestProperty('Range', "bytes=${dataLengthProgress}-${dataLength}")
                    reader = setupConnection(httpConnection)
                }
            }

            log.info("TOTAL LENGTH: ${dataLength} -- Downloaded: ${dataLengthProgress}")
        }

        System.exit(0)
    }

    private static BufferedInputStream setupConnection(HttpURLConnection httpConnection){
        final long RETRY_WAIT = 10 * 1000

        httpConnection.setRequestMethod('GET')

        log.info(httpConnection.getHeaderFields().toMapString())
        BufferedInputStream reader
        boolean readerInitialized = false

        while(!readerInitialized){
            try{
                reader = new BufferedInputStream(httpConnection.getInputStream())
            }catch(IOException ie) {
                log.error("IO Exception (sleeping for 10 seconds and then retrying: ${ie.message}")
                sleep(RETRY_WAIT)
            }
        }

        return reader
    }

    //TODO: Check Test with files hashes and compute stats on times and clusters
    private static void testResults(){

    }

}
