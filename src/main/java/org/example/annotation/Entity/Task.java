package org.example.annotation.Entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;
@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "annotator_id")
    private User annotator;
    @ManyToOne
    @JoinColumn(name = "text_pair_id")
    private TextPair textPair;
    @ManyToOne
    @JoinColumn(name = "datasets_id")
    private Dataset dataset;
    private String annotation;
    @Column(name = "annotation_date")
    private LocalDateTime annotationDate;
    private Date deadline;
    private Boolean visible = true;
    public Task() {}
    public Task(Long id, User annotator, TextPair textPair, Dataset dataset, String annotation, Date deadline,LocalDateTime annotationDate, Boolean visible) {
        this.id = id;
        this.annotator = annotator;
        this.textPair = textPair;
        this.dataset = dataset;
        this.annotation = annotation;
        this.deadline = deadline;
        this.annotationDate=annotationDate;
        this.visible=visible;
    }
    @Column(name = "active")
    private boolean active = true;
    public Date getDeadline() {
        return deadline;
    }
    public LocalDateTime getAnnotationDate() {
        return annotationDate;
    }
    public void setAnnotationDate(LocalDateTime annotationDate) {
        this.annotationDate = annotationDate;
    }
    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public User getAnnotator() {
        return annotator;
    }
    public void setAnnotator(User annotator) {
        this.annotator = annotator;
    }
    public TextPair getTextPair() {
        return textPair;
    }
    public void setTextPair(TextPair textPair) {
        this.textPair = textPair;
    }
    public Dataset getDataset() {
        return dataset;
    }
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
    public String getAnnotation() {
        return annotation;
    }
    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}