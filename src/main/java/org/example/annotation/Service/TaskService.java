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
        // Si la liste des nouveaux annotateurs est vide ou nulle, on arrête la fonction
        if (newAnnotators == null || newAnnotators.isEmpty()) return;

        // 1. Récupérer toutes les tâches existantes liées à ce dataset
        List<Task> existingTasks = taskRepository.findByDataset(dataset);

        // 2. Extraire les IDs des nouveaux annotateurs pour comparaison facile
        Set<Long> newAnnotatorIds = newAnnotators.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // 3. Trouver les TextPairs déjà annotés (avec annotation non vide)
        Set<TextPair> alreadyAnnotatedPairs = existingTasks.stream()
                .filter(task -> task.getAnnotation() != null && !task.getAnnotation().trim().isEmpty())
                .map(Task::getTextPair)
                .collect(Collectors.toSet());

        // 4. Préparer un ensemble pour garder les paires valides des nouveaux annotateurs
        Set<TextPair> remainingPairs = new HashSet<>();

        // 5. Parcourir toutes les tâches existantes pour nettoyer ou garder
        for (Task task : existingTasks) {
            User annotator = task.getAnnotator();

            if (annotator != null && !newAnnotatorIds.contains(annotator.getId())) {
                // Si l’annotateur n’est plus dans la nouvelle liste
                if (task.getAnnotation() == null) {
                    // Supprimer la tâche si non annotée
                    taskRepository.delete(task);
                } else {
                    // Sinon masquer la tâche annotée et sauvegarder
                    task.setVisible(false);
                    taskRepository.save(task);
                }
            } else if (annotator != null && newAnnotatorIds.contains(annotator.getId())) {
                // Garder la tâche si annotateur encore présent
                remainingPairs.add(task.getTextPair());
            }
        }

        // 6. Récupérer toutes les paires de textes du dataset
        List<TextPair> allPairs = textPairRepository.findByDataset(dataset);

        // 7. Filtrer pour garder uniquement les paires non encore annotées et non assignées
        List<TextPair> unassignedPairs = allPairs.stream()
                .filter(pair -> !alreadyAnnotatedPairs.contains(pair) && !remainingPairs.contains(pair))
                .collect(Collectors.toList());

        // 8. Répartir équitablement les paires non assignées aux nouveaux annotateurs
        int index = 0;
        for (TextPair pair : unassignedPairs) {
            User annotator = newAnnotators.get(index % newAnnotators.size());

            // Créer une nouvelle tâche et définir ses attributs
            Task task = new Task();
            task.setDataset(dataset);
            task.setTextPair(pair);
            task.setAnnotator(annotator);
            task.setDeadline(deadline);
            task.setVisible(true);

            // Sauvegarder la tâche dans la base de données
            taskRepository.save(task);

            // Passer à l’annotateur suivant
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
        // Récupérer toutes les tâches de cet annotateur dans le dataset
        List<Task> tasks = taskRepository.findByAnnotatorIdAndDataset(annotatorId, dataset);
        for (Task task : tasks) {
            String annotation = task.getAnnotation();
            boolean isAnnotated = annotation != null && !annotation.trim().isEmpty();
            if (!isAnnotated) {
                // Si la tâche n'est pas annotée => on la supprime
                taskRepository.delete(task);
            } else {
                // Si la tâche est annotée => on la garde mais on la rend invisible (historique)
                task.setVisible(false);
                taskRepository.save(task);
            }
        }
    }


    //Find Tasks by Dataset
    public List<Task> getTasksByDataset(Dataset dataset) {
        return taskRepository.findByDataset(dataset);
    }
    // Cette fonction retourne un résumé des tâches pour un annotateur donné (par son ID)
    public List<Map<String, Object>> getTaskSummariesForAnnotator(Long annotatorId) {
        // 1. Récupérer toutes les tâches assignées à cet annotateur
        List<Task> allTasks = taskRepository.findByAnnotatorId(annotatorId);

        // 2. Regrouper ces tâches par nom de dataset (chaque dataset a sa liste de tâches)
        Map<String, List<Task>> grouped = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getDataset().getName()));

        // 3. Préparer une liste qui contiendra le résumé pour chaque dataset
        List<Map<String, Object>> summaries = new ArrayList<>();

        // 4. Pour chaque groupe de tâches (par dataset)
        for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
            List<Task> datasetTasks = entry.getValue();

            // 5. Compter combien de tâches sont complétées (annotation non nulle)
            long completed = datasetTasks.stream()
                    .filter(t -> t.getAnnotation() != null)
                    .count();

            // 6. Calculer l’avancement en pourcentage (tâches complétées / total)
            int avancement = (int) ((completed * 100.0) / datasetTasks.size());

            // 7. Prendre une tâche quelconque du dataset (pour récupérer infos comme deadline)
            Task anyTask = datasetTasks.get(0);

            // 8. Chercher la première tâche non complétée (annotation nulle)
            Optional<Task> firstIncomplete = datasetTasks.stream()
                    .filter(t -> t.getAnnotation() == null)
                    .findFirst();

            // 9. Construire un résumé sous forme de map avec plusieurs infos
            Map<String, Object> summary = new HashMap<>();
            // ID d’une tâche à afficher (la première incomplète, sinon une tâche quelconque)
            summary.put("taskId", firstIncomplete.map(Task::getId).orElse(anyTask.getId()));
            // Nom du dataset
            summary.put("datasetName", entry.getKey());
            // Date limite (deadline) de la tâche
            summary.put("deadline", anyTask.getDeadline());
            // Nombre total de tâches dans ce dataset
            summary.put("total", datasetTasks.size());
            // Pourcentage d’avancement des tâches
            summary.put("avancement", avancement);

            // 10. Ajouter ce résumé à la liste des résumés
            summaries.add(summary);
        }

        // 11. Retourner la liste complète des résumés pour tous les datasets
        return summaries;
    }

    // Get Current task data
    public Map<String, Object> getTaskViewData(Long annotatorId, Long taskId) {
        // 1. Récupérer toutes les tâches de l’annotateur, triées par ID
        List<Task> tasks = taskRepository.findByAnnotatorIdOrderById(annotatorId);

        // 2. Si la liste des tâches est vide, retourner null (pas de données)
        if (tasks.isEmpty()) return null;

        // 3. Initialiser l'index à 0 (première tâche par défaut)
        int index = 0;

        // 4. Si un taskId est donné, chercher la position (index) de cette tâche dans la liste
        if (taskId != null) {
            for (int i = 0; i < tasks.size(); i++) {
                if (Objects.equals(tasks.get(i).getId(), taskId)) {
                    index = i; // Trouver l’index où la tâche correspond
                    break;
                }
            }
        }

        // 5. Récupérer la tâche actuelle selon l’index trouvé (ou 0 par défaut)
        Task current = tasks.get(index);

        // 6. Trouver la tâche précédente (s’il y en a une)
        Task previous = (index > 0) ? tasks.get(index - 1) : null;

        // 7. Trouver la tâche suivante (s’il y en a une)
        Task next = (index < tasks.size() - 1) ? tasks.get(index + 1) : null;

        // 8. Récupérer les classes du dataset de la tâche actuelle en les séparant par ';'
        List<String> classes = Arrays.asList(current.getDataset().getClasses().split(";"));

        // 9. Préparer une map qui contient toutes les données à retourner
        Map<String, Object> modelData = new HashMap<>();
        modelData.put("task", current);                       // la tâche actuelle
        modelData.put("classes", classes);                     // la liste des classes
        modelData.put("previousTaskId", previous != null ? previous.getId() : null); // ID tâche précédente
        modelData.put("nextTaskId", next != null ? next.getId() : null);             // ID tâche suivante
        modelData.put("taskIndex", index);                     // position actuelle dans la liste
        modelData.put("taskTotal", tasks.size());              // nombre total de tâches

        // 10. Retourner toutes ces données dans une map
        return modelData;
    }

    //Submit task and go next
    public Long saveAndGetNextTaskId(Long annotatorId, Long currentTaskId, String annotation) {
        // 1. Chercher la tâche actuelle par son ID
        Task task = taskRepository.findById(currentTaskId).orElse(null);

        // 2. Si la tâche existe
        if (task != null) {
            // 3. Mettre à jour l'annotation avec la nouvelle valeur
            task.setAnnotation(annotation);
            // 4. Mettre à jour la date d'annotation avec la date/heure actuelle
            task.setAnnotationDate(LocalDateTime.now());
            // 5. Enregistrer la tâche modifiée dans la base de données
            taskRepository.save(task);
        }

        // 6. Récupérer la liste complète des tâches de l’annotateur, triées par ID
        List<Task> tasks = taskRepository.findByAnnotatorIdOrderById(annotatorId);

        // 7. Chercher dans la liste la position de la tâche actuelle
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(currentTaskId) && i < tasks.size() - 1) {
                // 8. Si ce n’est pas la dernière tâche, retourner l’ID de la tâche suivante
                return tasks.get(i + 1).getId();
            }
        }

        // 9. Si c’est la dernière tâche, ou si la tâche n’est pas trouvée, retourner null
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