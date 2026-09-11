package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing an audit log entry in the Firestore portalActivities collection.
 */
public class PortalActivity {

    private String activityId;
    private String type; // STUDENT_ADDED, RESULT_PUBLISHED, etc.
    private String title;
    private String description;
    private String entityType; // STUDENT, SUBJECT, ATTENDANCE, ASSIGNMENT, RESULT, BROADCAST, REPORT, AUTHENTICATION
    private String entityId;
    private String entityName;
    private String studentId;
    private String studentName;
    private String registerNo;
    private String subjectId;
    private String subjectName;
    private String teacherId;
    private String teacherName;

    @ServerTimestamp
    private Timestamp timestamp;

    private Map<String, Object> metadata;

    public PortalActivity() {
        this.metadata = new HashMap<>();
    }

    public PortalActivity(String activityId, String type, String title, String description,
                          String entityType, String entityId, String entityName, String studentId,
                          String studentName, String registerNo, String subjectId, String subjectName,
                          String teacherId, String teacherName, Map<String, Object> metadata) {
        this.activityId = activityId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = entityName;
        this.studentId = studentId;
        this.studentName = studentName;
        this.registerNo = registerNo;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getType() {
        return type != null ? type : "GENERAL";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEntityType() {
        return entityType != null ? entityType : "GENERAL";
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
