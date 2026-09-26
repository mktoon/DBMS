package com.micahtoo.hospital;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {
    private final ClinicRepository repository;
    private final ClinicTime time;
    public DashboardController(ClinicRepository repository,ClinicTime time) { this.repository=repository;this.time=time; }
    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/") String dashboard(Model model) {
        var date=time.today();var start=time.dayStartUtc(date);var end=time.dayStartUtc(date.plusDays(1));
        model.addAttribute("overview",repository.overview(start,end));
        model.addAttribute("today",date);model.addAttribute("agenda",repository.appointments(start,end,"scheduled",null,0).items().stream().limit(8).toList());
        return "dashboard";
    }
    @GetMapping("/physicians") String physicians(@RequestParam(required=false) Long department,Model model) {
        model.addAttribute("departments",repository.departments());model.addAttribute("selectedDepartment",department);
        model.addAttribute("physicians",repository.physicians(department));return "physicians";
    }
    @GetMapping("/reports") String reports(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,Model model) {
        if(from==null) from=time.today().withDayOfMonth(1);if(to==null) to=time.today();time.validateRange(from,to);
        var rows=repository.report(time.dayStartUtc(from),time.dayStartUtc(to.plusDays(1)));
        model.addAttribute("from",from);model.addAttribute("to",to);model.addAttribute("rows",rows);
        model.addAttribute("scheduled",rows.stream().mapToLong(Models.ReportRow::scheduled).sum());
        model.addAttribute("completed",rows.stream().mapToLong(Models.ReportRow::completed).sum());
        model.addAttribute("cancelled",rows.stream().mapToLong(Models.ReportRow::cancelled).sum());
        return "reports";
    }
}
