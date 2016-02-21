package edu.gmu.cs

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.ui.Model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@CompileStatic
@Controller
public class FileUploadController {

    @Value('${upload.location}')
    protected String UPLOAD_DIR

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

//        if (!file.isEmpty()) {
//            try {
//                BufferedOutputStream stream =
//                        new BufferedOutputStream(new FileOutputStream(fileToUpload));
//                BufferedInputStream input = new BufferedInputStream(file.inputStream)
//
//                while(input.read(buffer)){
//                    stream.write(buffer)
//                }
//                stream.close()
//                input.close()
//                return "You successfully uploaded " + name + "!";
//            } catch (Exception e) {
//                return "You failed to upload " + name + " => " + e.getMessage();
//            }
//        } else {
//            return "You failed to upload " + name + " because the file was empty.";
//        }
    }

    public void replicate(List<String> hosts){

        List<Thread> replicationThreads = []
        hosts.each { String host ->
            replicationThreads.add(
                    new Thread({

                    })
            )
        }

    }

}