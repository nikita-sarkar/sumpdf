package com.example.pdfsum.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PDFService {

    private final HuggingFaceService huggingFaceService;

    public Map<String, Object> processPDF(MultipartFile file)
    {
        String fileText =  extractText(file);

//        List<String> highlights = extractHighlights(file);
//
//        String highlightsCombined = String.join("\n", highlights);
//        String summary = huggingFaceService.summarize(highlightsCombined, 150);
//        Map<String, Object> response = new HashMap<>();
//
//        response.put("highlights", highlightsCombined);
//        response.put("summary", summary);

        String summary = huggingFaceService.summarize("summarize: "+fileText, 150);
        Map<String, Object> response = new HashMap<>();

        response.put("fileText", fileText);
        response.put("summary", summary);

        return response;
    }

    private String extractText(MultipartFile file)
    {
        try(PDDocument document = Loader.loadPDF(file.getBytes()))
        {
            PDFTextStripper stripper = new PDFTextStripper();
            String highlightedText = stripper.getText(document);

            return highlightedText;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error processing PDF", e);
        }
    }

    private List<String> extractHighlights(MultipartFile file)
    {
        List<String> highlights = new ArrayList<>();

        try(PDDocument document = Loader.loadPDF(file.getBytes()))
        {
            int pageIndex = 0;
            for(PDPage page : document.getPages())
            {
                List<PDAnnotation> annotations = page.getAnnotations();
                for(PDAnnotation annotation: annotations)
                {
                    //TODO: should i use instanceof here? hmmmm
                    if(annotation instanceof PDAnnotationTextMarkup && annotation.getSubtype().equals("Highlight"))
                    {
                        PDAnnotationTextMarkup textMarkup = (PDAnnotationTextMarkup) annotation;
                        float[] quads = textMarkup.getQuadPoints();
                        if(quads == null)
                            continue;

                        for(int i=0; i<quads.length; i+=8)
                        {
                            //TODO: can i move this to a function?
                            float x1 = Math.min(Math.min(quads[i], quads[i+2]), Math.min(quads[i+4], quads[i+6]));
                            float x2 = Math.min(Math.max(quads[i], quads[i+2]), Math.max(quads[i+4], quads[i+6]));
                            float y1 = Math.min(Math.min(quads[i+1], quads[i+3]), Math.min(quads[i+5], quads[i+7]));
                            float y2 = Math.min(Math.max(quads[i+1], quads[i+3]), Math.max(quads[i+5], quads[i+7]));

                            Rectangle2D.Float rect = new Rectangle2D.Float(x1, y1, x2-x1, y2-y1);
                            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                            String regionName = "highlight-" + pageIndex + "-" + i;
                            stripper.addRegion(regionName, rect);
                            stripper.extractRegions(page);

                            String text = stripper.getTextForRegion(regionName).trim();

                            if(!text.isEmpty())
                                highlights.add(text);
                        }
                    }
                }

                pageIndex++;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error processing PDF", e);
        }
        return highlights;
    }
}
