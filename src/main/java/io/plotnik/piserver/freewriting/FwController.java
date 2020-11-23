package io.plotnik.piserver.freewriting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@RestController
public class FwController {

    private static final Logger log = LoggerFactory.getLogger(FwController.class);

    @Value("${home.path}")
    private String homePath;

    @Value("${freewriting.path}")
    private String fwPath;

    Freewriting fw;

    @PostConstruct
    public void init() {
        try {
            fw = new Freewriting(homePath + fwPath);
        } catch (FwException e) {
            log.info("[FwException] " + e.getMessage());
        }
    }

    @RequestMapping(value = "/fw", method = RequestMethod.GET)
    public FwDate getPage(@RequestParam(name = "d", defaultValue = "") String datestr) {
        return null;
    }

}
