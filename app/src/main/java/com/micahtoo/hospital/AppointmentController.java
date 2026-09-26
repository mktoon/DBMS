package com.micahtoo.hospital;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    private final ClinicRepository repository;
    private final SchedulingService scheduling;
    private final ClinicTime time;
    public AppointmentController(ClinicRepository repository,SchedulingService scheduling,ClinicTime time) {
        this.repository=repository;this.scheduling=scheduling;this.time=time;
    }
    @InitBinder("appointmentForm") void bind(WebDataBinder binder) { binder.setAllowedFields("patientId","physicianId","startsAt","endsAt","visitReason"); }
    @GetMapping String list(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue="all") String status,@RequestParam(required=false) Long physician,
            @RequestParam(defaultValue="0") int page,Model model) {
        if(date==null) date=time.today();time.validateRange(date,date);
        model.addAttribute("date",date);model.addAttribute("status",status);model.addAttribute("selectedPhysician",physician);
        model.addAttribute("physicians",repository.physicians(null));
        model.addAttribute("appointments",repository.appointments(time.dayStartUtc(date),time.dayStartUtc(date.plusDays(1)),status,physician,page));
        return "appointments";
    }
    @GetMapping("/new") String create(@RequestParam(required=false) Long patientId,Model model) {
        if(patientId==null) return "redirect:/patients?booking";
        repository.patient(patientId);var f=new Forms.AppointmentForm();f.setPatientId(patientId);
        model.addAttribute("appointmentForm",f);return form(null,f,model);
    }
    @PostMapping String book(@Valid @ModelAttribute("appointmentForm") Forms.AppointmentForm f,BindingResult errors,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors()) {
            try {long id=scheduling.book(f);flash.addFlashAttribute("success","Appointment booked.");return "redirect:/appointments/"+id;}
            catch(FormProblem e) { errors.reject("scheduling",e.getMessage()); }
        }
        return form(null,f,model);
    }
    @GetMapping("/{id}") String details(@PathVariable long id,Model model) { model.addAttribute("appointment",repository.appointment(id));return "appointment-detail"; }
    @GetMapping("/{id}/reschedule") String reschedule(@PathVariable long id,Model model) {
        var a=repository.appointment(id);if(!a.status().equals("scheduled")) throw new FormProblem("Only scheduled appointments can be rescheduled.");
        var f=new Forms.AppointmentForm();f.setPatientId(a.patientId());f.setPhysicianId(a.physicianId());
        f.setStartsAt(time.toLocal(a.startsAt()));f.setEndsAt(time.toLocal(a.endsAt()));f.setVisitReason(a.visitReason());
        model.addAttribute("appointmentForm",f);return form(id,f,model);
    }
    @PostMapping("/{id}/reschedule") String move(@PathVariable long id,@Valid @ModelAttribute("appointmentForm") Forms.AppointmentForm f,
            BindingResult errors,Model model,RedirectAttributes flash) {
        var a=repository.appointment(id);f.setPatientId(a.patientId());f.setPhysicianId(a.physicianId());f.setVisitReason(a.visitReason());
        if(!errors.hasErrors()) {
            try {scheduling.reschedule(id,f);flash.addFlashAttribute("success","Appointment rescheduled.");return "redirect:/appointments/"+id;}
            catch(FormProblem e) { errors.reject("scheduling",e.getMessage()); }
        }
        return form(id,f,model);
    }
    @PostMapping("/{id}/status") String status(@PathVariable long id,@RequestParam String status,RedirectAttributes flash) {
        repository.appointment(id);
        try {scheduling.status(id,status);flash.addFlashAttribute("success","Appointment "+status+".");}
        catch(FormProblem e) {flash.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/appointments/"+id;
    }
    private String form(Long id,Forms.AppointmentForm f,Model model) {
        model.addAttribute("appointmentId",id);
        model.addAttribute("patient",f.getPatientId()==null?null:repository.patient(f.getPatientId()));
        model.addAttribute("physicians",repository.physicians(null));return "appointment-form";
    }
}
