package io.plotnik.piserver.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.plotnik.piserver.freewriting.FwException;
import io.plotnik.piserver.freewriting.dao.ApperyTag;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.client5.http.fluent.Request;

@Service
public class ApperyClient {

    private static final Logger log = LoggerFactory.getLogger(ApperyClient.class);

    @Value("${appery.db-id}")
    private String dbId;

    @Value("${appery.coll-name}")
    private String collName;

    final String apperyProtocol = "https";
    final String apperyHost = "api.appery.io";
    final String apperyPath = "/rest/1/db/collections/";

    ObjectMapper om = new ObjectMapper();

    URIBuilder getApperyUriBuilder() {
        return new URIBuilder()
            .setScheme(apperyProtocol).setHost(apperyHost).setPath(apperyPath + collName);
    }

    String json(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    void log_str(String str, String title) {
        log.info("\n---------- " + title + ": " + str);
        //log.info(str);
        //log.info("----------");
    }

    /**
     * Изменить теги для фрирайта в базе Аппери.
     */
    public OpResult updateTagList(String dstamp, Set<String> tagList) {
        try {
            /* Проверить, существует ли такой `dstamp`
             */
            URIBuilder uri = getApperyUriBuilder()
                .setParameter("where", json(Map.of("dstamp", dstamp)));
            String findResStr = Request.get(uri.build())
                .addHeader("X-Appery-Database-Id", dbId)
                .execute().returnContent().asString();

            log_str(findResStr, "findResStr");
            String bodyStr = json(Map.of(
                "dstamp", dstamp,
                "obj", json(tagList)));
            log_str(bodyStr, "bodyStr");

            ApperyTag[] findRes = om.readValue(findResStr, ApperyTag[].class);
            if (findRes.length == 0) {
                /* Создать новую запись.
                 */
                String createResStr = Request.post(getApperyUriBuilder().build())
                    .addHeader("X-Appery-Database-Id", dbId)
                    .bodyString(bodyStr, ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString();

                log_str(createResStr, "createResStr");

            } else {
                /* Поменять существующую запись.
                 */
                uri = getApperyUriBuilder()
                    .setPath(apperyPath + collName + "/" + findRes[0].get_id());
                //log.info("uri path: " + uri.getPath());
                String updateResStr = Request.put(uri.build())
                    .addHeader("X-Appery-Database-Id", dbId)
                    .bodyString(bodyStr, ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString();

                log_str(updateResStr, "updateResStr");
            }

            return new OpResult(true);

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return new OpResult(false);
        }
    }

    public ApperyTag[] loadNoteTags() throws FwException {
        try {
            String findResStr = Request.get(getApperyUriBuilder().build())
                .addHeader("X-Appery-Database-Id", dbId)
                .execute().returnContent().asString();
            log_str(findResStr, "findResStr");
            return om.readValue(findResStr, ApperyTag[].class);

        } catch (IOException | URISyntaxException e) {
            throw new FwException(e.getMessage());
        }
    }
}
