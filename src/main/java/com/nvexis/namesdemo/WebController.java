package com.nvexis.namesdemo;

import com.itextpdf.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController()
public class WebController {

    @Autowired
    ServiceOne serviceOne;


    @GetMapping(value = "/getPDF/{type}",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public void getDoc(HttpServletResponse response, @PathVariable String type) throws IOException, DocumentException, SQLException {
        serviceOne.getPDF(response.getOutputStream());
    }

    @GetMapping(value = "/getXlsx")
    public void getXLSX(HttpServletResponse response) throws IOException, IllegalAccessException {
        serviceOne.getXLSX(response);
    }

    @GetMapping(value = "/zip")
    public void getZip(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        StreamingResponseBody stream = out -> {
            ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

            ByteArrayOutputStream outStream1 = new ByteArrayOutputStream();
            ByteArrayOutputStream outStream2 = new ByteArrayOutputStream();

            try {
                serviceOne.getPDF(outStream1);
            } catch (DocumentException e) {
                throw new Error(e);
            }
            zipOutputStream.putNextEntry(new ZipEntry("document.pdf"));
            zipOutputStream.write(outStream1.toByteArray());

            try {
                serviceOne.getXLSX(response);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }

            zipOutputStream.putNextEntry(new ZipEntry("document.xlsx"));
            zipOutputStream.write(outStream2.toByteArray());

            zipOutputStream.close();
        };
        stream.writeTo(response.getOutputStream());

    }

    @GetMapping(value = "/get/{type}")
    public void getFile(HttpServletResponse response, @PathVariable String type) throws IOException, InvocationTargetException, IllegalAccessException, SQLException, NoSuchMethodException {

        String contentType = (String) serviceOne.getClass()
                .getMethod(String.format("get%s", type.toUpperCase()), HttpServletResponse.class).invoke(serviceOne, response);


    }

}
