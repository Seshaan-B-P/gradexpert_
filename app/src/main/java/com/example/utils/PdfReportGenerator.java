package com.example.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.model.AppNotification;
import com.example.model.Assignment;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.example.model.TopperItem;

public class PdfReportGenerator {

    public enum ReportType {
        STUDENT_CARD,
        DEPARTMENT,
        CGPA,
        ATTENDANCE,
        TOPPER
    }

    public static File generatePdf(Context context, ReportType type, String title, String subtitle, List<String[]> tableData, String summaryText) {
        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 dimensions in points (72 dpi)
        int pageHeight = 842;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Header Banner Background
        paint.setColor(Color.parseColor("#1B5E20")); // Dark Green Theme
        canvas.drawRect(0, 0, pageWidth, 90, paint);

        // Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GRADEXPERT ACADEMIC ERP", 20, 38, paint);

        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("OFFICIAL ACADEMIC REPORT & TRANSCRIPT SYSTEM", 20, 58, paint);

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        paint.setTextSize(9);
        canvas.drawText("Generated: " + timeStamp, pageWidth - 180, 58, paint);

        // Subheader Box
        paint.setColor(Color.parseColor("#E8F5E9"));
        canvas.drawRect(20, 105, pageWidth - 20, 155, paint);

        paint.setColor(Color.parseColor("#1B5E20"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(title != null ? title.toUpperCase() : "ACADEMIC REPORT", 32, 128, paint);

        paint.setColor(Color.parseColor("#333333"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(subtitle != null ? subtitle : "Official document for academic records", 32, 145, paint);

        // Summary Text Section
        int currentY = 175;
        if (summaryText != null && !summaryText.isEmpty()) {
            paint.setColor(Color.parseColor("#F5F5F5"));
            canvas.drawRect(20, currentY, pageWidth - 20, currentY + 45, paint);

            paint.setColor(Color.parseColor("#222222"));
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            String[] lines = summaryText.split("\n");
            int lineY = currentY + 18;
            for (String line : lines) {
                canvas.drawText(line, 32, lineY, paint);
                lineY += 14;
            }
            currentY += 60;
        }

        // Draw Table Data
        if (tableData != null && !tableData.isEmpty()) {
            int startX = 20;
            int tableWidth = pageWidth - 40;
            int rowHeight = 26;

            // Header row
            String[] headers = tableData.get(0);
            int colCount = headers.length;
            int colWidth = tableWidth / Math.max(colCount, 1);

            paint.setColor(Color.parseColor("#2E7D32"));
            canvas.drawRect(startX, currentY, startX + tableWidth, currentY + rowHeight, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            for (int c = 0; c < colCount; c++) {
                canvas.drawText(headers[c], startX + (c * colWidth) + 8, currentY + 17, paint);
            }

            currentY += rowHeight;

            // Data rows
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            for (int r = 1; r < tableData.size(); r++) {
                if (currentY > pageHeight - 120) break; // prevent page overflow

                // Alternating row background
                if (r % 2 == 0) {
                    paint.setColor(Color.parseColor("#FAFAFA"));
                } else {
                    paint.setColor(Color.WHITE);
                }
                canvas.drawRect(startX, currentY, startX + tableWidth, currentY + rowHeight, paint);

                // Row bottom border line
                paint.setColor(Color.parseColor("#E0E0E0"));
                canvas.drawLine(startX, currentY + rowHeight, startX + tableWidth, currentY + rowHeight, paint);

                paint.setColor(Color.parseColor("#333333"));
                paint.setTextSize(9);

                String[] row = tableData.get(r);
                for (int c = 0; c < Math.min(row.length, colCount); c++) {
                    String cellVal = row[c] != null ? row[c] : "";
                    canvas.drawText(cellVal, startX + (c * colWidth) + 8, currentY + 17, paint);
                }

                currentY += rowHeight;
            }
        }

        // Footer & Official Seal Section
        int footerY = pageHeight - 90;
        paint.setColor(Color.parseColor("#CCCCCC"));
        canvas.drawLine(20, footerY, pageWidth - 20, footerY, paint);

        paint.setColor(Color.parseColor("#666666"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("This document is verified and generated by GradeXpert Academic ERP Engine.", 20, footerY + 20, paint);
        canvas.drawText("Page 1 of 1  |  Confidential Student Record", 20, footerY + 35, paint);

        // Signature Line
        paint.setColor(Color.parseColor("#333333"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawLine(pageWidth - 180, footerY + 30, pageWidth - 20, footerY + 30, paint);
        canvas.drawText("Controller of Examinations", pageWidth - 170, footerY + 45, paint);

        document.finishPage(page);

        // Save PDF file locally
        String filePrefix = type.name().toLowerCase() + "_report_";
        String fileName = filePrefix + System.currentTimeMillis() + ".pdf";

        File pdfDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (pdfDir == null) {
            pdfDir = context.getCacheDir();
        }
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        File pdfFile = new File(pdfDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();
            return pdfFile;
        } catch (IOException e) {
            e.printStackTrace();
            document.close();
            return null;
        }
    }
}
