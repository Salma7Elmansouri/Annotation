package org.example.annotation.Entity;
import jakarta.persistence.*;

@Entity
public class TextPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text1;
    private String text2;

    @ManyToOne
    @JoinColumn(name = "datasets_id")
    private Dataset dataset;
    public TextPair() {}

    public TextPair(Long id, String text1, String text2, Dataset dataset) {
        this.id = id;
        this.text1 = text1;
        this.text2 = text2;
        this.dataset = dataset;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText1() {
        return text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    public String getText2() {
        return text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
}
