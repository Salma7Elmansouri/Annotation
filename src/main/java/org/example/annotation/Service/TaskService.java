package org.example.annotation.Service;

import org.example.annotation.Entity.Dataset;
import org.example.annotation.Entity.Task;
import org.example.annotation.Entity.TextPair;
import org.example.annotation.Entity.User;
import org.example.annotation.Repository.DatasetRepository;
import org.example.annotation.Repository.TaskRepository;
import org.example.annotation.Repository.TextPairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TextPairRepository textPairRepository;
    @Autowired
    private DatasetRepository datasetRepository;
    // Assign Tasks(Text Paires) to users auto

    public void assignTextPairsToAnnotators(Dataset dataset, List<User> newAnnotators, Date deadline) {
        if (newAnnotators == null || newAnnotators.isEmpty()) return;
        List<Task> existingTasks = taskRepository.findByDataset(dataset);
        Set<Long> newAnnotatorIds = newAnnotators.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        Set<TextPair> alreadyAnnotatedPairs = existingTasks.stream()
                .filter(task -> task.getAnnotation() != null && !task.getAnnotation().trim().isEmpty())
                .map(Task::getTextPair)
                .collect(Collectors.toSet());
        Set<TextPair> remainingPairs = new HashSet<>();
        for (Task task : existingTasks) {
            User annotator = task.getAnnotator();
            if (annotator != null && !newAnnotatorIds.contains(annotator.getId())) {
                if (task.getAnnotation() == null) {
                    taskRepository.delete(task);
                } else {
                    task.setVisible(false);
                    taskRepository.save(task);
                }
            } else if (annotator != null && newAnnotatorIds.contains(annotator.getId())) {
                remainingPairs.add(task.getTextPair());
            }
        }
        List<TextPair> allPairs = textPairRepository.findByDataset(dataset);
        List<TextPair> unassignedPairs = allPairs.stream()
                .filter(pair -> !alreadyAnnotatedPairs.contains(pair) && !remainingPairs.contains(pair))
                .collect(Collectors.toList());
        int index = 0;
        for (TextPair pair : unassignedPairs) {
            User annotator = newAnnotators.get(index % newAnnotators.size());
            Task task = new Task();
            task.setDataset(dataset);
            task.setTextPair(pair);
            task.setAnnotator(annotator);
            task.setDeadline(deadline);
            task.setVisible(true);
            taskRepository.save(task);
            index++;
        }
    }

    // Assign Tasks(Text Paires) to users manuel
    public void createManualTasks(Dataset dataset, TextPair pair, List<User> annotators, Date deadline) {
        for (User annotator : annotators) {
            if (annotator != null) {
                Task task = new Task();
                task.setDataset(dataset);
                task.setTextPair(pair);
                task.setAnnotator(annotator);
                task.setDeadline(deadline);
                task.setAnnotationDate(LocalDateTime.now());
                taskRepository.save(task);
            }

        }
    }
    // Unassign Annotators from dataset with proper cleanup
    public void unassignAnnotatorFromDataset(Long annotatorId, Dataset dataset) {
        List<Task> tasks = taskRepository.findByAnnotatorIdAndDataset(annotatorId, dataset);
        for (Task task : tasks) {
            String annotation = task.getAnnotation();
            boolean isAnnotated = annotation != null && !annotation.trim().isEmpty();
            if (!isAnnotated) {
                taskRepository.delete(task);
            } else {
                task.setVisible(false);
                taskRepository.save(task);
            }
        }
    }


    //Find Tasks by Dataset
    public List<Task> getTasksByDataset(Dataset dataset) {
        return taskRepository.findByDataset(dataset);
    }
    public List<Map<String, Object>> getTaskSummariesForAnnotator(Long annotatorId) {
        List<Task> allTasks = taskRepository.findByAnnotatorId(annotatorId);
        Map<String, List<Task>> grouped = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getDataset().getName()));
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
            List<Task> datasetTasks = entry.getValue();
            long completed = datasetTasks.stream()
                    .filter(t -> t.getAnnotation() != null)
                    .count();
            int avancement = (int) ((completed * 100.0) / datasetTasks.size());
            Task anyTask = datasetTasks.get(0);
            Optional<Task> firstIncomplete = datasetTasks.stream()
                    .filter(t -> t.getAnnotation() == null)
                    .findFirst();
            Map<String, Object> summary = new HashMap<>();
            summary.put("taskId", firstIncomplete.map(Task::getId).orElse(anyTask.getId()));
            summary.put("datasetName", entry.getKey());
            summary.put("deadline", anyTask.getDeadline());
            summary.put("total", datasetTasks.size());
            summary.put("avancement", avancement);
            summaries.add(summary);
        }
        return summaries;
    }

    // Get Current task data
    public Map<String, Object> getTaskViewData(Long annotatorId, Long taskId) {
        List<Task> tasks = taskRepository.findByAnnotatorIdOrderById(annotatorId);
        if (tasks.isEmpty()) return null;
        int index = 0;
        if (taskId != null) {
            for (int i = 0; i < tasks.size(); i++) {
                if (Objects.equals(tasks.get(i).getId(), taskId)) {
                    index = i;
                    break;
                }
            }
        }
        Task current = tasks.get(index);
        Task previous = (index > 0) ? tasks.get(index - 1) : null;
        Task next = (index < tasks.size() - 1) ? tasks.get(index + 1) : null;
        List<String> classes = Arrays.asList(current.getDataset().getClasses().split(";"));
        Map<String, Object> modelData = new HashMap<>();
        modelData.put("task", current);
        modelData.put("classes", classes);
        modelData.put("previousTaskId", previous != null ? previous.getId() : null);
        modelData.put("nextTaskId", next != null ? next.getId() : null);
        modelData.put("taskIndex", index);
        modelData.put("taskTotal", tasks.size());
        return modelData;
    }

    //Submit task and go next
    public Long saveAndGetNextTaskId(Long annotatorId, Long currentTaskId, String annotation) {
        Task task = taskRepository.findById(currentTaskId).orElse(null);
        if (task != null) {
            task.setAnnotation(annotation);
            task.setAnnotationDate(LocalDateTime.now());
            taskRepository.save(task);
        }
        List<Task> tasks = taskRepository.findByAnnotatorIdOrderById(annotatorId);
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(currentTaskId) && i < tasks.size() - 1) {
                return tasks.get(i + 1).getId();
            }
        }
        return null;
    }

    //Find Annotators by Dataset
    public List<User> findAnnotatorsByDataset(Dataset dataset) {
        List<Task> tasks = taskRepository.findByDataset(dataset);
        return tasks.stream()
                .map(Task::getAnnotator)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    //STATISTICS

    //Count annotated tasks by Annotator ID
    public int countAnnotatedTasksByAnnotator(Long annotatorId) {
        return taskRepository.countByAnnotatorIdAndAnnotationIsNotNull(annotatorId);
    }
    //Count Annotators per Class
    public Map<String, Long> countAnnotationsPerClass() {
        List<Task> annotatedTasks = taskRepository.findByAnnotationIsNotNull();
        return annotatedTasks.stream()
                .collect(Collectors.groupingBy(Task::getAnnotation, Collectors.counting()));
    }
    //Count Annotations per Day
    public Map<String, Long> countAnnotationsPerDay() {
        List<Task> annotatedTasks = taskRepository.findByAnnotationIsNotNull();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return annotatedTasks.stream()
                .filter(t -> t.getAnnotationDate() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getAnnotationDate()
                                .toLocalDate()
                                .format(formatter),
                        Collectors.counting()
                ));
    }

    //Count Annotated Tasks per Dataset
    public Map<String, Integer> countAnnotatedTasksPerDataset() {
        List<Dataset> datasets = datasetRepository.findAll();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Dataset d : datasets) {
            int count = (int) d.getTasks().stream()
                    .filter(t -> t.getAnnotation() != null)
                    .count();
            result.put(d.getName(), count);
        }
        return result;
    }
    //count annotations per day by annotator
    public Map<String, Long> countAnnotationsPerDayByAnnotator(Long annotatorId) {
        List<Task> tasks = taskRepository.findByAnnotatorIdAndAnnotationIsNotNull(annotatorId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return tasks.stream()
                .filter(t -> t.getAnnotationDate() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getAnnotationDate().toLocalDate().format(formatter),
                        Collectors.counting()
                ));
    }
    //count annottaions per class by annotator
    public Map<String, Long> countAnnotationsPerClassByAnnotator(Long annotatorId) {
        List<Task> tasks = taskRepository.findByAnnotatorIdAndAnnotationIsNotNull(annotatorId);

        return tasks.stream()
                .collect(Collectors.groupingBy(Task::getAnnotation, Collectors.counting()));
    }
    // Find All Tasks & paginating
    public Page<Task> findAll(Pageable pageable) {
        return taskRepository.findAll(pageable);
    }
    //Find Tasks by Annotator ID & paginating
    public Page<Task> findByAnnotatorId(Long annotatorId, Pageable pageable) {
        return taskRepository.findByAnnotatorId(annotatorId, pageable);
    }
    //Find Tasks by Dataset ID & paginating
    public Page<Task> findByDatasetId(Long datasetId, Pageable pageable) {
        return taskRepository.findByDatasetId(datasetId, pageable);
    }
    //Find Tasks By Annotator ID and Dataset ID & paginating
    public Page<Task> findByAnnotatorAndDataset(Long annotatorId, Long datasetId, Pageable pageable) {
        return taskRepository.findByAnnotatorAndDataset(annotatorId, datasetId, pageable);
    }



}