package com.micahtoo.hospital;

/** A safe, user-facing validation or scheduling message. */
public class FormProblem extends RuntimeException {
    public FormProblem(String message) { super(message); }
}
