package com.example.pdfsum.controller;

import com.example.pdfsum.service.PDFService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PDFController {
        private final PDFService pdfService;

        @PostMapping("/upload")
        public ResponseEntity<?> uploadPDF(@RequestParam("file")MultipartFile file)
        {
            return ResponseEntity.ok(PDFService.processPDF(file));
        }
}
