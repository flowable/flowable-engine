package org.flowable.ui.modeler.rest.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.io.PatternFilenameFilter;
import com.sun.nio.zipfs.ZipFileSystem;
import org.apache.commons.io.IOUtils;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/app")
public class ModelExportResource {
    private final Logger LOGGER = LoggerFactory.getLogger(ModelExportResource.class);

    @PostMapping(value = "/rest/model/export", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public FileSystemResource exportToSvg(@RequestBody JsonNode body, HttpServletResponse response) {
        //TODO: add more checks for diffrent formats in the future for now we go for svg only.
        //TODO: ...?
        LOGGER.info("Handeling request with body {}", body.toString());
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        date = date.replaceAll(":","-");

        File tempDirectory = null;


        try {
            tempDirectory = Files.createTempDirectory(date + "-svg-export", new FileAttribute[]{}).toFile();
            LOGGER.info("Tempdirectory created at {}", tempDirectory.getAbsolutePath());
            ArrayNode x = (ArrayNode) body;
            for (JsonNode jsonNode : x) {
                File svgFile = Files.createTempFile(tempDirectory.toPath(), jsonNode.path("elementId").asText(), ".svg").toFile();
                LOGGER.info("Temp file created at {}", svgFile.getAbsolutePath());

                //TODO: character encoding might be a problem for some languages...
                IOUtils.write(jsonNode.path("svg").asText(), new FileOutputStream(svgFile), "UTF-8");

            }

            //TODO: create a zip file of the temp directory content.
            Path zipFile = Paths.get(tempDirectory.getPath(),date + "-svg-export.zip");
            try (FileSystem zipfs = createZipFileSystem(zipFile,true)) {
                File[] svgs = tempDirectory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".svg");
                    }
                });
                for (File svg : svgs) {
                    Path source = svg.toPath();
                    Path target = zipfs.getPath(svg.getName());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            response.setHeader("x-export-filename", zipFile.getFileName().toString());
            return new FileSystemResource(zipFile.toFile());
        } catch (Exception ex) {
            LOGGER.error("An exception took place during the export", ex);
            throw new InternalServerErrorException("Failed to export model svg", ex);
        } finally {
            //TODO: do we need to remove the temp directories?
        }

    }

    private static FileSystem createZipFileSystem(Path path, boolean create) throws IOException {
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }
}
