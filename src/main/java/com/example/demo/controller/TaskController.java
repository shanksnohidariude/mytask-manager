package com.example.demo.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Task;
import com.example.demo.service.GeminiService;
import com.example.demo.service.TaskService;
import com.example.demo.util.CalendarUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private GeminiService geminiService;

    // 初期表示 → カレンダーへ
    @GetMapping
    public String defaultPage() {
        return "redirect:/tasks/calendar";
    }

    // カレンダー表示
    @GetMapping("/calendar")
    public String showCalendar(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model) {

        LocalDate current = LocalDate.now();

        if (year == null || month == null) {
            year = current.getYear();
            month = current.getMonthValue();
        }
        

        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

        LocalDate prevMonthDate = firstDayOfMonth.minusMonths(1);
        LocalDate nextMonthDate = firstDayOfMonth.plusMonths(1);
        
        Map<LocalDate, List<Task>> taskCountMap = taskService.getTaskCountByMonth(year, month);
        
        model.addAttribute(
        	    "weeks",
        	    CalendarUtil.generateCalendar(LocalDate.of(year, month, 1), taskCountMap)
        	);
        model.addAttribute("prevYear", prevMonthDate.getYear());
        model.addAttribute("prevMonth", prevMonthDate.getMonthValue());
        model.addAttribute("nextYear", nextMonthDate.getYear());
        model.addAttribute("nextMonth", nextMonthDate.getMonthValue());

        model.addAttribute("currentDate", firstDayOfMonth);

        return "task/calendar";
    }


    // 日付クリック後のタスク一覧
    @GetMapping("/date/{date}")
    public String showTasksByDate(@PathVariable("date") String date, Model model) {
        LocalDate targetDate = LocalDate.parse(date);
        List<Task> tasks = taskService.getTasksByDate(targetDate);

        model.addAttribute("tasks", tasks);
        model.addAttribute("selectedDate", targetDate);
        model.addAttribute("newTask", new Task()); // 新規タスク用
        return "task/tasks_by_date";
    }

    // タスク一覧
    @GetMapping("/list")
    public String listTasks(Model model) {
        model.addAttribute("tasks", taskService.getAllTasks());
        return "task/list";
    }

    // 新規作成フォーム
    @GetMapping("/new")
    public String newTaskForm(Model model) {
        model.addAttribute("task", new Task());
        return "task/new";
    }

    // 保存
    @PostMapping
    public String createTask(@ModelAttribute Task task) {
        taskService.saveTask(task);
        return "redirect:/tasks";
    }

    // 締切順一覧
    @GetMapping("/upcoming")
    public String upcomingTasks(Model model) {
        List<Task> tasks = taskService.getTasksOrderByDueDate();
        model.addAttribute("tasks", tasks);
        return "task/upcoming";
    }
    
    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) String date, Model model) {
        Task task = new Task();

        if (date != null) {
            task.setDueDate(LocalDate.parse(date)); // 日付を初期セット
        }
        
        model.addAttribute("task", task);
        return "task/task_form";  // ← 追加用フォーム
    }
    
    @PostMapping("/save")
    public String saveTask(@ModelAttribute Task task) {
        if (task.getId() != null) {
            // 既存タスクの場合
            Optional<Task> existingTaskOpt = taskService.findById(task.getId());
            if (existingTaskOpt.isPresent()) {
                Task existingTask = existingTaskOpt.get();

                // 日付が入力されていなければ元のまま保持
                if (task.getDueDate() == null) {
                    task.setDueDate(existingTask.getDueDate());
                }

                taskService.saveTask(task);
                return "redirect:/tasks/date/" + task.getDueDate();
            } else {
                // idはあるが存在しない場合はカレンダーに戻す
                return "redirect:/tasks/calendar";
            }
        }

        // 新規タスクの場合
        taskService.saveTask(task);
        return "redirect:/tasks/date/" + task.getDueDate();
    }

    
    @GetMapping("/edit/{id}")
    public String editTask(@PathVariable Long id, Model model) {
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);
        return "task/task_edit";
    }
    
    @PostMapping("/delete/{id}")
    public String deleteTask(@PathVariable Long id) {
        Optional<Task> taskOpt = taskService.findById(id);
        if (taskOpt.isPresent()) {
            LocalDate date = taskOpt.get().getDueDate();
            taskService.deleteById(id);

            // 削除後は必ずタスク一覧ページにリダイレクト
            if (date != null) {
                return "redirect:/tasks/date/" + date;
            }
        }

        // 万一日付が null の場合やタスクが存在しない場合はカレンダーに戻す
        return "redirect:/tasks/calendar";
    }
    
 // AI計画生成フォーム表示
    @GetMapping("/ai-plan")
    public String showAiPlanForm(Model model) {
        model.addAttribute("goal", "");
        return "task/ai_plan";
    }

 // AI計画生成処理を修正
    @PostMapping("/generate-plan")
    public String generatePlan(
            @RequestParam String goal,
            @RequestParam(defaultValue = "3") int weeklyFrequency,
            @RequestParam(defaultValue = "4") int deadlineWeeks,
            Model model) {
        
        String aiResponse = geminiService.generateTaskPlan(goal, weeklyFrequency, deadlineWeeks);
        
        if (aiResponse == null || aiResponse.isEmpty()) {
            model.addAttribute("error", "AIからの応答が取得できませんでした。");
            model.addAttribute("goal", goal);
            return "task/ai_plan";
        }
        
        // JSONパース＆タスクリスト作成（まだ保存しない）
        List<Task> generatedTasks = parseTasksWithoutSaving(aiResponse);
        
        if (generatedTasks.isEmpty()) {
            model.addAttribute("error", "タスクの生成に失敗しました。");
            model.addAttribute("goal", goal);
            return "task/ai_plan";
        }
        
        // タスクリストをJSON文字列に変換
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String tasksJson = mapper.writeValueAsString(generatedTasks);
            
            model.addAttribute("tasks", generatedTasks);
            model.addAttribute("tasksJson", tasksJson);
            model.addAttribute("goal", goal);
            return "task/ai_plan_confirm";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "タスクデータの変換に失敗しました。");
            model.addAttribute("goal", goal);
            return "task/ai_plan";
        }
    }

 // 確認後の保存処理を修正
    @PostMapping("/save-generated-tasks")
    public String saveGeneratedTasks(@RequestParam Map<String, String> allParams) {
        try {
            List<Task> tasks = new ArrayList<>();
            
            // パラメータからタスクを再構築
            int index = 0;
            while (allParams.containsKey("tasks[" + index + "].title")) {
                Task task = new Task();
                task.setTitle(allParams.get("tasks[" + index + "].title"));
                task.setDescription(allParams.get("tasks[" + index + "].description"));
                
                String dueDateStr = allParams.get("tasks[" + index + "].dueDate");
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    task.setDueDate(LocalDate.parse(dueDateStr));
                }
                
                tasks.add(task);
                index++;
            }
            
            // タスクを保存
            for (Task task : tasks) {
                taskService.saveTask(task);
            }
            
            return "redirect:/tasks/calendar";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/tasks/ai-plan";
        }
    }

    // 保存せずにタスクリストを作成するメソッド
    private List<Task> parseTasksWithoutSaving(String jsonText) {
        List<Task> tasks = new ArrayList<>();
        try {
            String cleanJson = jsonText.replaceAll("```json\\n?|```", "").trim();
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tasksNode = mapper.readTree(cleanJson);
            
            LocalDate today = LocalDate.now();
            
            for (JsonNode taskNode : tasksNode) {
                Task task = new Task();
                task.setTitle(taskNode.get("title").asText());
                task.setDescription(taskNode.get("description").asText());
                task.setDueDate(today.plusDays(taskNode.get("daysFromNow").asInt()));
                
                tasks.add(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tasks;
    }

    // 既存のparseAndSaveTasksメソッドは削除または名前変更
}
