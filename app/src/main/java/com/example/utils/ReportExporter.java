package com.example.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.model.AcademicReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility for exporting dynamic GradeXpert Academic Performance Reports as PDF and CSV files.
 */
public class ReportExporter {

    private static final String TAG = "ReportExporter";

    /**
     * Exports academic report data as a structured PDF document.
     */
    public static File exportToPdf(Context context, List<AcademicReport> reports, String dept, String sem, String subject, String year) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 Size
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();

        // Header Background
        titlePaint.setColor(Color.parseColor("#4F46E5")); // Indigo
        canvas.drawRect(0, 0, 595, 90, titlePaint);

        // Header Text
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(20f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("GRADEXPERT ACADEMIC REPORT", 20, 42, titlePaint);

        titlePaint.setTextSize(11f);
        titlePaint.setFakeBoldText(false);
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()), 20, 68, titlePaint);

        // Metadata Info
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(11f);
        int y = 120;
        canvas.drawText("Department: " + (dept != null ? dept : "All"), 20, y, paint);
        canvas.drawText("Semester: " + (sem != null ? sem : "All"), 300, y, paint);
        y += 20;
        canvas.drawText("Subject: " + (subject != null ? subject : "All"), 20, y, paint);
        canvas.drawText("Academic Year: " + (year != null ? year : "2026-27"), 300, y, paint);
        y += 25;

        // Divider Line
        paint.setStrokeWidth(1f);
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(20, y, 575, y, paint);
        y += 25;

        // Table Header
        headerPaint.setColor(Color.parseColor("#1E293B"));
        headerPaint.setTextSize(11f);
        headerPaint.setFakeBoldText(true);

        canvas.drawText("Reg No", 20, y, headerPaint);
        canvas.drawText("Student Name", 110, y, headerPaint);
        canvas.drawText("Marks %", 310, y, headerPaint);
        canvas.drawText("Attendance", 400, y, headerPaint);
        canvas.drawText("SGPA", 490, y, headerPaint);
        y += 15;

        canvas.drawLine(20, y, 575, y, paint);
        y += 20;

        // Data Rows
        paint.setColor(Color.BLACK);
        paint.setTextSize(10f);
        paint.setFakeBoldText(false);

        if (reports != null) {
            int count = Math.min(reports.size(), 25);
            for (int i = 0; i < count; i++) {
                AcademicReport ar = reports.get(i);
                canvas.drawText(ar.getRegisterNo(), 20, y, paint);

                String name = ar.getStudentName();
                if (name.length() > 22) name = name.substring(0, 20) + "..";
                canvas.drawText(name, 110, y, paint);

                canvas.drawText(String.format(Locale.US, "%.1f%%", ar.getAverageMarksPercentage()), 310, y, paint);
                canvas.drawText(String.format(Locale.US, "%.1f%%", ar.getAttendancePercentage()), 400, y, paint);
                canvas.drawText(String.format(Locale.US, "%.2f", ar.getSgpa()), 490, y, paint);

                y += 22;
                if (y > 800) break;
            }
        }

        document.finishPage(page);

        File pdfFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GradeXpert_Academic_Report_" + System.currentTimeMillis() + ".pdf");
        try {
            document.writeTo(new FileOutputStream(pdfFile));
            document.close();
            Log.d(TAG, "PDF Exported successfully to: " + pdfFile.getAbsolutePath());
            return pdfFile;
        } catch (Exception e) {
            Log.e(TAG, "Error writing PDF file", e);
            document.close();
            return null;
        }
    }

    /**
     * Exports academic report data as a structured CSV file.
     */
    public static File exportToCsv(Context context, List<AcademicReport> reports, String dept, String sem, String subject, String year) {
        File csvFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GradeXpert_Academic_Report_" + System.currentTimeMillis() + ".csv");
        try (FileWriter writer = new FileWriter(csvFile)) {

            // CSV Header
            writer.append("Register No,Student Name,Department,Semester,Marks Percentage,Attendance Percentage,SGPA,Status\n");

            if (reports != null) {
                for (AcademicReport ar : reports) {
                    writer.append(escapeCsv(ar.getRegisterNo())).append(",");
                    writer.append(escapeCsv(ar.getStudentName())).append(",");
                    writer.append(escapeCsv(ar.getDepartment())).append(",");
                    writer.append(escapeCsv(ar.getSemester())).append(",");
                    writer.append(String.format(Locale.US, "%.2f", ar.getAverageMarksPercentage())).append(",");
                    writer.append(String.format(Locale.US, "%.2f", ar.getAttendancePercentage())).append(",");
                    writer.append(String.format(Locale.US, "%.2f", ar.getSgpa())).append(",");
                    writer.append(escapeCsv(ar.getAcademicStatus())).append("\n");
                }
            }

            writer.flush();
            Log.d(TAG, "CSV Exported successfully to: " + csvFile.getAbsolutePath());
            return csvFile;

        } catch (Exception e) {
            Log.e(TAG, "Error writing CSV file", e);
            return null;
        }
    }

    private static String escapeCsv(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\"", "\"\"") + "\"";
    }

    public static void shareFile(Context context, File file, String mimeType) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Export file not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Academic Report"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file", e);
            Toast.makeText(context, "File created at: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}
