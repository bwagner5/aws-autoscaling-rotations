package edu.gmu.cs

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3ObjectSummary
import groovy.io.FileType
import groovy.util.logging.Log
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam

import javax.servlet.http.HttpServletResponse

@Controller
@Log
class DownloadController {

    @Value('${upload.location}')
    protected String downloadDir

    @Value('${cloud.aws.credentials.accessKey}')
    private String awsAccessKey

    @Value('${cloud.aws.credentials.secretKey}')
    private String aws_secret_key

    @RequestMapping(value = '/download', method = RequestMethod.GET)
    public String fileListing(Model model){
        List<Map> fileList = []
        File directory = new File(downloadDir)
        directory.eachFileRecurse (FileType.FILES){ File file ->
            fileList << [name: file.name, size: file.size(), lastModified: new Date(file.lastModified())]
        }
        model.addAttribute('instanceId', MainController.instanceId)
        model.addAttribute('fileList', fileList)
        return 'download'
    }

    @RequestMapping(value = "/download/{file_name:.+}", method = RequestMethod.GET)
    public void getFile(
            @PathVariable("file_name") String fileName,
            @RequestHeader Map<String, String> headers,
            HttpServletResponse response) {
        try {
            File file = new File(downloadDir + File.separator + fileName)
            InputStream is = FileUtils.openInputStream(file)
            println headers
            if(headers.range){
                println headers.range.replace('bytes=', '')
                is.skip(Long.valueOf(headers.range.replace('bytes=', '').split('-')[0]))
            }
            org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            log.info("Error writing file to output stream. Filename was '${fileName}'")
            throw new RuntimeException("IOError writing file to output stream");
        }

    }

    @RequestMapping(value = '/s3download', method = RequestMethod.GET)
    public String s3FileListing(Model model){
        AmazonS3Client s3 = new AmazonS3Client(new ProfileCredentialsProvider('gmu'))
        ObjectListing objectList = s3.listObjects('cs779')
        model.addAttribute('objectList', objectList.objectSummaries)
        return 'download'
    }

}

