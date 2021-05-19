package com.nvexis.namesdemo;


import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service
public class ServiceOne {

    @Autowired
    DataSource ds;


    private CyclicBarrier barrier;
    private Object[] people1;

    private static final int numThreads = 8;
    private static final ExecutorService es = Executors.newFixedThreadPool(numThreads);


    public void report() throws SQLException {
        int size = 0;

        int portion;

        //Phase 1: get counter
        try (Connection con = this.ds.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM names"); ResultSet resultSet = ps.executeQuery();
        ) {
            if (resultSet.next()) {
                size = resultSet.getInt(1);
            }
        }  // try

        if (0 == size) {
            throw new Error("can not continue....");
        }

        people1 = new Person[size + 1];
        portion = size / numThreads;
        barrier = new CyclicBarrier(numThreads + 1);


        // Phase 2 start parallel threads
        for (int i = 0; i < numThreads; i++) {
            es.submit(new ProcessDatabaseShards(portion,i*portion));
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new Error(e);
        }

        //Phase 3: print objects & summary
        try (Writer printWriter = new BufferedWriter(new FileWriter("text.txt"), 4096);) {
            for (Object person : people1) {

                if (person != null) {
                    printWriter.write(objectToString(person) + System.getProperty("line.separator"));

                }
            }
            printWriter.write("Total number of people is " + size);
            System.out.println("DONE!!!");

        } catch (IOException e) {
            throw new Error();
        }

    }

    public void getPDF(HttpServletResponse response) throws IOException, DocumentException {
        response.setContentType("application/pdf");
        getPDF(response.getOutputStream());

    }
    public void getPDF(OutputStream os) throws IOException, DocumentException {

        Document document = new Document();
        PdfWriter.getInstance(document, os);
        Image image = Image.getInstance("thumbnail.png");
        image.scaleAbsolute(500, 200);
        document.open();
        document.add(image);
        Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
        int i = 0;
        for (Object person : people1) {

            if (i++ < 20) document.add(new Paragraph(String.format("PERSON : %s  %n", objectToString(person))));
        }
        document.close();

    }


    public void getXLSX(HttpServletResponse response ) throws IOException, IllegalAccessException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        getXLSX(response.getOutputStream());

    }
    private void getXLSX(OutputStream os) throws IOException, IllegalAccessException {
        XSSFWorkbook workbook = new XSSFWorkbook();


        Sheet sheet = workbook.createSheet("People");
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);

        Row header = sheet.createRow(0);

        CellStyle headerStyle = setHeaderStyle(workbook);
        XSSFFont font = setFont(workbook);

        headerStyle.setFont(font);

        Field[] declaredFields = Person.class.getDeclaredFields();
        int p =0;
        Cell headerCell1;

        for (Field field: declaredFields ) {
            field.setAccessible(true);
            headerCell1=header.createCell(p++);
            headerCell1.setCellValue(field.getName());
            headerCell1.setCellStyle(headerStyle);
        }

        int temp;
        for (int i = 0; i < people1.length / 2; i++) {
            temp=0;
            Row row = sheet.createRow(i+2);
            for (Field field:declaredFields) {
                field.setAccessible(true);
                Cell cell = row.createCell(temp++);
                cell.setCellValue(field.get(people1[i]).toString());
            }

        }

        workbook.write(os);
        workbook.close();

    }

    private String objectToString(Object o){
        Field[] declaredFields = o.getClass().getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        String format="";
        for (Field f :  declaredFields) {
            f.setAccessible(true);
            try {
                format = String.format("%s: %s, ",f.getName(),f.get(o));
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
            sb.append(format);
        }

        return sb.substring(0,sb.length()-2);
    }

    private CellStyle setHeaderStyle(XSSFWorkbook workbook){
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THICK);
        headerStyle.setBorderRight(BorderStyle.THICK);

        return headerStyle;
    }

    private XSSFFont setFont(XSSFWorkbook workbook){
        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        return font;
    }


    public void getZIP(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        OutputStream  os = response.getOutputStream();

        StreamingResponseBody stream = out -> {
            ZipOutputStream zipOutputStream = new ZipOutputStream(os);

            ByteArrayOutputStream outStream1 = new ByteArrayOutputStream();
            ByteArrayOutputStream outStream2 = new ByteArrayOutputStream();

            try {
                getPDF(outStream1);
            } catch (DocumentException e) {
                throw  new Error(e);
            }
            zipOutputStream.putNextEntry(new ZipEntry("document.pdf"));
            zipOutputStream.write(outStream1.toByteArray());

            try {
                getXLSX(outStream2);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }

            zipOutputStream.putNextEntry(new ZipEntry("document.xlsx"));
            zipOutputStream.write(outStream2.toByteArray());

            zipOutputStream.close();
        };
        stream.writeTo(os);

    }


    class ProcessDatabaseShards implements Runnable {
        private final int portion;
        private int begin;

        ProcessDatabaseShards(int portion, int begin) {
            this.portion = portion;
            this.begin = begin;
        }

        @Override
        public void run() {
            String query = "SELECT id, name FROM names LIMIT " + portion + " offset  " + begin;
            try (Connection con = ds.getConnection();
                 PreparedStatement pr = con.prepareStatement(query); ResultSet resultSet = pr.executeQuery();) {
                while (resultSet.next()) {
                    Person person = new Person(resultSet.getInt("id"), resultSet.getString("name"));
                    if (begin < people1.length - 1)
                        people1[this.begin++] = person;
                }
            } catch (SQLException e) {
                throw new Error();
            } finally {
                try {
                    barrier.await();
                } catch (BrokenBarrierException | InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("here");
                }
            }
        }
    }

}
