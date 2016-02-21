package edu.gmu.cs

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.*

@Controller
class MainController {

    public static String instanceId

    @RequestMapping('/')
    public String index(Model model){
        Process instanceIdProcess
        try{
            if(!instanceId){
                instanceIdProcess = 'curl -L http://169.254.169.254/latest/meta-data/instance-id'.execute()
                instanceIdProcess.waitForOrKill(100)
                instanceId = instanceIdProcess.text
            }
        }catch(Exception e){
            instanceId = 'ifconfig | grep -Eo \'inet (addr:)?([0-9]*\\.){3}[0-9]*\' | grep -Eo \'([0-9]*\\.){3}[0-9]*\' | grep -v \'127.0.0.1\''.execute().text
        }

        model.addAttribute('instanceId', instanceId)
        return 'index'
    }


}

