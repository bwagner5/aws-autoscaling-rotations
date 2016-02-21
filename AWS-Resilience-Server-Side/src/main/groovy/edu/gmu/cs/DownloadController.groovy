package edu.gmu.cs

import groovy.io.FileType
import groovy.util.logging.Log
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

import javax.servlet.http.HttpServletResponse

@Controller
@Log
class DownloadController {

    @Value('${upload.location}')
    protected String downloadDir

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
            HttpServletResponse response) {
        try {
            File file = new File(downloadDir + File.separator + fileName)
            InputStream is = FileUtils.openInputStream(file)
            org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            log.info("Error writing file to output stream. Filename was '${fileName}'")
            throw new RuntimeException("IOError writing file to output stream");
        }

    }

}

