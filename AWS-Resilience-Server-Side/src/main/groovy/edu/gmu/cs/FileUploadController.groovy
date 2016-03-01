package edu.gmu.cs

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.omg.CORBA.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.ui.Model
import sun.misc.BASE64Encoder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile

import java.util.logging.Level;

@CompileStatic
@Controller
@Log(value = 'LOGGER')
public class FileUploadController {

    private static final Random random = new Random()

    @Value('${upload.location}')
    protected String UPLOAD_DIR

    @Value('${multipart.location}')
    protected String TEMP_UPLOAD_DIR

    @Value('${cloud.aws.credentials.accessKey}')
    private String awsAccessKey

    @Value('${cloud.aws.credentials.secretKey}')
    private String aws_secret_key

    private String redirectString = 'http://s3resilience.brandonwagner.info'

    private final String POLICY = """{"expiration": "2017-01-01T00:00:00Z",
  "conditions": [
    {"bucket": "cs779"},
    ["starts-with", "\$key", ""],
    {"acl": "public-read"},
    {"success_action_redirect": "$redirectString"},
    ["content-length-range", 0, 100048576]
  ]
}"""

    @RequestMapping(value="/upload", method=RequestMethod.GET)
    public String provideUploadInfo(Model model) {
        model.addAttribute('instanceId', MainController.instanceId)
        return "upload";
    }

    @RequestMapping(value="/upload", method=RequestMethod.POST)
    public @ResponseBody String handleFileUpload(@RequestParam("file") MultipartFile file){
        String name = file.originalFilename
        File fileToUpload = new File(UPLOAD_DIR + File.separator + name)
        return "${file.transferTo(fileToUpload)}"
    }

    @RequestMapping(value = "/s3upload", method = RequestMethod.GET)
    public String displayS3Form(Model model){

        model.addAttribute('policy', new BASE64Encoder().encode(POLICY.getBytes("UTF-8")).replaceAll('\n', '').replaceAll('\r',''))
        model.addAttribute('accessKeyId', awsAccessKey)
        model.addAttribute('signature', getSignature())
        model.addAttribute('success_action_redirect', redirectString)
        return 'uploadS3'
    }

    private String getSignature(){

        String policy = (new BASE64Encoder()).encode(
                POLICY.getBytes("UTF-8")).replaceAll("\n","").replaceAll("\r","");

        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(
                aws_secret_key.getBytes("UTF-8"), "HmacSHA1"));
        String signature = (new BASE64Encoder()).encode(
                hmac.doFinal(policy.getBytes("UTF-8")))
                .replaceAll("\n", "");

        return signature
    }





}