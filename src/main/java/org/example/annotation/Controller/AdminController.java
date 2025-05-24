package org.example.annotation.Controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.annotation.Entity.Dataset;
import org.example.annotation.Entity.Task;
import org.example.annotation.Entity.TextPair;
import org.example.annotation.Entity.User;
import org.example.annotation.Service.DatasetService;
import org.example.annotation.Service.TaskService;
import org.example.annotation.Service.TextPairService;
import org.example.annotation.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
@Autowired
    private UserService userService;
@Autowired
    private DatasetService datasetService;
@Autowired
    private TaskService taskService;
@Autowired
private TextPairService textPairService;

  // ADMIN DASHBOARD

    @GetMapping("/admin_dashboard")
    public String showDashboard(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "admin_dashboard";
    }

    // ANNOTATORS MANAGEMENT

    //Annotators list
    @GetMapping("/annotators")
    public String listAnnotators(Model model) {
        List<User> annotators = userService.findAllAnnotators();
        model.addAttribute("annotators",annotators);
        return "annotators";
    }

    // Add Annotator (get) //
    @GetMapping("/annotators/create")
    public String showCreateForm(Model model) {
        model.addAttribute("annotator", new User());
        return "annotator_form";
    }

    // Add Annotator (post) //
    @PostMapping("/annotators/save")
    public String saveAnnotator(@Valid @ModelAttribute("annotator") User user,
                                BindingResult result,
                                Model model) {
        if (userService.findByUsernameAndActiveTrue(user.getUsername()).isPresent()) {
            result.rejectValue("username", null, "Ce nom d'utilisateur est déjà utilisé");//spring.validator
        }

        if (result.hasErrors()) {
            model.addAttribute("annotator", user);
            return "annotator_form";
        }

        user.setRole("ANNOTATOR");
        userService.save(user);
        return "redirect:/admin/annotators";
    }

    // Delete Annotator //
    @GetMapping("/annotators/delete/{id}")
    public String deleteAnnotator(@PathVariable Long id) {
        User user = userService.getById(id);
        user.setActive(false); // suppression logique
        userService.save(user);
        return "redirect:/admin/annotators";
    }

    //Annotator progress
    @GetMapping("/annotators/{id}/progress")
    public String showAnnotatorProgress(@PathVariable Long id, Model model) {
        User annotator = userService.getById(id);
        List<Map<String, Object>> summaries = taskService.getTaskSummariesForAnnotator(id);

        model.addAttribute("annotator", annotator);
        model.addAttribute("summaries", summaries);

        return "annotator_progress";
    }
    // Update Annotator (get) //
    @GetMapping("/annotators/update/{id}")
    public String showUpdateForm(@PathVariable Long id, Model model) {
        model.addAttribute("annotator", userService.getById(id));
        return "annotator_update";
    }
    // Update Annotator(post) //
    @PostMapping("/annotators/update/{id}")
    public String updateAnnotator(@PathVariable Long id,
                                  @Valid @ModelAttribute("annotator") User annotator,
                                  BindingResult result,
                                  Model model) {
        if (result.hasErrors()) {
            annotator.setId(id);
            model.addAttribute("annotator", annotator);
            return "annotator_update";
        }

        userService.update(id, annotator);
        return "redirect:/admin/annotators";
    }

    // DATASETS MANAGEMENT

    // Add Dataset (get) //
    @GetMapping("/datasets/create")
    public String showCreateDataset(Model model) {
        model.addAttribute("dataset", new Dataset());
        return "dataset_form";
    }

    // Add Dataset (post) //
    @PostMapping("/datasets/create")
    public String handleCreateDataset(@ModelAttribute Dataset dataset,
                                      @RequestParam("file") MultipartFile file) throws Exception {
        Dataset created = datasetService.createDataset(dataset.getName(), dataset.getClasses(), dataset.getDescription());
        textPairService.importTextPairsFromCsv(created, file);

        if (file != null && !file.isEmpty()) {
            String uploadDir = new File("src/main/resources/static/uploads").getAbsolutePath();
            new File(uploadDir).mkdirs();
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + File.separator + fileName;
            file.transferTo(new File(filePath));
            created.setFilePath("uploads/" + fileName);
            datasetService.save(created);
        }

        return "redirect:/admin/datasets";
    }

    // Datasets List //
    @GetMapping("/datasets")
    public String listDatasets(Model model) {
        List<Dataset> datasets = datasetService.findAll();
        Map<Long, Integer> avancements = new HashMap<>();
        for (Dataset dataset : datasets) {
            avancements.put(dataset.getId(), datasetService.calculerAvancement(dataset));
        }
        model.addAttribute("datasets", datasets);
        model.addAttribute("avancements", avancements);
        return "datasets";
    }

    // Dataset Details //
    @GetMapping("/datasets/{id}/details")
    public String viewDatasetDetails(@PathVariable Long id,
                                     @RequestParam(defaultValue = "0") int page,
                                     Model model) {
        Dataset dataset = datasetService.getById(id);
        int size = 10;
        Page<TextPair> pageTextPairs = textPairService.getTextPairsByDatasetPaginated(dataset, page, size);

        model.addAttribute("dataset", dataset);
        model.addAttribute("avancement", datasetService.calculerAvancement(dataset));
        model.addAttribute("textPairs", pageTextPairs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageTextPairs.getTotalPages());
        model.addAttribute("totalPairs", pageTextPairs.getTotalElements());

        return "dataset_details";
    }

    // Dataset Tasks Assign (auto-get) //
    @GetMapping("/datasets/{id}/assign")
    public String showAnnotatorAssignment(@PathVariable Long id, Model model) {
        Dataset dataset = datasetService.getById(id);
        List<User> allAnnotators = userService.findAllAnnotators();
        List<User> assignedAnnotators = taskService.findAnnotatorsByDataset(dataset);
        Set<Long> assignedIds = assignedAnnotators.stream().map(User::getId).collect(Collectors.toSet());

        model.addAttribute("dataset", dataset);
        model.addAttribute("annotators", allAnnotators);
        model.addAttribute("assignedIds", assignedIds);
        return "assign_annotators";
    }

    // Dataset Tasks Assign (auto-post) //
    @PostMapping("/datasets/{id}/assign")
    public String assignAnnotatorsToDataset(@PathVariable Long id,
                                            @RequestParam(name = "annotatorIds", required = false) List<Long> annotatorIds,
                                            @RequestParam("deadline") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date deadline) {
        Dataset dataset = datasetService.getById(id);

        List<User> currentlyAssigned = taskService.findAnnotatorsByDataset(dataset);
        Set<Long> currentlyAssignedIds = currentlyAssigned.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        Set<Long> newAssignedIds = annotatorIds == null ? Collections.emptySet() : new HashSet<>(annotatorIds);

        // Désaffecter annotateurs non sélectionnés sans supprimer leur travail
        Set<Long> toUnassign = new HashSet<>(currentlyAssignedIds);
        toUnassign.removeAll(newAssignedIds);
        for (Long userIdToRemove : toUnassign) {
            taskService.unassignAnnotatorFromDataset(userIdToRemove, dataset);
        }

        // Assigner les annotateurs sélectionnés
        if (!newAssignedIds.isEmpty()) {
            List<User> annotators = userService.findByIds(new ArrayList<>(newAssignedIds));
            taskService.assignTextPairsToAnnotators(dataset, annotators, deadline);
        }

        return "redirect:/admin/datasets";
    }

    // TASKS MANAGEMENT

    // Tasks List //
    @GetMapping("/tasks")
    public String listAllTasksFiltered(@RequestParam(required = false) Long annotatorId,
                                       @RequestParam(required = false) Long datasetId,
                                       @RequestParam(defaultValue = "0") int page,
                                       Model model) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("deadline").ascending());
        Page<Task> tasksPage;

        if (annotatorId != null && datasetId != null) {
            tasksPage = taskService.findByAnnotatorAndDataset(annotatorId, datasetId, pageable);
        } else if (annotatorId != null) {
            tasksPage = taskService.findByAnnotatorId(annotatorId, pageable);
        } else if (datasetId != null) {
            tasksPage = taskService.findByDatasetId(datasetId, pageable);
        } else {
            tasksPage = taskService.findAll(pageable);
        }

        model.addAttribute("tasksPage", tasksPage);
        model.addAttribute("annotators", userService.findAllAnnotators());
        model.addAttribute("datasets", datasetService.findAll());
        model.addAttribute("selectedAnnotatorId", annotatorId);
        model.addAttribute("selectedDatasetId", datasetId);
        return "tasks";
    }

    // Dataset Task Assign (manuel-get) //
    @GetMapping("tasks/create")
    public String showCreateForm(@RequestParam(required = false) Long datasetId, Model model) {
        model.addAttribute("datasets", datasetService.findAll());
        model.addAttribute("selectedDatasetId", datasetId);

        if (datasetId != null) {
            model.addAttribute("textPairs", textPairService.getByDatasetId(datasetId));
            model.addAttribute("annotators", userService.findAllAnnotators());
        }

        return "task";
    }

    // Dataset Task Assign (manuel-post) //
    @PostMapping("/tasks/create")
    public String createManualTasks(@RequestParam Long datasetId,
                                    @RequestParam Long textPairId,
                                    @RequestParam List<Long> annotatorIds,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date deadline) {
        Dataset dataset = datasetService.getById(datasetId);
        TextPair pair = textPairService.getById(textPairId);
        List<User> annotators = userService.findByIds(annotatorIds);
        taskService.createManualTasks(dataset, pair, annotators,deadline);
        return "redirect:/admin/tasks";
    }

    //DATASET EXPORT

    // Dataset Export after Annotation Completion //
    @GetMapping("/datasets/{id}/export")
    public void exportDataset(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Dataset dataset = datasetService.getById(id);
        if (datasetService.calculerAvancement(dataset) < 100) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Le dataset n’est pas encore complété.");
            return;
        }

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"dataset_" + id + "_annotations.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Text1,Text2,Annotation");

        for (Task task : taskService.getTasksByDataset(dataset)) {
            if (task.getAnnotation() != null) {
                String text1 = task.getTextPair().getText1().replaceAll("\"", "\"\"");
                String text2 = task.getTextPair().getText2().replaceAll("\"", "\"\"");
                String annotation = task.getAnnotation().replaceAll("\"", "\"\"");
                writer.printf("\"%s\",\"%s\",\"%s\"%n", text1, text2, annotation);
            }
        }

        writer.flush();
        writer.close();
    }

    //STATISCTICS

    // Statistics List //
    @GetMapping("/statistics")
    public String showStatisticsPage() {
        return "admin_statistics1";
    }
    // Statistics by annotator //
    @GetMapping("/statistics/annotations-per-annotator")
    @ResponseBody
    public Map<String, Integer> getAnnotationsPerAnnotator() {
        List<User> annotators = userService.findAllAnnotators();
        Map<String, Integer> result = new LinkedHashMap<>();

        for (User annotator : annotators) {
            int count = taskService.countAnnotatedTasksByAnnotator(annotator.getId());
            result.put(annotator.getFirstName() + " " + annotator.getLastName(), count);
        }

        return result;
    }
    // Statistics by classes //
    @GetMapping("/statistics/classes-distribution")
    @ResponseBody
    public Map<String, Long> classesDistribution() {
        return taskService.countAnnotationsPerClass(); // Ex: {"N": 10, "E": 20, "C": 15}
    }
    // Statistics by annotations/date //
    @GetMapping("/statistics/annotations-over-time")
    @ResponseBody
    public Map<String, Long> annotationsOverTime() {
        return taskService.countAnnotationsPerDay();
    }
    //Statistics by annotations/datset //
    @GetMapping("/statistics/annotations-per-dataset")
    @ResponseBody
    public Map<String, Integer> annotationsPerDataset() {
        return taskService.countAnnotatedTasksPerDataset();
    }

}
