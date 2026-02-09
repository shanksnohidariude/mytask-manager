package com.example.demo.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.demo.model.Task;

public class CalendarUtil {
    
    /** カレンダー1マス分 */
    public static class CalendarDay {
        private LocalDate date;
        private boolean inMonth;
        private List<Task> tasks;

        public CalendarDay(LocalDate date, boolean inMonth, List<Task> tasks) {
            this.date = date;
            this.inMonth = inMonth;
            this.tasks = tasks;
        }

        public LocalDate getDate() {
            return date;
        }

        public boolean isInMonth() {
            return inMonth;
        }

        public List<Task> getTasks() {
            return tasks;
        }

        public boolean hasTask() {
            return tasks != null && !tasks.isEmpty();
        }
    }

    /**
     * カレンダーの週リストを生成
     * 日曜日始まりの7列 × 最大6行
     * 翌月の日付のみの週は表示しない
     */
    public static List<List<CalendarDay>> generateCalendar(
            LocalDate firstDayOfMonth,
            Map<LocalDate, List<Task>> taskMap) {
        
        List<List<CalendarDay>> calendar = new ArrayList<>();
        
        // 月の最初の日の曜日を取得（日曜日=7, 月曜日=1, ..., 土曜日=6）
        DayOfWeek firstDayOfWeek = firstDayOfMonth.getDayOfWeek();
        int dayOfWeekValue = firstDayOfWeek.getValue();
        
        // 日曜日始まりにするための調整
        // getValue(): 月曜=1, 火曜=2, ..., 日曜=7
        // 日曜始まりの場合: 日曜=0, 月曜=1, ..., 土曜=6 にしたい
        int offsetDays = (dayOfWeekValue == 7) ? 0 : dayOfWeekValue;
        
        // カレンダーの開始日（前月の日付から始まる場合がある）
        LocalDate startDate = firstDayOfMonth.minusDays(offsetDays);
        
        LocalDate current = startDate;
        
        // 最大6週分のループ
        for (int week = 0; week < 6; week++) {
            List<CalendarDay> weekRow = new ArrayList<>();
            boolean hasCurrentOrPreviousMonth = false;
            
            for (int dow = 0; dow < 7; dow++) {
                boolean inMonth = current.getMonth() == firstDayOfMonth.getMonth();
                
                // 当月または前月の日付がある場合はフラグを立てる
                if (inMonth || current.isBefore(firstDayOfMonth)) {
                    hasCurrentOrPreviousMonth = true;
                }
                
                List<Task> tasks = taskMap.getOrDefault(current, List.of());
                weekRow.add(new CalendarDay(current, inMonth, tasks));
                current = current.plusDays(1);
            }
            
            // 翌月の日付のみの週（当月や前月の日付が1日もない週）は追加しない
            if (hasCurrentOrPreviousMonth) {
                calendar.add(weekRow);
            }
        }
        
        return calendar;
    }
}