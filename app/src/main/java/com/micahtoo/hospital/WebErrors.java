package com.micahtoo.hospital;

import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
class WebErrors {
    @ExceptionHandler(FormProblem.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalid(FormProblem e,Model model) { model.addAttribute("friendlyMessage",e.getMessage());return "error"; }
    @ExceptionHandler(DataAccessException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    String database(DataAccessException e,Model model) {
        // Avoid writing query parameters or patient data into application logs.
        LoggerFactory.getLogger(WebErrors.class).error("Database operation failed ({})",e.getClass().getSimpleName());
        model.addAttribute("friendlyMessage","We could not finish that request. Check the record before retrying, or contact the app administrator.");
        return "error";
    }
}
