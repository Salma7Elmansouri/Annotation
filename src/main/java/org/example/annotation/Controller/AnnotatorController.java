package org.example.annotation.Controller;

import org.example.annotation.Entity.User;
import org.example.annotation.Service.TaskService;
import org.example.annotation.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/annotator")
public class AnnotatorController {
    @Autowired
    private UserService userService;
    @Autowired
    private TaskService taskService;

   // ADMIN DASHBOARD

    @GetMapping("/annotator_dashboard")
    public String showDashboard(Authentication auth, Model model) {
        User currentUser = userService.findByUsernameAndActiveTrue(auth.getName()).orElseThrow();
        var tasks = taskService.getTaskSummariesForAnnotator(currentUser.getId());

        model.addAttribute("tasks", tasks);
        model.addAttribute("username", currentUser.getFirstName() + " " + currentUser.getLastName());

        if (tasks.isEmpty()) {
            model.addAttribute("message", "Unavailable tasks for the moment.");
        }

        return "annotator_dashboard";
    }
    // TASKS

    // TASKS (get) //
    @GetMapping("/task")
    public String showTask(@RequestParam(required = false) Long taskId,
                           Authentication auth,
                           Model model) {
        User current = userService.findByUsernameAndActiveTrue(auth.getName()).orElseThrow(null);
        Map<String, Object> taskData = taskService.getTaskViewData(current.getId(), taskId);

        if (taskData == null) {
            model.addAttribute("message", "Unavailable task for the moment.");
            return "annotator_dashboard";
        }

        model.addAllAttributes(taskData);
        return "annotator_task";
    }

    // Tasks (post) //
    @PostMapping("/task/submit")
    public String submitAnnotation(@RequestParam Long taskId,
                                   @RequestParam String selectedClass,
                                   Authentication auth) {
        User current = userService.findByUsernameAndActiveTrue(auth.getName()).orElseThrow(null);
        Long nextTaskId = taskService.saveAndGetNextTaskId(current.getId(), taskId, selectedClass);
        return (nextTaskId != null)
                ? "redirect:/annotator/task?taskId=" + nextTaskId
                : "redirect:/annotator/annotator_dashboard";
    }

    //STATISTICS
    @GetMapping("/statistics")
    public String showMyStatisticsPage() {
        return "annotator_statistics";
    }

    @GetMapping("/statistics/per-day")
    @ResponseBody
    public Map<String, Long> getMyAnnotationsPerDay(Principal principal) {
        Long userId = userService.findByUsernameAndActiveTrue(principal.getName()).orElseThrow().getId();
        return taskService.countAnnotationsPerDayByAnnotator(userId);
    }

    @GetMapping("statistics/per-class")
    @ResponseBody
    public Map<String, Long> getMyAnnotationsPerClass(Principal principal) {
        Long userId = userService.findByUsernameAndActiveTrue(principal.getName()).orElseThrow().getId();
        return taskService.countAnnotationsPerClassByAnnotator(userId);
    }




}
