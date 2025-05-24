package org.example.annotation.Service;

import org.example.annotation.Entity.Dataset;
import org.example.annotation.Entity.TextPair;
import org.example.annotation.Repository.TextPairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
@Transactional
@Service
public class TextPairService {
    @Autowired
    private TextPairRepository textPairRepository;

    //Import Text Paires from Dataset CSV splitting with ,
    public void importTextPairsFromCsv(Dataset dataset, MultipartFile file) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String line;
        boolean isFirstLine = true;
        while ((line = reader.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                TextPair pair = new TextPair();
                pair.setText1(parts[1].trim());
                pair.setText2(parts[2].trim());
                pair.setDataset(dataset);
                textPairRepository.save(pair);
            }
        }
    }
    //Find Dataset by ID
    public List<TextPair> getByDatasetId(Long Id) {
        return textPairRepository.findByDatasetId(Id);
    }
    //Find Text Pair by ID
    public TextPair getById(Long textPairId) {
        return textPairRepository.getById(textPairId);
    }
    //Find Text Paires by Dataset & Paginating
    public Page<TextPair> getTextPairsByDatasetPaginated(Dataset dataset, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return textPairRepository.findByDataset(dataset, pageable);
    }
}