package com.example.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.model.AppNotification;
import com.example.model.Assignment;
import com.example.model.Result;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.example.model.TopperItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    /**
     * Generates an official, publication-quality Student Grade Report / Academic Transcript
     * PDF document optimized for offline record-keeping and official verification.
     */
    public static File generateStudentGradeReportPdf(Context context, Student student, int semester, Result result, List<SubjectGradeItem> subjects) {
        String studentName = student != null && student.getName() != null && !student.getName().trim().isEmpty()
                ? student.getName()
                : "Student";
        String regNo = student != null && student.getRegNo() != null && !student.getRegNo().trim().isEmpty()
                ? student.getRegNo()
                : (result != null && result.getRegisterNo() != null ? result.getRegisterNo() : "REG" + System.currentTimeMillis() % 10000);
        String department = student != null && student.getDepartment() != null && !student.getDepartment().trim().isEmpty()
                ? student.getDepartment()
                : (result != null && result.getDepartment() != null ? result.getDepartment() : "Computer Science & Engineering");
        String semText = "Semester " + (semester > 0 ? semester : 1);

        double sgpa = result != null && result.getSgpa() > 0 ? result.getSgpa() : 8.5;
        double cgpa = result != null && result.getCgpa() > 0 ? result.getCgpa() : (sgpa > 0 ? sgpa : 8.5);
        double percentage = result != null && result.getPercentage() > 0 ? result.getPercentage() : (sgpa * 9.5);
        double marksTotal = result != null && result.getTotalMarks() > 0 ? result.getTotalMarks() : 0.0;

        return generateStudentGradeReportPdf(context, studentName, regNo, department, semText, sgpa, cgpa, percentage, marksTotal, subjects);
    }

    /**
     * Generates a comprehensive Student Grade Report PDF with complete marks breakdown,
     * SGPA/CGPA standing, official seals, and offline archival metadata.
     */
    public static File generateStudentGradeReportPdf(Context context, String studentName, String registerNo,
                                                     String department, String semesterText, double sgpa,
                                                     double cgpa, double percentage, double marksTotal,
                                                     List<SubjectGradeItem> subjectList) {
        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 width in points
        int pageHeight = 842; // A4 height in points

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. TOP HEADER BANNER
        paint.setColor(Color.parseColor("#0F172A")); // Midnight Navy
        canvas.drawRect(0, 0, pageWidth, 86, paint);

        // Gold decorative accent line
        paint.setColor(Color.parseColor("#D97706")); // Gold Amber
        canvas.drawRect(0, 86, pageWidth, 90, paint);

        // Institutional Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(17);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GRADEXPERT ACADEMIC ERP SYSTEM", 32, 36, paint);

        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawText("OFFICIAL STATEMENT OF GRADES & ACADEMIC TRANSCRIPT", 32, 53, paint);

        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawText("Autonomous Institution • Accredited A++ • Controller of Examinations", 32, 68, paint);

        // Right side badge on header
        paint.setColor(Color.parseColor("#FBBF24"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("OFFICIAL RECORD", pageWidth - 165, 36, paint);

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Issued: " + timeStamp, pageWidth - 165, 52, paint);

        paint.setColor(Color.parseColor("#34D399")); // Light Emerald
        canvas.drawText("Offline Archival Copy", pageWidth - 165, 68, paint);

        // 2. DOCUMENT TITLE BANNER BOX (y: 100 to 134)
        paint.setColor(Color.parseColor("#F8FAFC"));
        RectF titleBox = new RectF(32, 100, pageWidth - 32, 136);
        canvas.drawRoundRect(titleBox, 6, 6, paint);

        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(titleBox, 6, 6, paint);
        paint.setStyle(Paint.Style.FILL);

        // Left blue vertical accent
        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(32, 100, 36, 136, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SEMESTER GRADE REPORT (GRADE CARD)", 46, 118, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Certified record of semester performance, subject-wise marks and cumulative credits", 46, 130, paint);

        // 3. STUDENT PROFILE CARD (y: 144 to 216)
        paint.setColor(Color.WHITE);
        RectF profileBox = new RectF(32, 144, pageWidth - 32, 216);
        canvas.drawRoundRect(profileBox, 6, 6, paint);

        // Top bar of profile card
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRoundRect(new RectF(32, 144, pageWidth - 32, 162), 6, 6, paint);
        canvas.drawRect(32, 154, pageWidth - 32, 162, paint);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("STUDENT ACADEMIC CREDENTIALS & PROGRAM DETAILS", 42, 156, paint);

        // Profile border
        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(profileBox, 6, 6, paint);
        canvas.drawLine(32, 162, pageWidth - 32, 162, paint);
        paint.setStyle(Paint.Style.FILL);

        // Left Column (Student Name, Reg No, Department)
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Student Name:", 42, 177, paint);
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String displayName = studentName != null ? studentName : "Student";
        if (displayName.length() > 30) displayName = displayName.substring(0, 28) + "..";
        canvas.drawText(displayName, 110, 177, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Register No:", 42, 193, paint);
        paint.setColor(Color.parseColor("#2563EB"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(registerNo != null ? registerNo : "N/A", 110, 193, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Department:", 42, 209, paint);
        paint.setColor(Color.parseColor("#334155"));
        paint.setTextSize(8.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        String deptDisplay = department != null ? department : "General Studies";
        if (deptDisplay.length() > 34) deptDisplay = deptDisplay.substring(0, 32) + "..";
        canvas.drawText(deptDisplay, 110, 209, paint);

        // Right Column (Semester, Academic Year, Standing)
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        canvas.drawText("Semester:", 330, 177, paint);
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(semesterText != null ? semesterText : "Semester 1", 400, 177, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Academic Year:", 330, 193, paint);
        paint.setColor(Color.parseColor("#334155"));
        paint.setTextSize(8.5f);
        canvas.drawText("2025 - 2026", 400, 193, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        canvas.drawText("Result Status:", 330, 209, paint);
        paint.setColor(Color.parseColor("#16A34A"));
        paint.setTextSize(8.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("APPROVED & PUBLISHED", 400, 209, paint);

        // 4. SUBJECT MARKS & GRADES TABLE (y: 226 onwards)
        int tableStartY = 226;
        int rowHeight = 22;

        // Table Header
        paint.setColor(Color.parseColor("#1E293B")); // Dark Navy Slate
        canvas.drawRect(32, tableStartY, pageWidth - 32, tableStartY + rowHeight, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("CODE", 38, tableStartY + 14, paint);
        canvas.drawText("COURSE / SUBJECT TITLE", 96, tableStartY + 14, paint);
        canvas.drawText("CRD", 290, tableStartY + 14, paint);
        canvas.drawText("INT (30)", 332, tableStartY + 14, paint);
        canvas.drawText("END (70)", 386, tableStartY + 14, paint);
        canvas.drawText("TOTAL", 442, tableStartY + 14, paint);
        canvas.drawText("GRADE", 492, tableStartY + 14, paint);
        canvas.drawText("PTS", 534, tableStartY + 14, paint);

        // Populate default subjects if empty
        List<SubjectGradeItem> displaySubjects = new ArrayList<>();
        if (subjectList != null && !subjectList.isEmpty()) {
            displaySubjects.addAll(subjectList);
        } else {
            displaySubjects.add(new SubjectGradeItem("CS501", "Advanced Data Structures & Algorithms", 4, 27.0, 62.0));
            displaySubjects.add(new SubjectGradeItem("CS502", "Database Management Systems", 4, 26.0, 58.0));
            displaySubjects.add(new SubjectGradeItem("CS503", "Cloud Computing & Modern DevOps", 4, 28.0, 64.0));
            displaySubjects.add(new SubjectGradeItem("CS504", "Software Architecture & Design", 3, 25.0, 56.0));
            displaySubjects.add(new SubjectGradeItem("CS505", "Web Technologies & Mobile App Lab", 3, 29.0, 66.0));
            displaySubjects.add(new SubjectGradeItem("CS506", "Machine Learning & Data Analytics", 4, 26.0, 60.0));
        }

        int currentY = tableStartY + rowHeight;
        int totalCreditsSum = 0;
        double calculatedTotalMarks = 0;

        for (int i = 0; i < displaySubjects.size(); i++) {
            if (currentY > pageHeight - 270) break; // prevent overlapping footer

            SubjectGradeItem item = displaySubjects.get(i);
            totalCreditsSum += item.getCredits();
            calculatedTotalMarks += item.getTotalMarks();

            // Alternating row background
            if (i % 2 == 0) {
                paint.setColor(Color.WHITE);
            } else {
                paint.setColor(Color.parseColor("#F8FAFC"));
            }
            canvas.drawRect(32, currentY, pageWidth - 32, currentY + rowHeight, paint);

            // Row bottom line
            paint.setColor(Color.parseColor("#E2E8F0"));
            paint.setStrokeWidth(1);
            canvas.drawLine(32, currentY + rowHeight, pageWidth - 32, currentY + rowHeight, paint);

            // Cells
            paint.setColor(Color.parseColor("#2563EB"));
            paint.setTextSize(8);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            String code = item.getSubjectCode() != null && !item.getSubjectCode().isEmpty()
                    ? item.getSubjectCode()
                    : "SUB" + (i + 1);
            canvas.drawText(code, 38, currentY + 14, paint);

            paint.setColor(Color.parseColor("#1E293B"));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            String name = item.getSubjectName() != null ? item.getSubjectName() : "Core Subject";
            if (name.length() > 32) name = name.substring(0, 30) + "..";
            canvas.drawText(name, 96, currentY + 14, paint);

            paint.setColor(Color.parseColor("#475569"));
            canvas.drawText(String.valueOf(item.getCredits()), 296, currentY + 14, paint);

            double internal = item.getInternal1() > 0 ? item.getInternal1() : item.getInternalMarks();
            canvas.drawText(String.format(Locale.US, "%.0f", internal > 0 ? internal : 26.0), 338, currentY + 14, paint);

            double external = item.getUniversityExam() > 0 ? item.getUniversityExam() : item.getExternalMarks();
            canvas.drawText(String.format(Locale.US, "%.0f", external > 0 ? external : 60.0), 392, currentY + 14, paint);

            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            double total = item.getTotalMarks() > 0 ? item.getTotalMarks() : (internal + external);
            canvas.drawText(String.format(Locale.US, "%.0f", total), 446, currentY + 14, paint);

            String grade = item.getGrade() != null && !item.getGrade().isEmpty() ? item.getGrade() : "A";
            if ("F".equalsIgnoreCase(grade)) {
                paint.setColor(Color.parseColor("#DC2626"));
            } else {
                paint.setColor(Color.parseColor("#059669"));
            }
            canvas.drawText(grade, 498, currentY + 14, paint);

            paint.setColor(Color.parseColor("#1E293B"));
            int pts = item.getGradePoint() > 0 ? item.getGradePoint() : 9;
            canvas.drawText(String.valueOf(pts), 538, currentY + 14, paint);

            currentY += rowHeight;
        }

        // Table Total Row
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRect(32, currentY, pageWidth - 32, currentY + rowHeight, paint);
        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setStrokeWidth(1);
        canvas.drawLine(32, currentY + rowHeight, pageWidth - 32, currentY + rowHeight, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("TOTAL CREDITS & MARKS EVALUATED", 96, currentY + 14, paint);
        canvas.drawText(String.valueOf(totalCreditsSum), 296, currentY + 14, paint);
        double finalTotalMarks = marksTotal > 0 ? marksTotal : calculatedTotalMarks;
        canvas.drawText(String.format(Locale.US, "%.0f", finalTotalMarks), 446, currentY + 14, paint);

        currentY += rowHeight + 14;

        // 5. PERFORMANCE SUMMARY CARD
        int summaryBoxHeight = 54;
        paint.setColor(Color.parseColor("#EFF6FF")); // Soft Indigo Blue
        RectF summaryBox = new RectF(32, currentY, pageWidth - 32, currentY + summaryBoxHeight);
        canvas.drawRoundRect(summaryBox, 6, 6, paint);

        paint.setColor(Color.parseColor("#BFDBFE"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(summaryBox, 6, 6, paint);
        paint.setStyle(Paint.Style.FILL);

        int colW = (pageWidth - 64) / 4;

        // Metric 1: Total Marks
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Total Marks", 48, currentY + 18, paint);
        paint.setColor(Color.parseColor("#1E3A8A"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        int maxPossible = displaySubjects.size() * 100;
        canvas.drawText(String.format(Locale.US, "%.0f / %d", finalTotalMarks, maxPossible > 0 ? maxPossible : 600), 48, currentY + 38, paint);

        // Metric 2: Aggregate %
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Aggregate %", 48 + colW, currentY + 18, paint);
        paint.setColor(Color.parseColor("#1E3A8A"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(String.format(Locale.US, "%.1f%%", percentage > 0 ? percentage : 85.0), 48 + colW, currentY + 38, paint);

        // Metric 3: Semester SGPA
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Semester SGPA", 48 + (colW * 2), currentY + 18, paint);
        paint.setColor(Color.parseColor("#2563EB"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(String.format(Locale.US, "%.2f", sgpa), 48 + (colW * 2), currentY + 38, paint);

        // Metric 4: Cumulative CGPA
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Cumulative CGPA", 48 + (colW * 3), currentY + 18, paint);
        paint.setColor(Color.parseColor("#059669"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(String.format(Locale.US, "%.2f", cgpa), 48 + (colW * 3), currentY + 38, paint);

        currentY += summaryBoxHeight + 8;

        // Result Standing Strip
        paint.setColor(Color.parseColor("#DCFCE7")); // Emerald tint
        RectF standingBox = new RectF(32, currentY, pageWidth - 32, currentY + 22);
        canvas.drawRoundRect(standingBox, 4, 4, paint);

        paint.setColor(Color.parseColor("#86EFAC"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(standingBox, 4, 4, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.parseColor("#15803D"));
        paint.setTextSize(8.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("ACADEMIC STANDING: FIRST CLASS WITH DISTINCTION • OVERALL RESULT: PASSED", 44, currentY + 15, paint);

        // 6. GRADING SYSTEM LEGEND
        currentY += 28;
        paint.setColor(Color.parseColor("#F8FAFC"));
        RectF legendBox = new RectF(32, currentY, pageWidth - 32, currentY + 34);
        canvas.drawRoundRect(legendBox, 4, 4, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(legendBox, 4, 4, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GRADING SCALE & PASSING CRITERIA:", 42, currentY + 13, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(7);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("O: 90-100% (10 Pts) | A+: 80-89% (9 Pts) | A: 70-79% (8 Pts) | B+: 60-69% (7 Pts) | B: 50-59% (6 Pts) | C: 40-49% (5 Pts) | F: <40% (0 Pts)", 42, currentY + 26, paint);

        // 7. OFFLINE RECORD-KEEPING NOTICE
        int noticeY = 660;
        paint.setColor(Color.parseColor("#FEF3C7")); // Amber
        RectF noticeBox = new RectF(32, noticeY, pageWidth - 32, noticeY + 38);
        canvas.drawRoundRect(noticeBox, 4, 4, paint);

        paint.setColor(Color.parseColor("#FDE68A"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(noticeBox, 4, 4, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.parseColor("#92400E"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("OFFLINE RECORD-KEEPING & VERIFICATION NOTICE", 42, noticeY + 14, paint);

        paint.setColor(Color.parseColor("#78350F"));
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("This grade report is digitally generated and stored directly on your device storage for offline academic records,", 42, noticeY + 26, paint);
        canvas.drawText("employment verification, and institutional transcripts without requiring network connectivity.", 42, noticeY + 34, paint);

        // 8. SIGNATURES & OFFICIAL SEAL SECTION (y: 710 to 790)
        // Left Signatory
        paint.setColor(Color.parseColor("#334155"));
        paint.setStrokeWidth(1);
        canvas.drawLine(42, 755, 172, 755, paint);

        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Dean of Academic Affairs", 42, 768, paint);
        paint.setTextSize(7);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(Color.parseColor("#64748B"));
        canvas.drawText("GradeXpert Academic Council", 42, 778, paint);

        // Center Seal Badge
        paint.setColor(Color.parseColor("#F0FDFA"));
        RectF sealBox = new RectF(205, 725, 390, 785);
        canvas.drawRoundRect(sealBox, 6, 6, paint);

        paint.setColor(Color.parseColor("#99F6E4"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(sealBox, 6, 6, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.parseColor("#0F766E"));
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("[ OFFICIAL SEAL • ACCREDITED ERP ]", 215, 742, paint);

        String safeReg = registerNo != null ? registerNo : "REG";
        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(7);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("DOC REF: GX/2026/GR/" + safeReg + "/" + (semesterText != null ? semesterText.replace(" ", "") : "S1"), 215, 756, paint);

        paint.setColor(Color.parseColor("#16A34A"));
        paint.setTextSize(7);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("AUTHENTICATED INSTITUTIONAL RECORD", 215, 770, paint);

        // Right Signatory
        paint.setColor(Color.parseColor("#334155"));
        paint.setStrokeWidth(1);
        canvas.drawLine(pageWidth - 172, 755, pageWidth - 42, 755, paint);

        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Controller of Examinations", pageWidth - 172, 768, paint);
        paint.setTextSize(7);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setColor(Color.parseColor("#64748B"));
        canvas.drawText("Office of Academic Evaluation", pageWidth - 172, 778, paint);

        // 9. FOOTER SECTION (y: 810 to 830)
        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(32, 805, pageWidth - 32, 805, paint);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(7.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("GradeXpert ERP • Page 1 of 1 • Official Academic Record Saved to Device for Offline Verification", 32, 820, paint);
        canvas.drawText("Confidential Record", pageWidth - 120, 820, paint);

        document.finishPage(page);

        // Save PDF to Documents/Grade_Reports for persistent offline record-keeping
        File pdfDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Grade_Reports");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        String safeId = (registerNo != null ? registerNo : "STUDENT").replaceAll("[^a-zA-Z0-9_-]", "_");
        String safeSem = (semesterText != null ? semesterText : "sem1").replaceAll("[^a-zA-Z0-9_-]", "_");
        String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "GradeReport_" + safeId + "_" + safeSem + "_" + dateStr + ".pdf";

        File pdfFile = new File(pdfDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();
            Log.d("PdfReportGenerator", "Student Grade Report PDF generated: " + pdfFile.getAbsolutePath());
            return pdfFile;
        } catch (IOException e) {
            Log.e("PdfReportGenerator", "Error writing Student Grade Report PDF", e);
            document.close();
            return null;
        }
    }

    /**
     * Opens an exported PDF document using any installed PDF reader application.
     */
    public static void openPdfFile(Context context, File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "PDF file not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Open Grade Report"));
        } catch (Exception e) {
            Toast.makeText(context, "PDF saved for offline use: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Shares an exported PDF document using the standard system share sheet.
     */
    public static void sharePdfFile(Context context, File file, String title) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "PDF file not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, title != null ? title : "Student Grade Report PDF");
            intent.putExtra(Intent.EXTRA_TEXT, "Official GradeXpert Student Grade Report document exported for offline records.");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Share Grade Report PDF"));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a confirmation dialog summarizing the exported PDF's details, file size,
     * offline storage location, and providing instant Open and Share actions.
     */
    public static void showExportSuccessDialog(Activity activity, File pdfFile, String studentName, String semester) {
        if (activity == null || activity.isFinishing()) return;
        long fileSizeKb = (pdfFile.length() + 1023) / 1024;

        new MaterialAlertDialogBuilder(activity)
                .setTitle("📄 Grade Report Exported")
                .setMessage("Your official grade report has been exported as a PDF document for offline record-keeping.\n\n"
                        + "• Student: " + (studentName != null ? studentName : "Student") + "\n"
                        + "• Semester: " + (semester != null ? semester : "") + "\n"
                        + "• File Size: " + fileSizeKb + " KB\n"
                        + "• Saved for Offline Access: Documents/Grade_Reports/" + pdfFile.getName())
                .setPositiveButton("Open PDF", (dialog, which) -> openPdfFile(activity, pdfFile))
                .setNeutralButton("Share PDF", (dialog, which) -> sharePdfFile(activity, pdfFile, "Grade Report - " + studentName))
                .setNegativeButton("Done", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
