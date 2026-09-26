package com.micahtoo.hospital;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patients")
public class PatientController {
    private final ClinicRepository repository;
    private final ClinicTime time;
    public PatientController(ClinicRepository repository, ClinicTime time) { this.repository=repository;this.time=time; }
    @InitBinder("patientForm") void bind(WebDataBinder binder) {
        binder.setAllowedFields("fullName","dateOfBirth","phone","address","primaryPhysicianId");
    }
    @GetMapping String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("patients",repository.patients(q,page));model.addAttribute("q",q);return "patients";
    }
    @GetMapping("/new") String create(Model model) { model.addAttribute("patientForm",new Forms.PatientForm());return form(null,model); }
    @GetMapping("/{id}") String details(@PathVariable long id, Model model) {
        model.addAttribute("patient",repository.patient(id));model.addAttribute("history",repository.patientHistory(id));return "patient-detail";
    }
    @GetMapping("/{id}/edit") String edit(@PathVariable long id,Model model) {
        var p=repository.patient(id);var f=new Forms.PatientForm();
        f.setFullName(p.name());f.setDateOfBirth(p.dateOfBirth());f.setPhone(p.phone());f.setAddress(p.address());f.setPrimaryPhysicianId(p.primaryPhysicianId());
        model.addAttribute("patientForm",f);return form(id,model);
    }
    @PostMapping String saveNew(@Valid @ModelAttribute("patientForm") Forms.PatientForm f,BindingResult errors,Model model,RedirectAttributes flash) {
        return save(null,f,errors,model,flash);
    }
    @PostMapping("/{id}") String saveExisting(@PathVariable long id,@Valid @ModelAttribute("patientForm") Forms.PatientForm f,BindingResult errors,Model model,RedirectAttributes flash) {
        repository.patient(id);return save(id,f,errors,model,flash);
    }
    private String save(Long id,Forms.PatientForm f,BindingResult errors,Model model,RedirectAttributes flash) {
        if(f.getDateOfBirth()!=null && (f.getDateOfBirth().getYear()<1000 || f.getDateOfBirth().isAfter(time.todayUtc())))
            errors.rejectValue("dateOfBirth","birthDate","Enter a valid birth date that is not in the future.");
        if(f.getPrimaryPhysicianId()!=null && !repository.physicianExists(f.getPrimaryPhysicianId()))
            errors.rejectValue("primaryPhysicianId","physician","Choose a physician from the directory.");
        if(errors.hasErrors()) return form(id,model);
        long saved=repository.savePatient(id,f);flash.addFlashAttribute("success",id==null?"Patient registered.":"Patient updated.");
        return "redirect:/patients/"+saved;
    }
    private String form(Long id,Model model) {
        model.addAttribute("patientId",id);model.addAttribute("physicians",repository.physicians(null));return "patient-form";
    }
}
