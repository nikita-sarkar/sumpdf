package com.example.pdfsum.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PDFService {

    public static Map<String, Object> processPDF(MultipartFile file)
    {
        Map<String, Object> response = new HashMap<>();

        try(PDDocument document = Loader.loadPDF(file.getBytes()))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String highlightedText = stripper.getText(document);

            response.put("highlights", highlightedText);
            response.put("summary", "TODO: will be adding soon :)");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error processing PDF", e);
        }

        return response;
    }
}
